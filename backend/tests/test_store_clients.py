import base64
import json
import urllib.parse
from datetime import UTC, datetime, timedelta

import pytest
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

from app.config import Settings
from app.main import build_store, build_vkid
from app.rustore import (
    PurchaseNotFound,
    PurchaseState,
    RuStoreApi,
    StoreUnavailable,
    StubStore,
    UnconfiguredStore,
    subscription_answer,
)
from app.vkid import AuthCode, AuthRejected, AuthUnavailable, StubVkId, UnconfiguredVkId, VkIdApi

NOW = datetime(2026, 11, 10, 12, 0, tzinfo=UTC)


class Recorder:
    def __init__(self, *answers):
        self.answers = list(answers)
        self.requests = []

    def __call__(self, request):
        self.requests.append(request)
        status, body = self.answers.pop(0)
        return status, json.dumps(body).encode() if not isinstance(body, bytes) else body


@pytest.fixture
def key_file(tmp_path):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    path = tmp_path / "rustore.pem"
    path.write_bytes(
        key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())
    )
    return path, key


def ok(body):
    return 200, {"code": "OK", "message": "Successful result", "body": body, "timestamp": "2026-11-10T12:00:00Z"}


def millis(moment):
    return str(int(moment.timestamp() * 1000))


def api(key_file, transport, sandbox=False):
    path, _ = key_file
    return RuStoreApi("key-1", str(path), "ru.palma.app", sandbox=sandbox, transport=transport, clock=NOW.timestamp)


def test_auth_signs_key_id_and_timestamp(key_file):
    _, key = key_file
    transport = Recorder(ok({"jwe": "jwe-1", "ttl": 900}), ok({"expiryTimeMillis": millis(NOW), "paymentState": 1}))
    api(key_file, transport).check("p-1", "palma_pro_month", NOW)
    auth = transport.requests[0]
    assert auth.full_url == "https://public-api.rustore.ru/public/auth/"
    payload = json.loads(auth.data)
    assert payload["keyId"] == "key-1"
    signature = base64.b64decode(payload["signature"])
    key.public_key().verify(signature, ("key-1" + payload["timestamp"]).encode(), padding.PKCS1v15(), hashes.SHA512())


def test_token_is_reused_until_expiry(key_file):
    future = millis(NOW + timedelta(days=3))
    transport = Recorder(
        ok({"jwe": "jwe-1", "ttl": 900}),
        ok({"expiryTimeMillis": future, "paymentState": 1}),
        ok({"expiryTimeMillis": future, "paymentState": 1}),
    )
    store = api(key_file, transport)
    store.check("p-1", "palma_pro_month", NOW)
    store.check("p-1", "palma_pro_month", NOW)
    assert sum(1 for r in transport.requests if r.full_url.endswith("/auth/")) == 1
    assert transport.requests[1].get_header("Public-token") == "jwe-1"


def test_subscription_check_uses_v5_path_and_maps_expiry(key_file):
    future = NOW + timedelta(days=30)
    transport = Recorder(
        ok({"jwe": "t", "ttl": 900}),
        ok({"expiryTimeMillis": millis(future), "paymentState": 1, "externalAccountId": "42"}),
    )
    answer = api(key_file, transport).check("3aa0c7bd", "palma_pro_quarter", NOW)
    url = transport.requests[1].full_url
    assert url == "https://public-api.rustore.ru/public/v5/subscription/ru.palma.app/palma_pro_quarter/3aa0c7bd"
    assert answer.state == PurchaseState.ACTIVE
    assert answer.paid_until == future
    assert answer.app_user_id == "42"


def test_sandbox_path(key_file):
    transport = Recorder(ok({"jwe": "t"}), ok({"invoiceId": 7, "invoiceStatus": "CONFIRMED"}))
    api(key_file, transport, sandbox=True).invoice("7")
    assert transport.requests[1].full_url == "https://public-api.rustore.ru/public/sandbox/v2/invoices/7"


def test_path_segments_are_escaped(key_file):
    transport = Recorder(ok({"jwe": "t"}), ok({"expiryTimeMillis": millis(NOW), "paymentState": 1}))
    api(key_file, transport).check("../x", "a/b", NOW)
    path = urllib.parse.urlsplit(transport.requests[1].full_url).path
    assert path.endswith("/a%2Fb/..%2Fx")


def test_invoice_fields(key_file):
    transport = Recorder(
        ok({"jwe": "t"}),
        ok(
            {
                "invoiceId": 12,
                "invoiceStatus": "confirmed",
                "purchaseId": "uuid-1",
                "appUserId": "42",
                "order": {"itemCode": "palma_pro_month", "amountCreate": 34900},
            }
        ),
    )
    invoice = api(key_file, transport).invoice("12")
    assert (invoice.purchase_id, invoice.product_id, invoice.status, invoice.app_user_id) == (
        "uuid-1",
        "palma_pro_month",
        "CONFIRMED",
        "42",
    )
    assert invoice.paid


def test_non_numeric_invoice_is_not_found_without_network(key_file):
    transport = Recorder()
    with pytest.raises(PurchaseNotFound):
        api(key_file, transport).invoice("../../auth")
    assert transport.requests == []


@pytest.mark.parametrize(
    ("status", "body", "error"),
    [
        (200, {"code": "ERROR", "message": "Purchase not found", "body": None}, PurchaseNotFound),
        (404, {"code": "NOT_FOUND", "body": None}, PurchaseNotFound),
        (200, {"code": "ERROR", "message": "Forbidden", "body": None}, StoreUnavailable),
        (500, {"code": "ERROR", "message": "Something went wrong"}, StoreUnavailable),
        (200, b"<html>", StoreUnavailable),
    ],
)
def test_store_errors(key_file, status, body, error):
    transport = Recorder(ok({"jwe": "t"}), (status, body))
    with pytest.raises(error):
        api(key_file, transport).check("p", "palma_pro_month", NOW)


