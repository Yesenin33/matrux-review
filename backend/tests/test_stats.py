from datetime import timedelta

import pytest

from app.clock import from_iso, to_iso, utcnow
from app.rustore import Invoice

MONTH = "palma_pro_month"
QUARTER = "palma_pro_quarter"
HALF = "palma_pro_half_year"
EMPTY_PLANS = {"1m": 0, "3m": 0, "6m": 0, "lifetime": 0, "other": 0}


@pytest.fixture
def stats(client, admin_headers):
    def _stats():
        response = client.get("/api/admin/stats", headers=admin_headers)
        assert response.status_code == 200, response.text
        return response.json()

    return _stats


@pytest.fixture
def buy(client, account):
    store = client.app.state.context.store

    def _buy(device_id, invoice_id, purchase_id, product=MONTH, user=None):
        headers = account(device_id, user)
        me = client.get("/api/account", headers=headers).json()
        store.invoices[invoice_id] = Invoice(invoice_id, purchase_id, product, "CONFIRMED", me["account"]["id"])
        response = client.post("/api/account/purchases", headers=headers, json={"invoice_id": invoice_id})
        assert response.status_code == 200, response.text
        return headers

    return _buy


def db(client):
    return client.app.state.context.db


def test_stats_needs_admin_token(client):
    assert client.get("/api/admin/stats").status_code == 403
    assert client.get("/api/admin/stats", headers={"X-Admin-Token": "nope"}).status_code == 403


def test_empty_base_is_all_zero(stats):
    body = stats()
    assert body["devices"] == {"total": 0, "pro": 0, "new_7d": 0, "new_30d": 0}
    assert body["accounts"] == {"total": 0, "pro": 0, "new_7d": 0, "new_30d": 0}
    assert body["subscriptions"] == {"active": EMPTY_PLANS, "active_total": 0, "expired": 0, "purchases_total": 0}


def test_anonymous_device_counts_as_device_not_pro(stats, device):
    device("anon-1")
    body = stats()
    assert body["devices"] == {"total": 1, "pro": 0, "new_7d": 1, "new_30d": 1}
    assert body["accounts"]["total"] == 0


def test_register_sets_last_seen(client, device):
    device("anon-1")
    with db(client).connect() as conn:
        row = conn.execute("SELECT created_at, last_seen_at FROM devices WHERE device_id = 'anon-1'").fetchone()
    assert row["last_seen_at"] == row["created_at"]


def test_status_and_manifest_refresh_last_seen(client, device):
    headers = device("anon-1")
    with db(client).connect() as conn:
        conn.execute("UPDATE devices SET last_seen_at = NULL")
    client.get("/api/sub/status", params={"device_id": "anon-1"}, headers=headers)
    with db(client).connect() as conn:
        seen = conn.execute("SELECT last_seen_at FROM devices").fetchone()[0]
        conn.execute("UPDATE devices SET last_seen_at = NULL")
    assert seen is not None
    client.get("/api/manifest", params={"device_id": "anon-1"}, headers=headers)
    with db(client).connect() as conn:
        assert conn.execute("SELECT last_seen_at FROM devices").fetchone()[0] is not None


def test_wrong_key_does_not_touch_last_seen(client, device):
    device("anon-1")
    with db(client).connect() as conn:
        conn.execute("UPDATE devices SET last_seen_at = NULL")
    client.get("/api/manifest", params={"device_id": "anon-1"}, headers={"X-Device-Key": "wrong"})
    client.get("/api/sub/status", params={"device_id": "anon-1"}, headers={"X-Device-Key": "wrong"})
    with db(client).connect() as conn:
        assert conn.execute("SELECT last_seen_at FROM devices").fetchone()[0] is None


def test_manifest_without_device_still_works(client):
    assert client.get("/api/manifest").status_code == 200
    assert client.get("/api/manifest", params={"device_id": "../bad"}, headers={"X-Device-Key": "k"}).status_code == 200


def test_sign_in_creates_account_and_links_device(stats, client, account):
    account("dev-1")
    body = stats()
    assert body["accounts"] == {"total": 1, "pro": 0, "new_7d": 1, "new_30d": 1}
    assert body["devices"]["total"] == 1
    assert body["devices"]["pro"] == 0
    with db(client).connect() as conn:
        assert conn.execute("SELECT COUNT(*) FROM account_devices").fetchone()[0] == 1
        assert conn.execute("SELECT last_seen_at FROM accounts").fetchone()[0] is not None


