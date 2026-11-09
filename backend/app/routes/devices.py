from typing import Annotated

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field

from app.clock import to_iso, utcnow
from app.context import Context, check_device_id, get_context
from app.devices import hash_key, key_matches, new_device_key, provided_key
from app.errors import ApiError, JsonUtf8Response
from app.subscriptions import device_status

router = APIRouter(prefix="/api/devices")

Ctx = Annotated[Context, Depends(get_context)]


class RegisterRequest(BaseModel):
    device_id: str
    name: str | None = Field(default=None, max_length=128)


def clean_name(name: str | None) -> str | None:
    if name is None:
        return None
    return name.strip() or None


@router.post("/register")
def register_device(payload: RegisterRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    device_id = check_device_id(payload.device_id)
    name = clean_name(payload.name)
    key = new_device_key()
    now = utcnow()
    with ctx.db.connect() as conn:
        created = conn.execute(
            "INSERT OR IGNORE INTO devices (device_id, key_hash, name, created_at) VALUES (?, ?, ?, ?)",
            (device_id, hash_key(key.encode()), name, to_iso(now)),
        ).rowcount
        if not created and not key_matches(conn, device_id, provided_key(request)):
            raise ApiError(409, "device_exists")
        status = device_status(conn, device_id, now)
    body = {"device_id": device_id, "tier": status.tier, "expires_at": to_iso(status.expires_at)}
    if created:
        return JsonUtf8Response({**body, "device_key": key}, status_code=201)
    return JsonUtf8Response(body)