def test_auth_failure_is_unavailable(key_file):
    transport = Recorder((200, {"code": "ERROR", "message": "Range timestamp not valid"}))
    with pytest.raises(StoreUnavailable):
        api(key_file, transport).check("p", "palma_pro_month", NOW)


def test_bare_base64_key_file_is_accepted(tmp_path, key_file):
    _, key = key_file
    der = key.private_bytes(serialization.Encoding.DER, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())
    bare = tmp_path / "bare.key"
    bare.write_text(base64.b64encode(der).decode())
    transport = Recorder(ok({"jwe": "t"}), ok({"expiryTimeMillis": millis(NOW), "paymentState": 1}))
    RuStoreApi("k", str(bare), "ru.palma.app", transport=transport, clock=NOW.timestamp).check("p", "x", NOW)


@pytest.mark.parametrize(
    ("body", "state"),
    [
        ({"expiryTimeMillis": millis(NOW + timedelta(days=1)), "paymentState": 1}, PurchaseState.ACTIVE),
        ({"expiryTimeMillis": millis(NOW + timedelta(days=1)), "paymentState": 2}, PurchaseState.ACTIVE),
        ({"expiryTimeMillis": millis(NOW - timedelta(days=1)), "paymentState": 1}, PurchaseState.TERMINATED),
        ({"expiryTimeMillis": millis(NOW + timedelta(days=1))}, PurchaseState.TERMINATED),
        ({}, PurchaseState.TERMINATED),
    ],
)
def test_subscription_answer_states(body, state):
    assert subscription_answer(body, NOW).state == state


def test_stub_period_scales_with_plan():
    store = StubStore(timedelta(days=30), {"m": 1, "q": 3, "h": 6})
    assert store.check("ok-1", "q", NOW).paid_until == NOW + timedelta(days=90)
    assert store.check("ok-1", "h", NOW).paid_until == NOW + timedelta(days=180)


def base_settings(tmp_path, **extra):
    return Settings(data_dir=tmp_path, admin_token="", signing_key="", rustore_stub=False, **extra)


def test_store_selection(tmp_path):
    assert isinstance(build_store(base_settings(tmp_path)), UnconfiguredStore)
    assert isinstance(build_store(base_settings(tmp_path, rustore_key_id="k")), UnconfiguredStore)
    real = build_store(base_settings(tmp_path, rustore_key_id="k", rustore_key_file="/x", rustore_sandbox=True))
    assert isinstance(real, RuStoreApi)
    assert real.sandbox


def test_vkid_selection(tmp_path):
    assert isinstance(build_vkid(base_settings(tmp_path)), UnconfiguredVkId)
    assert isinstance(build_vkid(base_settings(tmp_path, vkid_stub=True)), StubVkId)
    real = build_vkid(base_settings(tmp_path, vkid_client_id="5123"))
    assert isinstance(real, VkIdApi)
    assert real.redirect_uri == "vk5123://vk.ru/blank.html"


def test_products_from_env(monkeypatch):
    monkeypatch.setenv("PALMA_PRODUCT_PRO", "legacy_month")
    monkeypatch.setenv("PALMA_PRODUCT_3M", "q3")
    settings = Settings.from_env()
    assert settings.pro_months == {"legacy_month": 1, "q3": 3, "palma_pro_half_year": 6}
    monkeypatch.setenv("PALMA_PRODUCT_1M", "m1")
    assert Settings.from_env().pro_months["m1"] == 1


AUTH = AuthCode("code-1", "v" * 43, "vk-device", "state-1")


def test_vkid_exchange_then_user_info():
    transport = Recorder(
        (200, {"access_token": "at", "user_id": 77, "state": "state-1"}),
        (200, {"user": {"user_id": "77", "phone": "79001234567", "email": "a@b.ru"}}),
    )
    user = VkIdApi("5123", "vk5123://vk.ru/blank.html", transport=transport).exchange(AUTH)
    assert (user.user_id, user.phone, user.email) == ("77", "79001234567", "a@b.ru")
    first = dict(urllib.parse.parse_qsl(transport.requests[0].data.decode()))
    assert transport.requests[0].full_url == "https://id.vk.com/oauth2/auth"
    assert first == {
        "grant_type": "authorization_code",
        "code": "code-1",
        "code_verifier": "v" * 43,
        "client_id": "5123",
        "device_id": "vk-device",
        "redirect_uri": "vk5123://vk.ru/blank.html",
        "state": "state-1",
    }
    second = dict(urllib.parse.parse_qsl(transport.requests[1].data.decode()))
    assert transport.requests[1].full_url == "https://id.vk.com/oauth2/user_info"
    assert second == {"client_id": "5123", "access_token": "at"}


@pytest.mark.parametrize(
    ("answers", "error"),
    [
        ([(400, {"error": "invalid_grant"})], AuthRejected),
        ([(200, {"error": "invalid_request"})], AuthRejected),
        ([(200, {"token_type": "Bearer"})], AuthRejected),
        ([(502, b"bad gateway")], AuthUnavailable),
        ([(503, {})], AuthUnavailable),
        ([(200, {"access_token": "at"}), (200, {"user": {}})], AuthRejected),
    ],
)
def test_vkid_errors(answers, error):
    with pytest.raises(error):
        VkIdApi("5123", "r", transport=Recorder(*answers)).exchange(AUTH)
