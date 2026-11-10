import base64
import json
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from pathlib import Path
from typing import Protocol

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa


class PurchaseState(StrEnum):
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    TERMINATED = "TERMINATED"


PAID_INVOICE = {"CONFIRMED", "PAID", "EXECUTED"}


@dataclass(frozen=True)
class StoreAnswer:
    state: PurchaseState
    paid_until: datetime
    app_user_id: str | None = None


@dataclass(frozen=True)
class Invoice:
    invoice_id: str
    purchase_id: str
    product_id: str
    status: str
    app_user_id: str | None = None

    @property
    def paid(self) -> bool:
        return self.status in PAID_INVOICE


class PurchaseNotFound(Exception):
    pass


class StoreUnavailable(Exception):
    pass


class PurchaseStore(Protocol):
    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer: ...

    def invoice(self, invoice_id: str) -> Invoice: ...


class StubStore:
    def __init__(self, period: timedelta, months: dict[str, int] | None = None) -> None:
        self.period = period
        self.months = months or {}
        self.invoices: dict[str, Invoice] = {}
        self.owners: dict[str, str] = {}

    def paid_until(self, product_id: str, now: datetime) -> datetime:
        months = self.months.get(product_id, 1)
        return now + self.period * months

    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer:
        owner = self.owners.get(purchase_id)
        if purchase_id.startswith("ok"):
            return StoreAnswer(PurchaseState.ACTIVE, self.paid_until(product_id, now), owner)
        if purchase_id.startswith("pause"):
            return StoreAnswer(PurchaseState.PAUSED, self.paid_until(product_id, now), owner)
        if purchase_id.startswith("stop"):
            return StoreAnswer(PurchaseState.TERMINATED, now, owner)
        raise PurchaseNotFound

    def invoice(self, invoice_id: str) -> Invoice:
        known = self.invoices.get(invoice_id)
        if known is None:
            raise PurchaseNotFound
        return known


class UnconfiguredStore:
    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer:
        raise StoreUnavailable

    def invoice(self, invoice_id: str) -> Invoice:
        raise StoreUnavailable


Transport = Callable[[urllib.request.Request], tuple[int, bytes]]


def urllib_transport(request: urllib.request.Request) -> tuple[int, bytes]:
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read()
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise StoreUnavailable from exc


def sign_auth(key: rsa.RSAPrivateKey, key_id: str, timestamp: str) -> str:
    signature = key.sign((key_id + timestamp).encode(), padding.PKCS1v15(), hashes.SHA512())
    return base64.b64encode(signature).decode()


def load_private_key(path: str) -> rsa.RSAPrivateKey:
    raw = Path(path).read_bytes().strip()
    if not raw.startswith(b"-----"):
        raw = b"-----BEGIN PRIVATE KEY-----\n" + raw + b"\n-----END PRIVATE KEY-----\n"
    key = serialization.load_pem_private_key(raw, password=None)
    if not isinstance(key, rsa.RSAPrivateKey):
        raise StoreUnavailable
    return key


def millis(value: object) -> datetime | None:
    try:
        return datetime.fromtimestamp(int(str(value)) / 1000, UTC).replace(microsecond=0)
    except (TypeError, ValueError):
        return None


def subscription_answer(body: dict, now: datetime) -> StoreAnswer:
    expires = millis(body.get("expiryTimeMillis")) or now
    owner = body.get("externalAccountId") or None
    if "paymentState" not in body or expires <= now:
        return StoreAnswer(PurchaseState.TERMINATED, expires, owner)
    return StoreAnswer(PurchaseState.ACTIVE, expires, owner)


def invoice_from(invoice_id: str, body: dict) -> Invoice:
    order = body.get("order") or {}
    return Invoice(
        invoice_id=invoice_id,
        purchase_id=str(body.get("purchaseId") or ""),
        product_id=str(order.get("itemCode") or ""),
        status=str(body.get("invoiceStatus") or "").upper(),
        app_user_id=body.get("appUserId") or None,
    )


@dataclass
class RuStoreApi:
    key_id: str
    key_file: str
    package_name: str
    sandbox: bool = False
    base: str = "https://public-api.rustore.ru"
    transport: Transport = urllib_transport
    clock: Callable[[], float] = time.time
    _token: str = field(default="", repr=False)
    _token_until: float = 0.0
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def _prefix(self) -> str:
        return "/public/sandbox" if self.sandbox else "/public"

    def token(self) -> str:
        with self._lock:
            if self._token and self.clock() < self._token_until:
                return self._token
            stamp = datetime.fromtimestamp(self.clock(), UTC).isoformat(timespec="milliseconds")
            key = load_private_key(self.key_file)
            payload = {"keyId": self.key_id, "timestamp": stamp, "signature": sign_auth(key, self.key_id, stamp)}
            status, raw = self.transport(
                urllib.request.Request(
                    self.base + "/public/auth/",
                    data=json.dumps(payload).encode(),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
            )
            body = self._body(status, raw)
            token = body.get("jwe")
            if not token:
                raise StoreUnavailable
            ttl = int(body.get("ttl") or 900)
            self._token = token
            self._token_until = self.clock() + max(ttl - 60, 30)
            return token

    def _get(self, path: str) -> dict:
        request = urllib.request.Request(self.base + path, headers={"Public-Token": self.token()}, method="GET")
        status, raw = self.transport(request)
        return self._body(status, raw)

    @staticmethod
    def _body(status: int, raw: bytes) -> dict:
        try:
            document = json.loads(raw or b"{}")
        except ValueError as exc:
            raise StoreUnavailable from exc
        message = str(document.get("message") or "").lower()
        if status == 404 or document.get("code") == "NOT_FOUND" or "not found" in message:
            raise PurchaseNotFound
        if status != 200 or document.get("code") != "OK":
            raise StoreUnavailable
        body = document.get("body")
        return body if isinstance(body, dict) else {}

    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer:
        parts = (self.package_name, product_id, purchase_id)
        path = "/".join(urllib.parse.quote(part, safe="") for part in parts)
        return subscription_answer(self._get(f"{self._prefix()}/v5/subscription/{path}"), now)

    def invoice(self, invoice_id: str) -> Invoice:
        if not invoice_id.isdigit():
            raise PurchaseNotFound
        return invoice_from(invoice_id, self._get(f"{self._prefix()}/v2/invoices/{invoice_id}"))
