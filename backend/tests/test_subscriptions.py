from datetime import timedelta

import pytest

from app.clock import from_iso, to_iso, utcnow

PRO = "palma_pro_month"
LIFETIME = "palma_lifetime"


@pytest.fixture
def verify(client, device):
    def _verify(purchase_id, device_id="dev-1", product_id=PRO):
        return client.post(
            "/api/sub/verify",
            headers=device(device_id),
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
