from datetime import timedelta

import pytest

from app.clock import from_iso, to_iso, utcnow
from app.rustore import Invoice

MONTH = "palma_pro_month"
QUARTER = "palma_pro_quarter"
HALF = "palma_pro_half_year"
VERIFIER = "v" * 43


@pytest.fixture
def store(client):
    return client.app.state.context.store


@pytest.fixture
def sign_in(client, device):
    def _sign_in(user="1001", device_id=None):
        payload = {"code": f"vk-{user}", "code_verifier": VERIFIER, "vk_device_id": "vkdev", "state": "s1"}
        headers = {}
        if device_id is not None:
            payload["device_id"] = device_id
            headers = device(device_id)
        return client.post("/api/account/vkid", json=payload, headers=headers)

    return _sign_in


def auth(token):
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def invoice(store):
    def _invoice(invoice_id, purchase_id, product_id=MONTH, owner=None, status="CONFIRMED"):
        store.invoices[invoice_id] = Invoice(invoice_id, purchase_id, product_id, status, owner)
        if owner is not None:
            store.owners[purchase_id] = owner
        return invoice_id

    return _invoice


def buy(client, token, invoice_id, **extra):
    return client.post("/api/account/purchases", headers=auth(token), json={"invoice_id": invoice_id, **extra})


def test_sign_in_creates_account_and_session(sign_in, client):
    response = sign_in("1001")
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["tier"] == "free"
    assert body["account"]["phone"] == "+7 ··· 10-01"
    me = client.get("/api/account", headers=auth(body["token"]))
    assert me.status_code == 200
    assert me.json()["account"]["id"] == body["account"]["id"]


def test_sign_in_twice_is_same_account(sign_in):
    first = sign_in("1001").json()
    second = sign_in("1001").json()
    assert first["account"]["id"] == second["account"]["id"]
    assert first["token"] != second["token"]


def test_tokens_are_stored_hashed(sign_in, client):
    token = sign_in("1001").json()["token"]
    with client.app.state.context.db.connect() as conn:
        stored = [row[0] for row in conn.execute("SELECT token_hash FROM account_sessions")]
    assert token not in stored
    assert len(stored) == 1


def test_rejected_code_is_401(client):
    response = client.post(
        "/api/account/vkid",
        json={"code": "bad", "code_verifier": VERIFIER, "vk_device_id": "x", "state": "s"},
    )
    assert response.status_code == 401
    assert response.json() == {"error": "vkid_rejected"}


def test_short_verifier_is_422(client):
    response = client.post(
        "/api/account/vkid",
        json={"code": "vk-1", "code_verifier": "short", "vk_device_id": "x", "state": "s"},
    )
    assert response.status_code == 422


def test_unconfigured_vkid_is_503(client):
    from app.vkid import UnconfiguredVkId

    context = client.app.state.context
    object.__setattr__(context, "vkid", UnconfiguredVkId())
    response = client.post(
        "/api/account/vkid",
        json={"code": "vk-1", "code_verifier": VERIFIER, "vk_device_id": "x", "state": "s"},
    )
    assert response.status_code == 503
    assert response.json() == {"error": "vkid_unavailable"}


def test_sign_in_with_foreign_device_key_is_403(client, device):
    device("dev-a")
    response = client.post(
        "/api/account/vkid",
        headers={"X-Device-Key": "wrong"},
        json={"code": "vk-1", "code_verifier": VERIFIER, "vk_device_id": "x", "state": "s", "device_id": "dev-a"},
    )
    assert response.status_code == 403


@pytest.mark.parametrize(
    "headers",
    [{}, {"Authorization": "Bearer nope"}, {"Authorization": "Basic abc"}, {"Authorization": "Bearer "}],
)
def test_account_requires_valid_token(client, headers):
    response = client.get("/api/account", headers=headers)
    assert response.status_code == 401
    assert response.json() == {"error": "unauthorized"}


def test_expired_session_is_401(sign_in, client):
    token = sign_in("1001").json()["token"]
    past = to_iso(utcnow() - timedelta(seconds=1))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE account_sessions SET expires_at = ?", (past,))
    assert client.get("/api/account", headers=auth(token)).status_code == 401


def test_device_pro_follows_account_to_new_phone(sign_in, subscribe, client, device):
    subscribe("old-phone", "ok-device-1")
    sign_in("1001", device_id="old-phone")
    fresh = client.get("/api/sub/status", params={"device_id": "new-phone"}, headers=device("new-phone"))
    assert fresh.json()["tier"] == "free"
    sign_in("1001", device_id="new-phone")
    moved = client.get("/api/sub/status", params={"device_id": "new-phone"}, headers=device("new-phone"))
    assert moved.json()["tier"] == "pro"
    with client.app.state.context.db.connect() as conn:
        row = conn.execute("SELECT source, invoice_id FROM entitlements").fetchone()
    assert row["source"] == "device"
    assert row["invoice_id"] is None


