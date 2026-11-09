import hashlib
import hmac
import secrets
import sqlite3

from fastapi import Request

from app.context import Context
from app.errors import ApiError

KEY_HEADER = "x-device-key"
UNKNOWN_DEVICE_HASH = hashlib.sha256(b"unknown-device").hexdigest()


def new_device_key() -> str:
    return secrets.token_urlsafe(32)


def hash_key(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def provided_key(request: Request) -> bytes:
    return request.headers.get(KEY_HEADER, "").encode("utf-8", "surrogateescape")


def key_matches(conn: sqlite3.Connection, device_id: str, raw: bytes) -> bool:
    row = conn.execute("SELECT key_hash FROM devices WHERE device_id = ?", (device_id,)).fetchone()
    stored = row["key_hash"] if row is not None else UNKNOWN_DEVICE_HASH
    same = hmac.compare_digest(hash_key(raw).encode(), stored.encode())
    return row is not None and bool(raw) and same


def require_device(ctx: Context, device_id: str, request: Request) -> None:
    with ctx.db.connect() as conn:
        allowed = key_matches(conn, device_id, provided_key(request))
    if not allowed:
        raise ApiError(403, "forbidden")
