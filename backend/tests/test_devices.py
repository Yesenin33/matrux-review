import base64
import hashlib
import logging
import os

import pytest

GARBAGE = [
    {},
    {"X-Device-Key": ""},
    {"X-Device-Key": "wrong"},
    {"X-Device-Key": "x" * 10000},
    {"X-Device-Key": "ключ".encode()},
    {"X-Device-Key": b"\xff\xfe\x00\x80garbage"},
]


def register(client, device_id, headers=None, name=None):
    return client.post("/api/devices/register", headers=headers or {}, json={"device_id": device_id, "name": name})


def device_routes(device_id):
    return [
        ("GET", f"/api/home/{device_id}", {}),
        ("POST", f"/api/home/{device_id}", {"files": {"archive": ("home.tar", b"data")}}),
        ("GET", "/api/sub/status", {"params": {"device_id": device_id}}),
        (
            "POST",
            "/api/sub/verify",
            {"json": {"purchase_id": "ok-9", "product_id": "palma_pro_month", "device_id": device_id}},
        ),
    ]


def test_register_returns_key_once(client, settings):
    response = register(client, "phone-1", name="Pixel")
    assert response.status_code == 201
    body = response.json()
    assert set(body) == {"device_id", "device_key", "tier", "expires_at"}
    assert body["device_id"] == "phone-1"
    assert body["tier"] == "free"
    assert body["expires_at"] is None
    key = body["device_key"]
    assert len(base64.urlsafe_b64decode(key + "=" * (-len(key) % 4))) == 32
    with client.app.state.context.db.connect() as conn:
        row = conn.execute("SELECT * FROM devices WHERE device_id = 'phone-1'").fetchone()
    assert row["key_hash"] == hashlib.sha256(key.encode()).hexdigest()
    assert row["name"] == "Pixel"
    assert key.encode() not in (settings.data_dir / "palma.sqlite3").read_bytes()


def test_register_keys_differ(client):
    assert register(client, "a").json()["device_key"] != register(client, "b").json()["device_key"]


@pytest.mark.parametrize("headers", GARBAGE)
def test_reregister_without_valid_key_409(client, headers):
    original = register(client, "phone-1").json()["device_key"]
    response = register(client, "phone-1", headers=headers)
    assert response.status_code == 409
    assert response.json() == {"error": "device_exists"}
    status = client.get("/api/sub/status", params={"device_id": "phone-1"}, headers={"X-Device-Key": original})
    assert status.status_code == 200


def test_reregister_with_own_key_keeps_key(client, subscribe):
    headers = subscribe("phone-1")
    response = register(client, "phone-1", headers=headers)
    assert response.status_code == 200
    assert response.json()["tier"] == "pro"
    assert "device_key" not in response.json()
    assert client.get("/api/sub/status", params={"device_id": "phone-1"}, headers=headers).status_code == 200


def test_register_rejects_bad_device_id(client):
    response = register(client, "bad id")
    assert response.status_code == 422
    assert response.json() == {"error": "invalid_device_id"}


def test_own_key_downloads_backup(client, subscribe):
    headers = subscribe("mine")
    data = os.urandom(2048)
    assert client.post("/api/home/mine", headers=headers, files={"archive": ("h.tar", data)}).status_code == 201
    response = client.get("/api/home/mine", headers=headers)
    assert response.status_code == 200
    assert response.content == data


@pytest.mark.parametrize("has_backup", [True, False])
def test_foreign_key_403_does_not_reveal_backup(client, subscribe, device, has_backup):
    owner = subscribe("victim")
    if has_backup:
        client.post("/api/home/victim", headers=owner, files={"archive": ("h.tar", b"secret")})
    response = client.get("/api/home/victim", headers=device("attacker"))
    assert response.status_code == 403
    assert response.json() == {"error": "forbidden"}


@pytest.mark.parametrize("route", range(4))
def test_foreign_key_403_on_every_device_route(client, subscribe, device, route):
    subscribe("victim")
    method, path, kwargs = device_routes("victim")[route]
    response = client.request(method, path, headers=device("attacker"), **kwargs)
    assert response.status_code == 403
    assert response.json() == {"error": "forbidden"}


@pytest.mark.parametrize("route", range(4))
@pytest.mark.parametrize("headers", GARBAGE)
def test_missing_or_garbage_key_403(client, subscribe, route, headers):
    subscribe("victim")
    method, path, kwargs = device_routes("victim")[route]
    response = client.request(method, path, headers=headers, **kwargs)
    assert response.status_code == 403
    assert response.json() == {"error": "forbidden"}


@pytest.mark.parametrize("route", range(4))
def test_unregistered_device_403(client, route):
    method, path, kwargs = device_routes("ghost")[route]
    response = client.request(method, path, headers={"X-Device-Key": "anything"}, **kwargs)
    assert response.status_code == 403


def test_key_not_logged(client, caplog):
    with caplog.at_level(logging.INFO, logger="palma"):
        key = register(client, "phone-1").json()["device_key"]
        client.get("/api/sub/status", params={"device_id": "phone-1"}, headers={"X-Device-Key": key})
    assert key not in caplog.text