def test_device_pro_stays_with_account_after_logout_on_old_phone(sign_in, subscribe, client, device):
    subscribe("old-phone", "ok-device-2")
    token = sign_in("1001", device_id="old-phone").json()["token"]
    logout = client.post(
        "/api/account/logout", headers={**auth(token), **device("old-phone")}, json={"device_id": "old-phone"}
    )
    assert logout.status_code == 200
    assert client.get("/api/account", headers=auth(token)).status_code == 401
    token = sign_in("1001", device_id="new-phone").json()["token"]
    assert client.get("/api/account", headers=auth(token)).json()["tier"] == "pro"
    old = client.get("/api/sub/status", params={"device_id": "old-phone"}, headers=device("old-phone"))
    assert old.json()["tier"] == "pro"


def test_reverify_on_linked_device_updates_account_copy(sign_in, subscribe, client, device):
    subscribe("old-phone", "pause-9")
    token = sign_in("1001", device_id="old-phone").json()["token"]
    past = to_iso(utcnow() - timedelta(minutes=1))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE purchases SET expires_at = ?", (past,))
    client.post(
        "/api/sub/verify",
        headers=device("old-phone"),
        json={"purchase_id": "pause-9", "product_id": MONTH, "device_id": "old-phone"},
    )
    assert client.get("/api/account", headers=auth(token)).json()["tier"] == "free"


def test_logout_unlinks_device(sign_in, client, device):
    token = sign_in("1001", device_id="dev-1").json()["token"]
    headers = {**auth(token), **device("dev-1")}
    client.post("/api/account/logout", headers=headers, json={"device_id": "dev-1"})
    with client.app.state.context.db.connect() as conn:
        assert conn.execute("SELECT COUNT(*) FROM account_devices").fetchone()[0] == 0


def test_attach_device_after_sign_in(sign_in, client, device):
    token = sign_in("1001").json()["token"]
    response = client.post(
        "/api/account/devices", headers={**auth(token), **device("dev-2")}, json={"device_id": "dev-2"}
    )
    assert response.status_code == 200
    with client.app.state.context.db.connect() as conn:
        assert conn.execute("SELECT account_id FROM account_devices WHERE device_id = 'dev-2'").fetchone()


def test_attach_device_needs_its_key(sign_in, client, device):
    device("dev-3")
    token = sign_in("1001").json()["token"]
    response = client.post(
        "/api/account/devices", headers={**auth(token), "X-Device-Key": "x"}, json={"device_id": "dev-3"}
    )
    assert response.status_code == 403


@pytest.mark.parametrize(("product", "months"), [(MONTH, 1), (QUARTER, 3), (HALF, 6)])
def test_account_purchase_grants_pro_for_plan(sign_in, client, invoice, product, months):
    body = sign_in("1001").json()
    invoice("501", f"ok-p{months}", product, owner=body["account"]["id"])
    response = buy(client, body["token"], "501")
    assert response.status_code == 200, response.text
    result = response.json()
    assert result["state"] == "ACTIVE"
    assert result["tier"] == "pro"
    expires = from_iso(result["expires_at"])
    assert expires > utcnow() + timedelta(days=30 * months - 1)
    with client.app.state.context.db.connect() as conn:
        row = conn.execute("SELECT * FROM entitlements").fetchone()
    assert (row["source"], row["invoice_id"], row["purchase_id"]) == ("purchase", "501", f"ok-p{months}")


def test_account_purchase_reaches_every_linked_device(sign_in, client, invoice, device):
    body = sign_in("1001", device_id="dev-x").json()
    invoice("502", "ok-reach", owner=body["account"]["id"])
    buy(client, body["token"], "502")
    sign_in("1001", device_id="dev-y")
    status = client.get("/api/sub/status", params={"device_id": "dev-y"}, headers=device("dev-y"))
    assert status.json()["tier"] == "pro"


def test_purchase_of_other_account_is_409(sign_in, client, invoice):
    first = sign_in("1001").json()
    second = sign_in("2002").json()
    invoice("503", "ok-own", owner=first["account"]["id"])
    response = buy(client, second["token"], "503")
    assert response.status_code == 409
    assert response.json() == {"error": "purchase_bound_to_other_account"}


def test_purchase_reuse_by_other_account_is_409(sign_in, client, invoice):
    first = sign_in("1001").json()
    second = sign_in("2002").json()
    invoice("504", "ok-twice")
    assert buy(client, first["token"], "504").status_code == 200
    assert buy(client, second["token"], "504").status_code == 409


