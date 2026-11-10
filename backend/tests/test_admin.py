import os
from datetime import timedelta

import pytest
from fastapi.testclient import TestClient

from app.clock import to_iso, utcnow
from app.config import Settings
from app.main import create_app
from app.signing import generate_seed

ROUTES = [
    ("GET", "/api/admin/images"),
    ("POST", "/api/admin/images"),
    ("DELETE", "/api/admin/images/alpine"),
    ("GET", "/api/admin/subscribers"),
    ("GET", "/api/admin/stats"),
]

GARBAGE = [
    {"X-Admin-Token": "wrong"},
    {"X-Admin-Token": ""},
    {"X-Admin-Token": "x" * 10000},
    {"X-Admin-Token": "токен".encode()},
    {"X-Admin-Token": b"\xff\xfe\x00\x80garbage"},
]


@pytest.mark.parametrize(("method", "path"), ROUTES)
def test_no_token_403(client, method, path):
    response = client.request(method, path)
    assert response.status_code == 403
    assert response.json() == {"error": "forbidden"}


@pytest.mark.parametrize(("method", "path"), ROUTES)
@pytest.mark.parametrize("headers", GARBAGE)
def test_garbage_token_403(client, method, path, headers):
    response = client.request(method, path, headers=headers)
    assert response.status_code == 403
    assert response.json() == {"error": "forbidden"}


def test_garbage_token_on_multipart_403(client):
    response = client.post(
        "/api/admin/images",
        headers={"X-Admin-Token": "nope", "content-type": "multipart/form-data; boundary=broken"},
        content=b"--broken\r\nnot a multipart body",
    )
    assert response.status_code == 403


def test_unset_admin_token_rejects_everyone(tmp_path):
    settings = Settings(data_dir=tmp_path, admin_token="", signing_key=generate_seed(), rustore_stub=True)
    with TestClient(create_app(settings)) as client:
        assert client.get("/api/admin/images", headers={"X-Admin-Token": ""}).status_code == 403


def test_publish_list_and_unpublish(client, publish, admin_headers):
    image = publish("alpine", size=1000)
    listed = client.get("/api/admin/images", headers=admin_headers).json()["images"]
    assert [(i["id"], i["active"], i["file_present"], i["sha256"]) for i in listed] == [
        ("alpine", True, True, image.sha256)
    ]
    response = client.delete("/api/admin/images/alpine", headers=admin_headers)
    assert response.json() == {"id": "alpine", "active": False}
    listed = client.get("/api/admin/images", headers=admin_headers).json()["images"]
    assert listed[0]["active"] is False
    assert listed[0]["file_present"] is True
    assert client.delete("/api/admin/images/ghost", headers=admin_headers).status_code == 404


def test_duplicate_sha256_409(client, publish, admin_headers):
    data = os.urandom(2048)
    publish("first", data=data)
    response = client.post(
        "/api/admin/images",
        headers=admin_headers,
        data={"id": "second", "name": "n", "distro": "d", "desktop": "x", "min_ram_mb": "1", "pro": "false"},
        files={"file": ("x", data)},
    )
    assert response.status_code == 409
    assert response.json() == {"error": "duplicate_sha256"}
    leftovers = [p.name for p in (client.app.state.context.images_dir).iterdir()]
    assert all(not name.startswith(".upload-") for name in leftovers)


def test_duplicate_id_409(client, publish, admin_headers):
    publish("alpine")
    response = client.post(
        "/api/admin/images",
        headers=admin_headers,
        data={"id": "alpine", "name": "n", "distro": "d", "desktop": "x", "min_ram_mb": "1", "pro": "false"},
        files={"file": ("x", os.urandom(64))},
    )
    assert response.status_code == 409
    assert response.json() == {"error": "duplicate_id"}


@pytest.mark.parametrize(
    ("field", "value", "code"),
    [("id", "Bad Id", "invalid_id"), ("min_ram_mb", "lots", "invalid_min_ram_mb"), ("name", "", "invalid_name")],
)
def test_publish_validation(client, admin_headers, field, value, code):
    data = {"id": "ok", "name": "n", "distro": "d", "desktop": "x", "min_ram_mb": "1", "pro": "false"}
    data[field] = value
    response = client.post("/api/admin/images", headers=admin_headers, data=data, files={"file": ("x", b"1")})
    assert response.status_code == 422
    assert response.json() == {"error": code}


def test_publish_without_file_422(client, admin_headers):
    data = {"id": "ok", "name": "n", "distro": "d", "desktop": "x", "min_ram_mb": "1", "pro": "false"}
    response = client.post("/api/admin/images", headers=admin_headers, data=data, files={"other": ("x", b"1")})
    assert response.json() == {"error": "missing_file"}


def test_subscribers_summary(client, admin_headers, device, account):
    for purchase, device_id, product in [
        ("ok-a", "d1", "palma_pro_month"),
        ("ok-b", "d2", "palma_pro_month"),
        ("ok-c", "d3", "palma_lifetime"),
        ("stop-d", "d4", "palma_pro_month"),
    ]:
        response = client.post(
            "/api/sub/verify",
            headers={**device(device_id), **account(device_id)},
            json={"purchase_id": purchase, "product_id": product, "device_id": device_id},
        )
        assert response.status_code == 200
    soon = to_iso(utcnow() + timedelta(days=3))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE purchases SET expires_at = ? WHERE purchase_id = 'ok-b'", (soon,))
        conn.execute("UPDATE entitlements SET expires_at = ? WHERE purchase_id = 'ok-b'", (soon,))
    response = client.get("/api/admin/subscribers", headers=admin_headers)
    assert response.status_code == 200
    assert response.json() == {"total": 4, "pro": 2, "lifetime": 1, "expiring_7d": 1}
