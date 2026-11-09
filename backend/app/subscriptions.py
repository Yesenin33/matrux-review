import sqlite3
from dataclasses import dataclass
from datetime import datetime, timedelta

from app.clock import from_iso, to_iso
from app.rustore import PurchaseState, StoreAnswer

FREE, PRO, LIFETIME = "free", "pro", "lifetime"
RANK = {FREE: 0, PRO: 1, LIFETIME: 2}


class DeviceConflict(Exception):
    pass


@dataclass(frozen=True)
class DeviceStatus:
    tier: str
    expires_at: datetime | None

    @property
    def paid(self) -> bool:
        return self.tier != FREE


def effective_tier(row: sqlite3.Row, now: datetime) -> str:
    if row["state"] == PurchaseState.TERMINATED:
        return FREE
    if row["tier"] == LIFETIME:
        return LIFETIME
    expires = from_iso(row["expires_at"])
    return PRO if expires and expires > now else FREE


def device_status(conn: sqlite3.Connection, device_id: str, now: datetime) -> DeviceStatus:
    rows = conn.execute("SELECT * FROM purchases WHERE device_id = ?", (device_id,)).fetchall()
    best = DeviceStatus(FREE, None)
    for row in rows:
        tier = effective_tier(row, now)
        expires = None if tier == LIFETIME else from_iso(row["expires_at"]) if tier == PRO else None
        if RANK[tier] > RANK[best.tier]:
            best = DeviceStatus(tier, expires)
        elif tier == PRO == best.tier and expires and best.expires_at and expires > best.expires_at:
            best = DeviceStatus(tier, expires)
    return best


def bound_device(conn: sqlite3.Connection, purchase_id: str) -> sqlite3.Row | None:
    return conn.execute("SELECT * FROM purchases WHERE purchase_id = ?", (purchase_id,)).fetchone()


def record_purchase(
    conn: sqlite3.Connection,
    purchase_id: str,
    product_id: str,
    device_id: str,
    tier: str,
    answer: StoreAnswer,
    existing: sqlite3.Row | None,
    now: datetime,
) -> None:
    if tier == LIFETIME and answer.state != PurchaseState.TERMINATED:
        expires = None
    elif answer.state == PurchaseState.PAUSED and existing and existing["expires_at"]:
        expires = from_iso(existing["expires_at"])
    else:
        expires = answer.paid_until
    cursor = conn.execute(
        """
        INSERT INTO purchases (purchase_id, product_id, device_id, tier, state, expires_at, checked_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (purchase_id) DO UPDATE SET
            product_id = excluded.product_id,
            tier = excluded.tier,
            state = excluded.state,
            expires_at = excluded.expires_at,
            checked_at = excluded.checked_at
        WHERE purchases.device_id = excluded.device_id
        """,
        (purchase_id, product_id, device_id, tier, answer.state.value, to_iso(expires), to_iso(now)),
    )
    if cursor.rowcount == 0:
        raise DeviceConflict


def summary(conn: sqlite3.Connection, now: datetime) -> dict[str, int]:
    devices: dict[str, DeviceStatus] = {}
    for (device_id,) in conn.execute("SELECT DISTINCT device_id FROM purchases"):
        devices[device_id] = device_status(conn, device_id, now)
    horizon = now + timedelta(days=7)
    return {
        "total": len(devices),
        "pro": sum(1 for s in devices.values() if s.tier == PRO),
        "lifetime": sum(1 for s in devices.values() if s.tier == LIFETIME),
        "expiring_7d": sum(1 for s in devices.values() if s.tier == PRO and s.expires_at and s.expires_at <= horizon),
    }
