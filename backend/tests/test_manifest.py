import base64
import json

import pytest
from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey

from app.errors import JSON_UTF8


def public_key(client) -> Ed25519PublicKey:
    response = client.get("/api/pubkey")
    assert response.status_code == 200
    assert response.json()["algorithm"] == "ed25519"
    return Ed25519PublicKey.from_public_bytes(base64.b64decode(response.json()["public_key"]))


def test_manifest_shape_and_headers(client, publish):
    image = publish("debian-xfce", pro=True)
    response = client.get("/api/manifest")
    assert response.status_code == 200
    assert response.headers["content-type"] == JSON_UTF8
    assert response.headers["cache-control"] == "public, max-age=60"
    assert response.headers["etag"]
    body = response.json()
    assert isinstance(body["version"], int)
    assert body["generated_at"].endswith("Z")
    assert body["images"] == [
        {
            "id": "debian-xfce",
            "name": "Образ debian-xfce",
            "distro": "debian",
            "desktop": "xfce",
            "size_bytes": len(image.data),
            "sha256": image.sha256,
            "min_ram_mb": 3072,
            "pro": True,
            "url": "/api/images/debian-xfce",
        }
    ]


def test_manifest_304_on_matching_etag(client, publish):
    publish("alpine")
    first = client.get("/api/manifest")
    etag = first.headers["etag"]
    second = client.get("/api/manifest", headers={"If-None-Match": etag})
    assert second.status_code == 304
    assert second.content == b""
    assert client.get("/api/manifest", headers={"If-None-Match": f'W/{etag}, "x"'}).status_code == 304
    assert client.get("/api/manifest", headers={"If-None-Match": '"stale"'}).status_code == 200


def test_manifest_etag_changes_after_publish(client, publish):
    before = client.get("/api/manifest")
    publish("alpine")
    after = client.get("/api/manifest", headers={"If-None-Match": before.headers["etag"]})
    assert after.status_code == 200
    assert after.json()["version"] > before.json()["version"]


def test_manifest_signature_verifies_with_pubkey(client, publish):
    publish("alpine")
    key = public_key(client)
    response = client.get("/api/manifest")
    signature = base64.b64decode(response.headers["x-palma-signature"])
    key.verify(signature, response.content)


def test_tampered_manifest_fails_signature(client, publish):
    publish("alpine")
    key = public_key(client)
    response = client.get("/api/manifest")
    signature = base64.b64decode(response.headers["x-palma-signature"])
    document = json.loads(response.content)
    document["images"][0]["pro"] = True
    tampered = json.dumps(document, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    with pytest.raises(InvalidSignature):
        key.verify(signature, tampered)
