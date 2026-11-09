import hashlib
import json
import sqlite3
from typing import Annotated

from fastapi import APIRouter, Depends, Header, Query
from starlette.responses import Response

from app.clock import utcnow
from app.context import Context, check_device_id, get_context
from app.errors import JSON_UTF8, ApiError, JsonUtf8Response
from app.files import serve_blob
from app.subscriptions import device_status

router = APIRouter(prefix="/api")

Ctx = Annotated[Context, Depends(get_context)]


def image_public(row: sqlite3.Row, public_url: str) -> dict:
    return {
        "id": row["id"],
        "name": row["name"],
        "distro": row["distro"],
        "desktop": row["desktop"],
        "size_bytes": row["size_bytes"],
        "sha256": row["sha256"],
        "min_ram_mb": row["min_ram_mb"],
        "pro": bool(row["pro"]),
        "url": f"{public_url}/api/images/{row['id']}",
    }


def etag_matches(if_none_match: str | None, etag: str) -> bool:
    if not if_none_match:
        return False
    candidates = [tag.strip().removeprefix("W/") for tag in if_none_match.split(",")]
    return "*" in candidates or etag in candidates


@router.get("/health")
def health() -> JsonUtf8Response:
    return JsonUtf8Response({"ok": True})


@router.get("/pubkey")
def pubkey(ctx: Ctx) -> JsonUtf8Response:
    return JsonUtf8Response({"algorithm": "ed25519", "public_key": ctx.signer.public_key_b64})


@router.get("/manifest")
def manifest(ctx: Ctx, if_none_match: Annotated[str | None, Header()] = None) -> Response:
    with ctx.db.connect() as conn:
        catalog = conn.execute("SELECT version, updated_at FROM catalog WHERE key = 1").fetchone()
        rows = conn.execute("SELECT * FROM images WHERE active = 1 ORDER BY created_at, id").fetchall()
    document = {
        "version": catalog["version"],
        "generated_at": catalog["updated_at"],
        "images": [image_public(row, ctx.settings.public_url) for row in rows],
    }
    body = json.dumps(document, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    etag = f'"{hashlib.sha256(body).hexdigest()[:32]}"'
    headers = {
        "ETag": etag,
        "Cache-Control": "public, max-age=60",
        "X-Palma-Signature": ctx.signer.sign(body),
    }
    if etag_matches(if_none_match, etag):
        return Response(status_code=304, headers=headers)
    return Response(body, media_type=JSON_UTF8, headers=headers)


@router.get("/images/{image_id}")
def download_image(
    image_id: str,
    ctx: Ctx,
    device_id: Annotated[str | None, Query()] = None,
    range_header: Annotated[str | None, Header(alias="range")] = None,
    if_range: Annotated[str | None, Header()] = None,
) -> Response:
    now = utcnow()
    with ctx.db.connect() as conn:
        row = conn.execute("SELECT * FROM images WHERE id = ? AND active = 1", (image_id,)).fetchone()
        if row is None:
            raise ApiError(404, "image_not_found")
        if row["pro"]:
            paid = device_id is not None and device_status(conn, check_device_id(device_id), now).paid
            if not paid:
                raise ApiError(402, "subscription_required")
    path = ctx.images_dir / row["sha256"]
    if not path.is_file():
        raise ApiError(503, "file_unavailable")
    if path.stat().st_size != row["size_bytes"]:
        raise ApiError(503, "file_corrupted")
    return serve_blob(path, row["sha256"], f"{row['id']}.tar.xz", range_header, if_range)
