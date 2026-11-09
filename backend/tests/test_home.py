import hashlib
import os
from datetime import timedelta

from app.clock import to_iso, utcnow


def upload(client, device_id, data, headers):
    return client.post(
        f"/api/home/{device_id}", headers=headers, files={"archive": ("home.tar", data, "application/x-tar")}
    )


def test_home_backup_roundtrip(client, subscribe):
    headers = subscribe("phone-1")
    first = os.urandom(300 * 1024)
    second = os.urandom(200 * 1024)
    response = upload(client, "phone-1", first, headers)
    assert response.status_code == 201
    body = response.json()
    assert body["size_bytes"] == len(first)
    assert body["sha256"] == hashlib.sha256(first).hexdigest()
    assert body["created_at"].endswith("Z")
    assert client.get("/api/home/phone-1", headers=headers).content == first
    upload(client, "phone-1", second, headers)
    latest = client.get("/api/home/phone-1", headers=headers)
    assert latest.status_code == 200
    assert latest.headers["etag"] == f'"{hashlib.sha256(second).hexdigest()}"'
    assert latest.headers["accept-ranges"] == "bytes"
    assert latest.content == second


def test_home_backup_range(client, subscribe):
    headers = subscribe("phone-2")
    data = os.urandom(10_000)
    upload(client, "phone-2", data, headers)
    part = client.get("/api/home/phone-2", headers={**headers, "Range": "bytes=5000-"})
    assert part.status_code == 206
    assert part.headers["content-range"] == "bytes 5000-9999/10000"
    assert part.content == data[5000:]


def test_home_backup_requires_subscription(client, device):
    response = upload(client, "free-phone", b"data", device("free-phone"))
    assert response.status_code == 402
    assert response.json() == {"error": "subscription_required"}


def test_home_backup_download_survives_expired_subscription(client, subscribe):
    headers = subscribe("phone-4", "ok-expire")
    data = os.urandom(4096)
    assert upload(client, "phone-4", data, headers).status_code == 201
    past = to_iso(utcnow() - timedelta(minutes=1))
    with client.app.state.context.db.connect() as conn:
        conn.execute("UPDATE purchases SET expires_at = ? WHERE purchase_id = 'ok-expire'", (past,))
    response = client.get("/api/home/phone-4", headers=headers)
    assert response.status_code == 200
    assert response.content == data
    blocked = upload(client, "phone-4", b"new", headers)
    assert blocked.status_code == 402
    assert blocked.json() == {"error": "subscription_required"}


def test_home_backup_missing_404(client, device):
    response = client.get("/api/home/nobody", headers=device("nobody"))
    assert response.status_code == 404
    assert response.json() == {"error": "backup_not_found"}


def test_home_backup_rejects_bad_device_id(client):
    response = client.get("/api/home/bad%20id")
    assert response.status_code == 422
    assert response.json() == {"error": "invalid_device_id"}


def test_home_upload_without_archive_422(client, subscribe):
    headers = subscribe("phone-3")
    response = client.post("/api/home/phone-3", headers=headers, files={"other": ("x", b"1")})
    assert response.status_code == 422
    assert response.json() == {"error": "missing_archive"}
