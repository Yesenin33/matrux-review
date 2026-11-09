from typing import Annotated

from fastapi import APIRouter, Depends, Query, Request
from pydantic import BaseModel, Field

from app.clock import to_iso, utcnow
from app.context import Context, check_device_id, get_context
from app.devices import require_device
from app.errors import ApiError, JsonUtf8Response
from app.rustore import PurchaseNotFound, StoreUnavailable
from app.subscriptions import LIFETIME, PRO, DeviceConflict, bound_device, device_status, record_purchase

router = APIRouter(prefix="/api/sub")

Ctx = Annotated[Context, Depends(get_context)]


class VerifyRequest(BaseModel):
    purchase_id: str = Field(min_length=1, max_length=256)
    product_id: str = Field(min_length=1, max_length=256)
    device_id: str


def status_body(ctx: Context, device_id: str) -> dict:
    now = utcnow()
    with ctx.db.connect() as conn:
        status = device_status(conn, device_id, now)
    return {"tier": status.tier, "expires_at": to_iso(status.expires_at), "checked_at": to_iso(now)}


@router.get("/status")
def sub_status(request: Request, ctx: Ctx, device_id: Annotated[str | None, Query()] = None) -> JsonUtf8Response:
    device_id = check_device_id(device_id)
    require_device(ctx, device_id, request)
    return JsonUtf8Response(status_body(ctx, device_id))


@router.post("/verify")
def sub_verify(payload: VerifyRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    device_id = check_device_id(payload.device_id)
    require_device(ctx, device_id, request)
    tiers = {ctx.settings.product_pro: PRO, ctx.settings.product_lifetime: LIFETIME}
    tier = tiers.get(payload.product_id)
    if tier is None:
        raise ApiError(422, "unknown_product")
    now = utcnow()
    with ctx.db.connect() as conn:
        existing = bound_device(conn, payload.purchase_id)
    if existing is not None and existing["device_id"] != device_id:
        raise ApiError(409, "purchase_bound_to_other_device")
    try:
        answer = ctx.store.check(payload.purchase_id, payload.product_id, now)
    except PurchaseNotFound as exc:
        raise ApiError(404, "purchase_not_found") from exc
    except StoreUnavailable as exc:
        raise ApiError(503, "store_unavailable") from exc
    with ctx.db.connect() as conn:
        try:
            record_purchase(conn, payload.purchase_id, payload.product_id, device_id, tier, answer, existing, now)
        except DeviceConflict as exc:
            raise ApiError(409, "purchase_bound_to_other_device") from exc
    return JsonUtf8Response({"state": answer.state.value, **status_body(ctx, device_id)})