def test_repeat_verify_is_idempotent(sign_in, client, invoice):
    body = sign_in("1001").json()
    invoice("505", "ok-again", owner=body["account"]["id"])
    buy(client, body["token"], "505")
    buy(client, body["token"], "505")
    with client.app.state.context.db.connect() as conn:
        assert conn.execute("SELECT COUNT(*) FROM entitlements").fetchone()[0] == 1


def test_unknown_invoice_is_404(sign_in, client):
    token = sign_in("1001").json()["token"]
    response = buy(client, token, "999")
    assert response.status_code == 404
    assert response.json() == {"error": "purchase_not_found"}


def test_unpaid_invoice_is_409(sign_in, client, invoice):
    token = sign_in("1001").json()["token"]
    invoice("506", "ok-unpaid", status="CANCELLED")
    response = buy(client, token, "506")
    assert response.status_code == 409
    assert response.json() == {"error": "invoice_not_paid"}


def test_invoice_mismatch_is_409(sign_in, client, invoice):
    token = sign_in("1001").json()["token"]
    invoice("507", "ok-real")
    assert buy(client, token, "507", purchase_id="ok-fake").json() == {"error": "invoice_mismatch"}
    assert buy(client, token, "507", product_id=HALF).json() == {"error": "invoice_mismatch"}


def test_unknown_product_is_422(sign_in, client, invoice):
    token = sign_in("1001").json()["token"]
    invoice("508", "ok-other", "palma_pro_year")
    response = buy(client, token, "508")
    assert response.status_code == 422
    assert response.json() == {"error": "unknown_product"}


def test_terminated_purchase_gives_free(sign_in, client, invoice):
    token = sign_in("1001").json()["token"]
    invoice("509", "stop-9")
    response = buy(client, token, "509")
    assert response.json()["state"] == "TERMINATED"
    assert response.json()["tier"] == "free"


def test_unconfigured_store_is_503(sign_in, client):
    from app.rustore import UnconfiguredStore

    token = sign_in("1001").json()["token"]
    object.__setattr__(client.app.state.context, "store", UnconfiguredStore())
    response = buy(client, token, "510")
    assert response.status_code == 503
    assert response.json() == {"error": "store_unavailable"}


def test_account_status_rechecks_expiring_purchase(sign_in, client, invoice):
    body = sign_in("1001").json()
    invoice("511", "ok-renew", owner=body["account"]["id"])
    buy(client, body["token"], "511")
    old = to_iso(utcnow() - timedelta(hours=2))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE entitlements SET expires_at = ?, checked_at = ?", (old, old))
    status = client.get("/api/account", headers=auth(body["token"])).json()
    assert status["tier"] == "pro"
    assert from_iso(status["expires_at"]) > utcnow() + timedelta(days=29)


def test_device_verify_of_account_purchase_on_unlinked_device_is_409(sign_in, client, invoice, device):
    body = sign_in("1001", device_id="dev-own").json()
    invoice("512", "ok-acc", owner=body["account"]["id"])
    buy(client, body["token"], "512")
    response = client.post(
        "/api/sub/verify",
        headers=device("dev-thief"),
        json={"purchase_id": "ok-acc", "product_id": MONTH, "device_id": "dev-thief"},
    )
    assert response.status_code == 409
    assert response.json() == {"error": "purchase_bound_to_other_account"}


def test_device_verify_of_account_purchase_on_linked_device_works(sign_in, client, invoice, device):
    body = sign_in("1001", device_id="dev-own").json()
    invoice("513", "ok-acc2", owner=body["account"]["id"])
    response = client.post(
        "/api/sub/verify",
        headers={**device("dev-own"), **auth(body["token"])},
        json={"purchase_id": "ok-acc2", "product_id": MONTH, "device_id": "dev-own", "invoice_id": "513"},
    )
    assert response.status_code == 200, response.text
    assert response.json()["tier"] == "pro"


def test_admin_lists_accounts_without_raw_phone(sign_in, client, admin_headers, subscribe):
    subscribe("dev-adm", "ok-adm")
    sign_in("1001", device_id="dev-adm")
    response = client.get("/api/admin/accounts", headers=admin_headers)
    assert response.status_code == 200
    item = response.json()["accounts"][0]
    assert item["tier"] == "pro"
    assert item["devices"] == 1
    assert item["phone"] == "+7 ··· 10-01"
    assert "79000001001" not in response.text


def test_admin_accounts_needs_token(client):
    assert client.get("/api/admin/accounts").status_code == 403
