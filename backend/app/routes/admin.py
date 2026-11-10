import hmac
import re
import sqlite3
from typing import Annotated

from fastapi import APIRouter, Depends, Request
from starlette.concurrency import run_in_threadpool
from starlette.datastructures import FormData, UploadFile

from app.clock import to_iso, utcnow
from app.context import Context, get_context
from app.db import bump_catalog
from app.errors import ApiError, JsonUtf8Response
from app.files import BlobTooLarge, commit_blob, store_blob
from app.stats import user_stats
from app.subscriptions import summary

IMAGE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}$")
TRUE_VALUES = {"1", "true", "on", "yes"}
FALSE_VALUES = {"0", "false", "off", "no", ""}

Ctx = Annotated[Context, Depends(get_context)]


def require_admin(request: Request, ctx: Ctx) -> None:
    expected = ctx.settings.admin_token.encode()
    provided = request.headers.get("x-admin-token", "").encode("utf-8", "surrogateescape")
    if not expected or not hmac.compare_digest(provided, expected):
        raise ApiError(403, "forbidden")


router = APIRouter(prefix="/api/admin", dependencies=[Depends(require_admin)])


def admin_image(row: sqlite3.Row, ctx: Context) -> dict:
    return {
        "id": row["id"],
        "name": row["name"],
        "distro": row["distro"],
        "desktop": row["desktop"],
        "size_bytes": row["size_bytes"],
        "sha256": row["sha256"],
        "min_ram_mb": row["min_ram_mb"],
        "pro": bool(row["pro"]),
        "active": bool(row["active"]),
        "file_present": (ctx.images_dir / row["sha256"]).is_file(),
        "created_at": row["created_at"],
    }


def text_field(form: FormData, name: str, limit: int = 128) -> str:
    value = form.get(name)
    if not isinstance(value, str) or not value.strip() or len(value) > limit:
        raise ApiError(422, f"invalid_{name}")
    return value.strip()


def parse_metadata(form: FormData) -> dict:
    image_id = text_field(form, "id", 64)
    if not IMAGE_ID.fullmatch(image_id):
        raise ApiError(422, "invalid_id")
    ram = text_field(form, "min_ram_mb", 7)
    if not ram.isdigit():
        raise ApiError(422, "invalid_min_ram_mb")
    pro_raw = form.get("pro", "")
    if not isinstance(pro_raw, str) or pro_raw.strip().lower() not in TRUE_VALUES | FALSE_VALUES:
        raise ApiError(422, "invalid_pro")
    return {
        "id": image_id,
        "name": text_field(form, "name"),
        "distro": text_field(form, "distro", 64),
        "desktop": text_field(form, "desktop", 64),
        "min_ram_mb": int(ram),
        "pro": pro_raw.strip().lower() in TRUE_VALUES,
    }


@router.get("/images")
def list_images(ctx: Ctx) -> JsonUtf8Response:
    with ctx.db.connect() as conn:
        rows = conn.execute("SELECT * FROM images ORDER BY created_at DESC, id").fetchall()
    return JsonUtf8Response({"images": [admin_image(row, ctx) for row in rows]})


@router.post("/images")
async def publish_image(request: Request, ctx: Ctx) -> JsonUtf8Response:
    if not request.headers.get("content-type", "").startswith("multipart/form-data"):
        raise ApiError(415, "multipart_required")
    async with request.form(max_files=1, max_fields=16) as form:
        meta = parse_metadata(form)
        upload = form.get("file")
        if not isinstance(upload, UploadFile):
            raise ApiError(422, "missing_file")
        try:
            sha256, size, temp = await run_in_threadpool(
                store_blob, upload.file, ctx.images_dir, ctx.settings.image_max_bytes
            )
        except BlobTooLarge as exc:
            raise ApiError(413, "payload_too_large") from exc
    with ctx.db.connect() as conn:
        clash = conn.execute(
            "SELECT id, sha256 FROM images WHERE id = ? OR sha256 = ?", (meta["id"], sha256)
        ).fetchone()
        if clash is None:
            try:
                conn.execute(
                    """
                    INSERT INTO images (id, name, distro, desktop, size_bytes, sha256, min_ram_mb, pro, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        meta["id"],
                        meta["name"],
                        meta["distro"],
                        meta["desktop"],
                        size,
                        sha256,
                        meta["min_ram_mb"],
                        int(meta["pro"]),
                        to_iso(utcnow()),
                    ),
                )
            except sqlite3.IntegrityError:
                clash = {"sha256": sha256}
            else:
                bump_catalog(conn)
        if clash is None:
            row = conn.execute("SELECT * FROM images WHERE id = ?", (meta["id"],)).fetchone()
    if clash is not None:
        temp.unlink(missing_ok=True)
        code = "duplicate_sha256" if clash["sha256"] == sha256 else "duplicate_id"
        raise ApiError(409, code)
    await run_in_threadpool(commit_blob, temp, ctx.images_dir, sha256)
    return JsonUtf8Response(admin_image(row, ctx), status_code=201)


@router.delete("/images/{image_id}")
def unpublish_image(image_id: str, ctx: Ctx) -> JsonUtf8Response:
    with ctx.db.connect() as conn:
        row = conn.execute("SELECT active FROM images WHERE id = ?", (image_id,)).fetchone()
        if row is None:
            raise ApiError(404, "image_not_found")
        if row["active"]:
            conn.execute("UPDATE images SET active = 0 WHERE id = ?", (image_id,))
            bump_catalog(conn)
    return JsonUtf8Response({"id": image_id, "active": False})


@router.get("/subscribers")
def subscribers(ctx: Ctx) -> JsonUtf8Response:
    with ctx.db.connect() as conn:
        return JsonUtf8Response(summary(conn, utcnow()))


@router.get("/stats")
def stats(ctx: Ctx) -> JsonUtf8Response:
    with ctx.db.connect() as conn:
        return JsonUtf8Response(user_stats(conn, utcnow(), ctx.settings.pro_months))
