from typing import Annotated

from fastapi import APIRouter, Depends, Header, Request
from starlette.concurrency import run_in_threadpool
from starlette.datastructures import UploadFile
from starlette.responses import Response

from app.clock import to_iso, utcnow
from app.context import Context, check_device_id, get_context
from app.devices import require_device
from app.errors import ApiError, JsonUtf8Response
from app.files import BlobTooLarge, commit_blob, serve_blob, store_blob
from app.subscriptions import device_status

router = APIRouter(prefix="/api/home")

Ctx = Annotated[Context, Depends(get_context)]


def declared_length(request: Request) -> int | None:
    raw = request.headers.get("content-length")
    return int(raw) if raw and raw.isdigit() else None


@router.post("/{device_id}")
async def upload_home(device_id: str, request: Request, ctx: Ctx) -> JsonUtf8Response:
    device_id = check_device_id(device_id)
    require_device(ctx, device_id, request)
    with ctx.db.connect() as conn:
        if not device_status(conn, device_id, utcnow()).paid:
            raise ApiError(402, "subscription_required")
    length = declared_length(request)
    if length is not None and length > ctx.settings.home_max_bytes + 64 * 1024:
        raise ApiError(413, "payload_too_large")
    if not request.headers.get("content-type", "").startswith("multipart/form-data"):
        raise ApiError(415, "multipart_required")
    async with request.form(max_files=1, max_fields=4) as form:
        archive = form.get("archive")
        if not isinstance(archive, UploadFile):
            raise ApiError(422, "missing_archive")
        try:
            sha256, size, temp = await run_in_threadpool(
                store_blob, archive.file, ctx.home_dir, ctx.settings.home_max_bytes
            )
        except BlobTooLarge as exc:
            raise ApiError(413, "payload_too_large") from exc
    await run_in_threadpool(commit_blob, temp, ctx.home_dir, sha256)
    created = to_iso(utcnow())
    with ctx.db.connect() as conn:
        conn.execute(
            "INSERT INTO home_backups (device_id, sha256, size_bytes, created_at) VALUES (?, ?, ?, ?)",
            (device_id, sha256, size, created),
        )
    return JsonUtf8Response({"sha256": sha256, "size_bytes": size, "created_at": created}, status_code=201)


@router.get("/{device_id}")
def download_home(
    device_id: str,
    request: Request,
    ctx: Ctx,
    range_header: Annotated[str | None, Header(alias="range")] = None,
    if_range: Annotated[str | None, Header()] = None,
) -> Response:
    device_id = check_device_id(device_id)
    require_device(ctx, device_id, request)
    with ctx.db.connect() as conn:
        row = conn.execute(
            "SELECT * FROM home_backups WHERE device_id = ? ORDER BY id DESC LIMIT 1", (device_id,)
        ).fetchone()
    if row is None:
        raise ApiError(404, "backup_not_found")
    return serve_blob(ctx.home_dir / row["sha256"], row["sha256"], "home.tar", range_header, if_range)
