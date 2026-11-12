from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query, Request
from pydantic import BaseModel, Field

from app.accounts import bearer, link_device, owner_matches, session_account, sync_device
from app.clock import to_iso, utcnow
from app.context import Context, check_device_id, get_context
from app.devices import require_device
from app.errors import ApiError, JsonUtf8Response
from app.rustore import Invoice, PurchaseNotFound, StoreAnswer, StoreUnavailable
from app.signed import signed_json
from app.subscriptions import (
    LIFETIME,
    PRO,
    DeviceConflict,
    DeviceStatus,
    bound_device,
    device_status,
    linked_account,
    record_purchase,
)

router = APIRouter(prefix="/api/sub")

Ctx = Annotated[Context, Depends(get_context)]


class VerifyRequest(BaseModel):
    purchase_id: str = Field(min_length=1, max_length=256)
    product_id: str = Field(min_length=1, max_length=256)
    device_id: str
    invoice_id: str | None = Field(default=None, min_length=1, max_length=64)


def product_tier(ctx: Context, product_id: str) -> str:
    tiers = {product: PRO for product in ctx.settings.pro_months}
    tiers[ctx.settings.product_lifetime] = LIFETIME
    tier = tiers.get(product_id)
    if tier is None:
        raise ApiError(422, "unknown_product")
    return tier


def store_invoice(ctx: Context, invoice_id: str, purchase_id: str | None, product_id: str | None) -> Invoice:
    try:
        invoice = ctx.store.invoice(invoice_id)
    except PurchaseNotFound as exc:
        raise ApiError(404, "purchase_not_found") from exc
    except StoreUnavailable as exc:
        raise ApiError(503, "store_unavailable") from exc
    if purchase_id is not None and invoice.purchase_id != purchase_id:
        raise ApiError(409, "invoice_mismatch")
    if product_id is not None and invoice.product_id != product_id:
        raise ApiError(409, "invoice_mismatch")
    if not invoice.paid:
        raise ApiError(409, "invoice_not_paid")
    return invoice


def store_answer(ctx: Context, purchase_id: str, product_id: str) -> StoreAnswer:
    try:
        return ctx.store.check(purchase_id, product_id, utcnow())
    except PurchaseNotFound as exc:
        raise ApiError(404, "purchase_not_found") from exc
    except StoreUnavailable as exc:
        raise ApiError(503, "store_unavailable") from exc


def status_json(status: DeviceStatus, now: datetime) -> dict:
    return {"tier": status.tier, "expires_at": to_iso(status.expires_at), "checked_at": to_iso(now)}


def status_body(ctx: Context, device_id: str) -> dict:
    now = utcnow()
    with ctx.db.connect() as conn:
        status = device_status(conn, device_id, now)
    return status_json(status, now)


@router.get("/status")
def sub_status(request: Request, ctx: Ctx, device_id: Annotated[str | None, Query()] = None) -> JsonUtf8Response:
    device_id = check_device_id(device_id)
    require_device(ctx, device_id, request)
    return signed_json(ctx, status_body(ctx, device_id))


@router.post("/verify")
def sub_verify(payload: VerifyRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    device_id = check_device_id(payload.device_id)
    require_device(ctx, device_id, request)
    tier = product_tier(ctx, payload.product_id)
    now = utcnow()
    with ctx.db.connect() as conn:
        existing = bound_device(conn, payload.purchase_id)
    if existing is not None and existing["device_id"] != device_id:
        raise ApiError(409, "purchase_bound_to_other_device")
    owners: list[str | None] = []
    if payload.invoice_id is not None:
        owners.append(store_invoice(ctx, payload.invoice_id, payload.purchase_id, payload.product_id).app_user_id)
    answer = store_answer(ctx, payload.purchase_id, payload.product_id)
    owners.append(answer.app_user_id)
    with ctx.db.connect() as conn:
        account_id = linked_account(conn, device_id)
        if any(owners) and (account_id is None or not owner_matches(account_id, *owners)):
            raise ApiError(409, "purchase_bound_to_other_account")
        if existing is None:
            buyer = session_account(conn, bearer(request), now).id
            if account_id is not None and account_id != buyer:
                raise ApiError(409, "purchase_bound_to_other_account")
            if not owner_matches(buyer, *owners):
                raise ApiError(409, "purchase_bound_to_other_account")
            if account_id is None:
                link_device(conn, device_id, buyer, now)
        try:
            record_purchase(conn, payload.purchase_id, payload.product_id, device_id, tier, answer, existing, now)
        except DeviceConflict as exc:
            raise ApiError(409, "purchase_bound_to_other_device") from exc
        sync_device(conn, device_id)
    return signed_json(ctx, {"state": answer.state.value, **status_body(ctx, device_id)})
