import hashlib
import secrets
import sqlite3
from dataclasses import dataclass
from datetime import datetime, timedelta

from fastapi import Request

from app.clock import from_iso, to_iso
from app.errors import ApiError
from app.rustore import Invoice, PurchaseState, StoreAnswer
from app.subscriptions import LIFETIME, linked_account
from app.vkid import VkUser

SOURCE_PURCHASE, SOURCE_DEVICE = "purchase", "device"


class OwnedElsewhere(Exception):
    pass


@dataclass(frozen=True)
class Account:
    id: int
    vk_user_id: str
    phone: str | None
    email: str | None
    created_at: str


def new_token() -> str:
    return secrets.token_urlsafe(32)


def token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8", "surrogateescape")).hexdigest()


def masked_phone(phone: str | None) -> str | None:
    if not phone:
        return None
    digits = "".join(ch for ch in phone if ch.isdigit())
    if len(digits) < 4:
        return None
    return f"+{digits[:1]} ··· {digits[-4:-2]}-{digits[-2:]}"


def account_from(row: sqlite3.Row) -> Account:
    return Account(row["id"], row["vk_user_id"], row["phone"], row["email"], row["created_at"])


def upsert_account(conn: sqlite3.Connection, user: VkUser, now: datetime) -> Account:
    conn.execute(
        """
        INSERT INTO accounts (vk_user_id, phone, email, created_at, last_seen_at) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (vk_user_id) DO UPDATE SET
            phone = COALESCE(excluded.phone, accounts.phone),
            email = COALESCE(excluded.email, accounts.email),
            last_seen_at = excluded.last_seen_at
        """,
        (user.user_id, user.phone, user.email, to_iso(now), to_iso(now)),
    )
    row = conn.execute("SELECT * FROM accounts WHERE vk_user_id = ?", (user.user_id,)).fetchone()
    return account_from(row)


def issue_session(conn: sqlite3.Connection, account_id: int, now: datetime, days: int) -> str:
    token = new_token()
    conn.execute(
        "INSERT INTO account_sessions (token_hash, account_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
        (token_hash(token), account_id, to_iso(now), to_iso(now + timedelta(days=days))),
    )
    return token


def bearer(request: Request) -> str:
    header = request.headers.get("authorization", "")
    scheme, _, token = header.partition(" ")
    if scheme.lower() != "bearer" or not token.strip():
        raise ApiError(401, "unauthorized")
    return token.strip()


def session_account(conn: sqlite3.Connection, token: str, now: datetime) -> Account:
    digest = token_hash(token)
    row = conn.execute(
        """
        SELECT accounts.*, account_sessions.expires_at AS session_until
        FROM account_sessions JOIN accounts ON accounts.id = account_sessions.account_id
        WHERE account_sessions.token_hash = ?
        """,
        (digest,),
    ).fetchone()
    if row is None:
        raise ApiError(401, "unauthorized")
    until = from_iso(row["session_until"])
    if until is None or until <= now:
        conn.execute("DELETE FROM account_sessions WHERE token_hash = ?", (digest,))
        raise ApiError(401, "unauthorized")
    conn.execute("UPDATE accounts SET last_seen_at = ? WHERE id = ?", (to_iso(now), row["id"]))
    return account_from(row)


def revoke_session(conn: sqlite3.Connection, token: str) -> None:
    conn.execute("DELETE FROM account_sessions WHERE token_hash = ?", (token_hash(token),))


def link_device(conn: sqlite3.Connection, device_id: str, account_id: int, now: datetime) -> None:
    conn.execute(
        """
        INSERT INTO account_devices (device_id, account_id, linked_at) VALUES (?, ?, ?)
        ON CONFLICT (device_id) DO UPDATE SET account_id = excluded.account_id, linked_at = excluded.linked_at
        """,
        (device_id, account_id, to_iso(now)),
    )
    adopt_device_purchases(conn, device_id, account_id)


def unlink_device(conn: sqlite3.Connection, device_id: str, account_id: int) -> None:
    conn.execute("DELETE FROM account_devices WHERE device_id = ? AND account_id = ?", (device_id, account_id))


def adopt_device_purchases(conn: sqlite3.Connection, device_id: str, account_id: int) -> None:
    conn.execute(
        """
        INSERT INTO entitlements
            (account_id, tier, state, expires_at, source, invoice_id, purchase_id, product_id, checked_at)
        SELECT ?, tier, state, expires_at, ?, NULL, purchase_id, product_id, checked_at
        FROM purchases WHERE device_id = ?
        ON CONFLICT (purchase_id) DO UPDATE SET
            tier = excluded.tier,
            state = excluded.state,
            expires_at = excluded.expires_at,
            product_id = excluded.product_id,
            checked_at = excluded.checked_at
        WHERE entitlements.account_id = excluded.account_id AND entitlements.source = excluded.source
        """,
        (account_id, SOURCE_DEVICE, device_id),
    )


def sync_device(conn: sqlite3.Connection, device_id: str) -> None:
    account_id = linked_account(conn, device_id)
    if account_id is not None:
        adopt_device_purchases(conn, device_id, account_id)


def purchase_owner(conn: sqlite3.Connection, purchase_id: str, invoice_id: str | None) -> sqlite3.Row | None:
    return conn.execute(
        "SELECT * FROM entitlements WHERE purchase_id = ? OR (invoice_id IS NOT NULL AND invoice_id = ?)",
        (purchase_id, invoice_id),
    ).fetchone()


def record_entitlement(
    conn: sqlite3.Connection,
    account_id: int,
    invoice: Invoice,
    tier: str,
    answer: StoreAnswer,
    now: datetime,
) -> None:
    existing = purchase_owner(conn, invoice.purchase_id, invoice.invoice_id)
    if existing is not None and existing["account_id"] != account_id:
        raise OwnedElsewhere
    if tier == LIFETIME and answer.state != PurchaseState.TERMINATED:
        expires = None
    elif answer.state == PurchaseState.PAUSED and existing and existing["expires_at"]:
        expires = from_iso(existing["expires_at"])
    else:
        expires = answer.paid_until
    conn.execute(
        """
        INSERT INTO entitlements
            (account_id, tier, state, expires_at, source, invoice_id, purchase_id, product_id, checked_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (purchase_id) DO UPDATE SET
            tier = excluded.tier,
            state = excluded.state,
            expires_at = excluded.expires_at,
            source = excluded.source,
            invoice_id = excluded.invoice_id,
            product_id = excluded.product_id,
            checked_at = excluded.checked_at
        """,
        (
            account_id,
            tier,
            answer.state.value,
            to_iso(expires),
            SOURCE_PURCHASE,
            invoice.invoice_id,
            invoice.purchase_id,
            invoice.product_id,
            to_iso(now),
        ),
    )


def owner_matches(account_id: int, *claims: str | None) -> bool:
    return all(claim is None or claim == str(account_id) for claim in claims)
