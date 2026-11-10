from datetime import datetime, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field

from app.accounts import (
    SOURCE_PURCHASE,
    Account,
    OwnedElsewhere,
    bearer,
    issue_session,
    link_device,
    masked_phone,
    owner_matches,
    record_entitlement,
    revoke_session,
    session_account,
    unlink_device,
    upsert_account,
)
from app.clock import from_iso, utcnow
from app.context import Context, check_device_id, get_context
from app.devices import provided_key, seen_device
from app.errors import ApiError, JsonUtf8Response
from app.routes.admin import require_admin
from app.routes.subs import product_tier, status_json, store_answer, store_invoice
from app.rustore import Invoice, PurchaseNotFound, PurchaseState, StoreAnswer, StoreUnavailable
from app.subscriptions import LIFETIME, account_status
from app.vkid import AuthCode, AuthRejected, AuthUnavailable

router = APIRouter(prefix="/api/account")
admin_router = APIRouter(prefix="/api/admin", dependencies=[Depends(require_admin)])

Ctx = Annotated[Context, Depends(get_context)]

RECHECK_AFTER = timedelta(hours=1)
RECHECK_AHEAD = timedelta(days=1)


class VkIdRequest(BaseModel):
    code: str = Field(min_length=1, max_length=4096)
    code_verifier: str = Field(min_length=43, max_length=128)
    vk_device_id: str = Field(min_length=1, max_length=512)
    state: str = Field(min_length=1, max_length=512)
    device_id: str | None = None


class DeviceRequest(BaseModel):
    device_id: str


class LogoutRequest(BaseModel):
    device_id: str | None = None


class PurchaseRequest(BaseModel):
    invoice_id: str = Field(min_length=1, max_length=64)
    purchase_id: str | None = Field(default=None, min_length=1, max_length=256)
    product_id: str | None = Field(default=None, min_length=1, max_length=256)


def owned_device(ctx: Context, device_id: str | None, request: Request) -> str | None:
    if device_id is None:
        return None
    device_id = check_device_id(device_id)
    with ctx.db.connect() as conn:
        if not seen_device(conn, device_id, provided_key(request)):
            raise ApiError(403, "forbidden")
    return device_id


def current_account(ctx: Context, request: Request) -> tuple[Account, str]:
    token = bearer(request)
    with ctx.db.connect() as conn:
        return session_account(conn, token, utcnow()), token


def account_body(ctx: Context, account: Account) -> dict:
    now = utcnow()
    with ctx.db.connect() as conn:
        status = account_status(conn, account.id, now)
    return {"account": {"id": str(account.id), "phone": masked_phone(account.phone)}, **status_json(status, now)}


def stale(row, now: datetime) -> bool:
    checked = from_iso(row["checked_at"])
    expires = from_iso(row["expires_at"])
    if row["tier"] == LIFETIME or row["state"] == PurchaseState.TERMINATED or expires is None:
        return False
    return expires <= now + RECHECK_AHEAD and (checked is None or checked <= now - RECHECK_AFTER)


def recheck_purchases(ctx: Context, account: Account) -> None:
    now = utcnow()
    with ctx.db.connect() as conn:
        rows = conn.execute(
            "SELECT * FROM entitlements WHERE account_id = ? AND source = ?", (account.id, SOURCE_PURCHASE)
        ).fetchall()
    for row in rows:
        if not stale(row, now):
            continue
        try:
            answer = ctx.store.check(row["purchase_id"], row["product_id"], now)
        except (PurchaseNotFound, StoreUnavailable):
            continue
        invoice = Invoice(row["invoice_id"], row["purchase_id"], row["product_id"], "CONFIRMED")
        with ctx.db.connect() as conn:
            try:
                record_entitlement(conn, account.id, invoice, row["tier"], answer, now)
            except OwnedElsewhere:
                continue


@router.post("/vkid")
def sign_in(payload: VkIdRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    device_id = owned_device(ctx, payload.device_id, request)
    auth = AuthCode(payload.code, payload.code_verifier, payload.vk_device_id, payload.state)
    try:
        user = ctx.vkid.exchange(auth)
    except AuthRejected as exc:
        raise ApiError(401, "vkid_rejected") from exc
    except AuthUnavailable as exc:
        raise ApiError(503, "vkid_unavailable") from exc
    now = utcnow()
    with ctx.db.connect() as conn:
        account = upsert_account(conn, user, now)
        token = issue_session(conn, account.id, now, ctx.settings.session_days)
        if device_id is not None:
            link_device(conn, device_id, account.id, now)
    return JsonUtf8Response({"token": token, **account_body(ctx, account)})


@router.get("")
def me(request: Request, ctx: Ctx) -> JsonUtf8Response:
    account, _ = current_account(ctx, request)
    recheck_purchases(ctx, account)
    return JsonUtf8Response(account_body(ctx, account))


@router.post("/devices")
def attach_device(payload: DeviceRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    account, _ = current_account(ctx, request)
    device_id = owned_device(ctx, payload.device_id, request)
    with ctx.db.connect() as conn:
        link_device(conn, device_id, account.id, utcnow())
    return JsonUtf8Response(account_body(ctx, account))


@router.post("/logout")
def sign_out(payload: LogoutRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    account, token = current_account(ctx, request)
    device_id = owned_device(ctx, payload.device_id, request)
    with ctx.db.connect() as conn:
        if device_id is not None:
            unlink_device(conn, device_id, account.id)
        revoke_session(conn, token)
    return JsonUtf8Response({"ok": True})


@router.post("/purchases")
def verify_purchase(payload: PurchaseRequest, request: Request, ctx: Ctx) -> JsonUtf8Response:
    account, _ = current_account(ctx, request)
    invoice = store_invoice(ctx, payload.invoice_id, payload.purchase_id, payload.product_id)
    tier = product_tier(ctx, invoice.product_id)
    now = utcnow()
    if tier == LIFETIME:
        answer = StoreAnswer(PurchaseState.ACTIVE, now, invoice.app_user_id)
    else:
        answer = store_answer(ctx, invoice.purchase_id, invoice.product_id)
    if not owner_matches(account.id, invoice.app_user_id, answer.app_user_id):
        raise ApiError(409, "purchase_bound_to_other_account")
    with ctx.db.connect() as conn:
        try:
            record_entitlement(conn, account.id, invoice, tier, answer, now)
        except OwnedElsewhere as exc:
            raise ApiError(409, "purchase_bound_to_other_account") from exc
    return JsonUtf8Response({"state": answer.state.value, **account_body(ctx, account)})


@admin_router.get("/accounts")
def list_accounts(ctx: Ctx) -> JsonUtf8Response:
    now = utcnow()
    with ctx.db.connect() as conn:
        rows = conn.execute("SELECT * FROM accounts ORDER BY id DESC LIMIT 500").fetchall()
        items = []
        for row in rows:
            status = account_status(conn, row["id"], now)
            devices = conn.execute(
                "SELECT COUNT(*) FROM account_devices WHERE account_id = ?", (row["id"],)
            ).fetchone()[0]
            items.append(
                {
                    "id": str(row["id"]),
                    "phone": masked_phone(row["phone"]),
                    "created_at": row["created_at"],
                    "devices": devices,
                    **status_json(status, now),
                }
            )
    return JsonUtf8Response({"accounts": items})
