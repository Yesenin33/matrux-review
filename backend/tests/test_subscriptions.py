from datetime import timedelta

import pytest

from app.clock import from_iso, to_iso, utcnow

PRO = "palma_pro_month"
LIFETIME = "palma_lifetime"


@pytest.fixture
def verify(client, device, account):
    def _verify(purchase_id, device_id="dev-1", product_id=PRO):
        return client.post(
            "/api/sub/verify",
            headers={**device(device_id), **account(device_id)},
            json={"purchase_id": purchase_id, "product_id": product_id, "device_id": device_id},
        )

    return _verify


@pytest.fixture
def status(client, device):
    def _status(device_id):
        return client.get("/api/sub/status", params={"device_id": device_id}, headers=device(device_id))

    return _status


def test_status_for_new_device_is_free(status):
    response = status("new-device")
    assert response.status_code == 200
    body = response.json()
    assert body["tier"] == "free"
    assert body["expires_at"] is None
    assert body["checked_at"].endswith("Z")


def test_status_requires_device_id(client):
    response = client.get("/api/sub/status")
    assert response.status_code == 422
    assert response.json() == {"error": "invalid_device_id"}


def test_verify_ok_is_active(verify, status):
    response = verify("ok-100")
    assert response.status_code == 200
    body = response.json()
    assert body["state"] == "ACTIVE"
    assert body["tier"] == "pro"
    assert from_iso(body["expires_at"]) > utcnow() + timedelta(days=29)
    assert status("dev-1").json()["tier"] == "pro"


def test_verify_pause_is_paused_and_keeps_paid_period(verify):
    first = verify("pause-7").json()
    assert first["state"] == "PAUSED"
    assert first["tier"] == "pro"
    second = verify("pause-7").json()
    assert second["expires_at"] == first["expires_at"]


def test_paused_access_ends_with_paid_period(client, verify):
    verify("pause-8")
    past = to_iso(utcnow() - timedelta(minutes=1))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE purchases SET expires_at = ? WHERE purchase_id = 'pause-8'", (past,))
    assert verify("pause-8").json()["tier"] == "free"


def test_verify_stop_is_terminated(verify, status):
    verify("ok-200")
    response = verify("stop-200", device_id="dev-2")
    assert response.status_code == 200
    assert response.json()["state"] == "TERMINATED"
    assert response.json()["tier"] == "free"
    assert status("dev-2").json()["tier"] == "free"


def test_verify_unknown_purchase_404(verify):
    response = verify("garbage-1")
    assert response.status_code == 404
    assert response.json() == {"error": "purchase_not_found"}


def test_lifetime_has_no_expiry(verify):
    body = verify("ok-life", product_id=LIFETIME).json()
    assert body["tier"] == "lifetime"
    assert body["expires_at"] is None


def test_unknown_product_422(verify):
    response = verify("ok-1", product_id="something")
    assert response.status_code == 422
    assert response.json() == {"error": "unknown_product"}


def test_same_purchase_other_device_409(verify, status):
    assert verify("ok-300", device_id="phone-a").status_code == 200
    response = verify("ok-300", device_id="phone-b")
    assert response.status_code == 409
    assert response.json() == {"error": "purchase_bound_to_other_device"}
    assert status("phone-b").json()["tier"] == "free"


def test_same_purchase_same_device_reverifies(verify):
    assert verify("ok-400", device_id="phone-a").status_code == 200
    assert verify("ok-400", device_id="phone-a").status_code == 200


def test_stub_off_reports_store_unavailable(tmp_path):
    from fastapi.testclient import TestClient

    from app.config import Settings
    from app.main import create_app
    from app.signing import generate_seed

    settings = Settings(data_dir=tmp_path, admin_token="x", signing_key=generate_seed(), rustore_stub=False)
    with TestClient(create_app(settings)) as client:
        key = client.post("/api/devices/register", json={"device_id": "dev-1"}).json()["device_key"]
        response = client.post(
            "/api/sub/verify",
            headers={"X-Device-Key": key},
            json={"purchase_id": "ok-1", "product_id": PRO, "device_id": "dev-1"},
        )
    assert response.status_code == 503
    assert response.json() == {"error": "store_unavailable"}


def anonymous_verify(client, device, purchase_id, device_id, headers=None):
    return client.post(
        "/api/sub/verify",
        headers={**device(device_id), **(headers or {})},
        json={"purchase_id": purchase_id, "product_id": PRO, "device_id": device_id},
    )


@pytest.mark.parametrize("headers", [None, {"Authorization": "Bearer garbage"}, {"Authorization": "Basic x"}])
def test_new_purchase_without_account_is_refused(client, device, status, headers):
    response = anonymous_verify(client, device, "ok-anon", "dev-anon", headers)
    assert response.status_code == 401
    assert response.json() == {"error": "unauthorized"}
    assert status("dev-anon").json()["tier"] == "free"
    with client.app.state.context.db.connect() as conn:
        assert conn.execute("SELECT COUNT(*) FROM purchases").fetchone()[0] == 0
        assert conn.execute("SELECT COUNT(*) FROM entitlements").fetchone()[0] == 0


def test_new_purchase_with_account_grants_pro_to_device_and_account(client, device, account, status):
    headers = account("dev-acc")
    response = anonymous_verify(client, device, "ok-acc", "dev-acc", headers)
    assert response.status_code == 200, response.text
    assert response.json()["tier"] == "pro"
    assert status("dev-acc").json()["tier"] == "pro"
    assert client.get("/api/account", headers=headers).json()["tier"] == "pro"


def test_new_purchase_with_token_of_other_account_is_409(client, device, account):
    account("dev-mine")
    stranger = account("dev-other")
    response = anonymous_verify(client, device, "ok-cross", "dev-mine", stranger)
    assert response.status_code == 409
    assert response.json() == {"error": "purchase_bound_to_other_account"}


def test_new_purchase_with_foreign_app_user_id_is_409(client, device, account):
    headers = account("dev-own")
    client.app.state.context.store.owners["ok-foreign"] = "someone-else"
    response = anonymous_verify(client, device, "ok-foreign", "dev-own", headers)
    assert response.status_code == 409
    assert response.json() == {"error": "purchase_bound_to_other_account"}


def test_legacy_device_purchase_keeps_working_without_account(client, device, subscribe, status):
    subscribe("dev-legacy", "ok-legacy")
    assert status("dev-legacy").json()["tier"] == "pro"
    response = anonymous_verify(client, device, "ok-legacy", "dev-legacy")
    assert response.status_code == 200, response.text
    assert response.json()["tier"] == "pro"
