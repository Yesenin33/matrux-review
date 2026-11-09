import hashlib

from app.errors import JSON_UTF8

MB = 1024 * 1024


def test_full_download_headers(client, publish):
    image = publish("alpine", size=MB + 17)
    response = client.get("/api/images/alpine")
    assert response.status_code == 200
    assert response.headers["accept-ranges"] == "bytes"
    assert response.headers["etag"] == f'"{image.sha256}"'
    assert response.headers["content-length"] == str(len(image.data))
    assert response.content == image.data


def test_range_returns_206(client, publish):
    image = publish("alpine", size=4096)
    response = client.get("/api/images/alpine", headers={"Range": "bytes=100-199"})
    assert response.status_code == 206
    assert response.headers["content-range"] == "bytes 100-199/4096"
    assert response.headers["content-length"] == "100"
    assert response.content == image.data[100:200]


def test_open_and_suffix_ranges(client, publish):
    image = publish("alpine", size=4096)
    tail = client.get("/api/images/alpine", headers={"Range": "bytes=4000-"})
    assert tail.status_code == 206
    assert tail.headers["content-range"] == "bytes 4000-4095/4096"
    assert tail.content == image.data[4000:]
    suffix = client.get("/api/images/alpine", headers={"Range": "bytes=-10"})
    assert suffix.headers["content-range"] == "bytes 4086-4095/4096"
    assert suffix.content == image.data[-10:]
    clipped = client.get("/api/images/alpine", headers={"Range": "bytes=4090-999999"})
    assert clipped.headers["content-range"] == "bytes 4090-4095/4096"


def test_unsatisfiable_range(client, publish):
    publish("alpine", size=4096)
    response = client.get("/api/images/alpine", headers={"Range": "bytes=5000-"})
    assert response.status_code == 416
    assert response.headers["content-range"] == "bytes */4096"
    assert response.json() == {"error": "range_not_satisfiable"}


def test_malformed_range_is_ignored(client, publish):
    image = publish("alpine", size=4096)
    response = client.get("/api/images/alpine", headers={"Range": "bytes=abc"})
    assert response.status_code == 200
    assert response.content == image.data


def test_resume_after_interrupted_download(client, publish):
    image = publish("debian", size=3 * MB + 123)
    received = bytearray()
    with client.stream("GET", "/api/images/debian") as response:
        assert response.status_code == 200
        for chunk in response.iter_bytes():
            received.extend(chunk)
            if len(received) >= 2 * MB:
                break
    offset = len(received)
    assert 0 < offset < len(image.data)
    etag = f'"{image.sha256}"'
    resumed = client.get("/api/images/debian", headers={"Range": f"bytes={offset}-", "If-Range": etag})
    assert resumed.status_code == 206
    assert resumed.headers["content-range"] == f"bytes {offset}-{len(image.data) - 1}/{len(image.data)}"
    received.extend(resumed.content)
    assert hashlib.sha256(received).hexdigest() == image.sha256


def test_resume_with_stale_if_range_restarts(client, publish):
    image = publish("alpine", size=4096)
    response = client.get("/api/images/alpine", headers={"Range": "bytes=100-", "If-Range": '"other"'})
    assert response.status_code == 200
    assert response.content == image.data


def test_unknown_image_404(client):
    response = client.get("/api/images/missing")
    assert response.status_code == 404
    assert response.json() == {"error": "image_not_found"}


def test_missing_file_503(client, publish):
    image = publish("alpine")
    image.path.unlink()
    response = client.get("/api/images/alpine")
    assert response.status_code == 503
    assert response.headers["content-type"] == JSON_UTF8
    assert response.json() == {"error": "file_unavailable"}


def test_unpublished_image_404_but_file_kept(client, publish, admin_headers):
    image = publish("alpine")
    assert client.delete("/api/admin/images/alpine", headers=admin_headers).status_code == 200
    assert client.get("/api/images/alpine").status_code == 404
    assert image.path.is_file()
    assert client.get("/api/manifest").json()["images"] == []