def test_purchase_makes_account_and_device_pro(stats, buy):
    buy("dev-1", "501", "ok-1")
    body = stats()
    assert body["accounts"]["pro"] == 1
    assert body["devices"]["pro"] == 1
    assert body["subscriptions"]["active"] == {**EMPTY_PLANS, "1m": 1}
    assert body["subscriptions"]["purchases_total"] == 1


def test_pro_reaches_second_device_of_same_account(stats, buy, account):
    buy("dev-1", "501", "ok-1", user="u1")
    account("dev-2", "u1")
    body = stats()
    assert body["accounts"] == {"total": 1, "pro": 1, "new_7d": 1, "new_30d": 1}
    assert body["devices"]["total"] == 2
    assert body["devices"]["pro"] == 2


def test_expired_subscription_leaves_active(stats, buy, client):
    buy("dev-1", "501", "ok-1")
    past = to_iso(utcnow() - timedelta(days=1))
    with db(client).connect() as conn:
        conn.execute("UPDATE entitlements SET expires_at = ?", (past,))
    body = stats()
    assert body["accounts"]["pro"] == 0
    assert body["devices"]["pro"] == 0
    assert body["subscriptions"]["active_total"] == 0
    assert body["subscriptions"]["expired"] == 1
    assert body["subscriptions"]["purchases_total"] == 1
    assert body["accounts"]["total"] == 1


def test_expired_account_stays_in_admin_list(buy, client, admin_headers):
    buy("dev-1", "501", "ok-1")
    with db(client).connect() as conn:
        conn.execute("UPDATE entitlements SET expires_at = ?", (to_iso(utcnow() - timedelta(days=1)),))
    accounts = client.get("/api/admin/accounts", headers=admin_headers).json()["accounts"]
    assert len(accounts) == 1
    assert accounts[0]["tier"] == "free"


def test_terminated_purchase_counts_as_expired(stats, buy):
    buy("dev-1", "501", "stop-1")
    subs = stats()["subscriptions"]
    assert subs["active_total"] == 0
    assert subs["expired"] == 1


def test_plans_match_purchases(stats, buy):
    buy("dev-1", "501", "ok-1", MONTH, user="u1")
    buy("dev-2", "502", "ok-2", QUARTER, user="u2")
    buy("dev-3", "503", "ok-3", HALF, user="u3")
    buy("dev-4", "504", "ok-4", HALF, user="u4")
    subs = stats()["subscriptions"]
    assert subs["active"] == {**EMPTY_PLANS, "1m": 1, "3m": 1, "6m": 2}
    assert subs["active_total"] == subs["purchases_total"] == 4
    assert sum(subs["active"].values()) == 4


def test_device_purchase_adopted_by_account_counts_once(stats, client, device, account):
    response = client.post(
        "/api/sub/verify",
        headers={**device("dev-1"), **account("dev-1")},
        json={"purchase_id": "ok-d", "product_id": QUARTER, "device_id": "dev-1"},
    )
    assert response.status_code == 200, response.text
    subs = stats()["subscriptions"]
    assert subs["purchases_total"] == 1
    assert subs["active"] == {**EMPTY_PLANS, "3m": 1}


def test_new_counts_follow_creation_date(stats, client, device, account):
    device("fresh")
    account("old-dev", "old-user")
    with db(client).connect() as conn:
        conn.execute("UPDATE devices SET created_at = ? WHERE device_id = 'old-dev'", (to_iso(utcnow() - timedelta(days=10)),))
        conn.execute("UPDATE accounts SET created_at = ?", (to_iso(utcnow() - timedelta(days=40)),))
    body = stats()
    assert body["devices"] == {"total": 2, "pro": 0, "new_7d": 1, "new_30d": 2}
    assert body["accounts"] == {"total": 1, "pro": 0, "new_7d": 0, "new_30d": 0}


def test_generated_at_is_now(stats):
    generated = from_iso(stats()["generated_at"])
    assert abs((generated - utcnow()).total_seconds()) < 5
