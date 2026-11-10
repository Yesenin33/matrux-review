import hashlib
import os
from collections.abc import Callable, Iterator
from dataclasses import dataclass
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.clock import utcnow
from app.config import Settings
from app.main import create_app
from app.signing import generate_seed
from app.subscriptions import PRO, record_purchase

ADMIN_TOKEN = "test-admin-token-4f1c"


@dataclass
class Published:
    id: str
    sha256: str
    data: bytes
    path: Path


@pytest.fixture
def settings(tmp_path: Path) -> Settings:
    return Settings(
        data_dir=tmp_path / "data",
        admin_token=ADMIN_TOKEN,
        signing_key=generate_seed(),
        rustore_stub=True,
        vkid_stub=True,
    )


@pytest.fixture
def client(settings: Settings) -> Iterator[TestClient]:
    with TestClient(create_app(settings)) as test_client:
        yield test_client


@pytest.fixture
def admin_headers() -> dict[str, str]:
    return {"X-Admin-Token": ADMIN_TOKEN}


@pytest.fixture
def publish(client: TestClient, settings: Settings, admin_headers: dict[str, str]) -> Callable[..., Published]:
    def _publish(image_id: str, size: int = 256 * 1024, pro: bool = False, data: bytes | None = None) -> Published:
        payload = data if data is not None else os.urandom(size)
        response = client.post(
            "/api/admin/images",
            headers=admin_headers,
            data={
                "id": image_id,
                "name": f"Образ {image_id}",
                "distro": "debian",
                "desktop": "xfce",
                "min_ram_mb": "3072",
                "pro": "true" if pro else "false",
            },
            files={"file": (f"{image_id}.tar.xz", payload, "application/octet-stream")},
        )
        assert response.status_code == 201, response.text
        sha256 = hashlib.sha256(payload).hexdigest()
        return Published(image_id, sha256, payload, settings.data_dir / "images" / sha256)

    return _publish


@pytest.fixture
def device(client: TestClient) -> Callable[[str], dict[str, str]]:
    keys: dict[str, dict[str, str]] = {}

    def _device(device_id: str) -> dict[str, str]:
        if device_id not in keys:
            response = client.post("/api/devices/register", json={"device_id": device_id, "name": None})
            assert response.status_code == 201, response.text
            keys[device_id] = {"X-Device-Key": response.json()["device_key"]}
        return keys[device_id]

    return _device


@pytest.fixture
def account(client: TestClient, device: Callable[[str], dict[str, str]]) -> Callable[..., dict[str, str]]:
    tokens: dict[str, dict[str, str]] = {}

    def _account(device_id: str, user: str | None = None) -> dict[str, str]:
        if device_id not in tokens:
            response = client.post(
                "/api/account/vkid",
                headers=device(device_id),
                json={
                    "code": f"vk-{user or device_id}",
                    "code_verifier": "v" * 43,
                    "vk_device_id": "vkdev",
                    "state": "s1",
                    "device_id": device_id,
                },
            )
            assert response.status_code == 200, response.text
            tokens[device_id] = {"Authorization": f"Bearer {response.json()['token']}"}
        return tokens[device_id]

    return _account


@pytest.fixture
def subscribe(client: TestClient, device: Callable[[str], dict[str, str]]) -> Callable[..., dict[str, str]]:
    context = client.app.state.context

    def _subscribe(device_id: str, purchase_id: str = "ok-1") -> dict[str, str]:
        headers = device(device_id)
        now = utcnow()
        answer = context.store.check(purchase_id, "palma_pro_month", now)
        with context.db.connect() as conn:
            record_purchase(conn, purchase_id, "palma_pro_month", device_id, PRO, answer, None, now)
        return headers

    return _subscribe
