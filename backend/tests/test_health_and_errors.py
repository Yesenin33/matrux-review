import logging

from app.errors import JSON_UTF8


def test_health(client):
    response = client.get("/api/health")
    assert response.status_code == 200
    assert response.json() == {"ok": True}
    assert response.headers["content-type"] == JSON_UTF8


def test_unknown_route_is_json(client):
    response = client.get("/api/nope")
    assert response.status_code == 404
    assert response.headers["content-type"] == JSON_UTF8
    assert response.json() == {"error": "not_found"}


def test_wrong_method_is_json(client):
    response = client.delete("/api/health")
    assert response.status_code == 405
    assert response.json() == {"error": "method_not_allowed"}


def test_validation_error_is_json(client):
    response = client.post("/api/sub/verify", content=b"{not json", headers={"content-type": "application/json"})
    assert response.status_code == 422
    assert response.json() == {"error": "invalid_request"}


def test_one_log_line_per_request_without_token_or_query(client, admin_headers, caplog):
    with caplog.at_level(logging.INFO, logger="palma.access"):
        client.get("/api/admin/subscribers?secret=zzz", headers=admin_headers)
    lines = [r.getMessage() for r in caplog.records if r.name == "palma.access"]
    assert len(lines) == 1
    assert lines[0].startswith("GET /api/admin/subscribers 200 ")
    assert admin_headers["X-Admin-Token"] not in caplog.text
    assert "zzz" not in caplog.text


def test_admin_page_is_served(client):
    response = client.get("/admin")
    assert response.status_code == 200
    assert response.headers["content-type"] == "text/html; charset=utf-8"
    assert "<html" in response.text
    assert client.get("/admin/fonts/inter-cyrillic.woff2").status_code == 200
    assert client.get("/admin/fonts/../main.py").status_code == 404
