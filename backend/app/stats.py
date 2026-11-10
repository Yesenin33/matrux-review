import sqlite3
from datetime import datetime, timedelta

from app.clock import from_iso, to_iso
from app.subscriptions import FREE, LIFETIME, account_status, device_status, effective_tier

PLAN_KEYS = {1: "1m", 3: "3m", 6: "6m"}


def known_devices(conn: sqlite3.Connection) -> list[str]:
    rows = conn.execute(
        """
        SELECT device_id FROM devices
        UNION SELECT device_id FROM purchases
        UNION SELECT device_id FROM account_devices
        """
    ).fetchall()
    return [row["device_id"] for row in rows]


def created_since(conn: sqlite3.Connection, table: str, now: datetime, days: int) -> int:
    since = now - timedelta(days=days)
    rows = conn.execute(f"SELECT created_at FROM {table}").fetchall()
    return sum(1 for row in rows if (created := from_iso(row["created_at"])) and created > since)


def unique_purchases(conn: sqlite3.Connection) -> list[sqlite3.Row]:
    latest: dict[str, sqlite3.Row] = {}
    rows = [
        *conn.execute("SELECT purchase_id, product_id, tier, state, expires_at, checked_at FROM purchases"),
        *conn.execute("SELECT purchase_id, product_id, tier, state, expires_at, checked_at FROM entitlements"),
    ]
    for row in rows:
        known = latest.get(row["purchase_id"])
        if known is None or (row["checked_at"] or "") >= (known["checked_at"] or ""):
            latest[row["purchase_id"]] = row
    return list(latest.values())


def plan_of(row: sqlite3.Row, months: dict[str, int]) -> str:
    if row["tier"] == LIFETIME:
        return "lifetime"
    return PLAN_KEYS.get(months.get(row["product_id"], 0), "other")


def subscriptions(conn: sqlite3.Connection, now: datetime, months: dict[str, int]) -> dict:
    active = {key: 0 for key in [*PLAN_KEYS.values(), "lifetime", "other"]}
    expired = 0
    purchases = unique_purchases(conn)
    for row in purchases:
        if effective_tier(row, now) == FREE:
            expired += 1
        else:
            active[plan_of(row, months)] += 1
    return {
        "active": active,
        "active_total": sum(active.values()),
        "expired": expired,
        "purchases_total": len(purchases),
    }


def device_counts(conn: sqlite3.Connection, now: datetime) -> dict[str, int]:
    devices = known_devices(conn)
    return {
        "total": len(devices),
        "pro": sum(1 for device_id in devices if device_status(conn, device_id, now).paid),
        "new_7d": created_since(conn, "devices", now, 7),
        "new_30d": created_since(conn, "devices", now, 30),
    }


def account_counts(conn: sqlite3.Connection, now: datetime) -> dict[str, int]:
    ids = [row["id"] for row in conn.execute("SELECT id FROM accounts")]
    return {
        "total": len(ids),
        "pro": sum(1 for account_id in ids if account_status(conn, account_id, now).paid),
        "new_7d": created_since(conn, "accounts", now, 7),
        "new_30d": created_since(conn, "accounts", now, 30),
    }


def user_stats(conn: sqlite3.Connection, now: datetime, months: dict[str, int]) -> dict:
    return {
        "devices": device_counts(conn, now),
        "accounts": account_counts(conn, now),
        "subscriptions": subscriptions(conn, now, months),
        "generated_at": to_iso(now),
    }
