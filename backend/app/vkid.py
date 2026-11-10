import json
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Callable
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class AuthCode:
    code: str
    code_verifier: str
    device_id: str
    state: str


@dataclass(frozen=True)
class VkUser:
    user_id: str
    phone: str | None
    email: str | None


class AuthRejected(Exception):
    pass


class AuthUnavailable(Exception):
    pass


class VkIdClient(Protocol):
    def exchange(self, auth: AuthCode) -> VkUser: ...


class StubVkId:
    def exchange(self, auth: AuthCode) -> VkUser:
        if not auth.code.startswith("vk-"):
            raise AuthRejected
        user_id = auth.code.removeprefix("vk-")
        return VkUser(user_id=user_id, phone=f"+7900{user_id[-7:]:0>7}", email=None)


class UnconfiguredVkId:
    def exchange(self, auth: AuthCode) -> VkUser:
        raise AuthUnavailable


Transport = Callable[[urllib.request.Request], tuple[int, bytes]]


def urllib_transport(request: urllib.request.Request) -> tuple[int, bytes]:
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read()
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise AuthUnavailable from exc


@dataclass
class VkIdApi:
    client_id: str
    redirect_uri: str
    base: str = "https://id.vk.com"
    transport: Transport = urllib_transport

    def _post(self, path: str, fields: dict[str, str]) -> dict:
        request = urllib.request.Request(
            self.base + path,
            data=urllib.parse.urlencode(fields).encode(),
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        status, raw = self.transport(request)
        try:
            body = json.loads(raw or b"{}")
        except ValueError as exc:
            raise AuthUnavailable from exc
        if not isinstance(body, dict):
            raise AuthUnavailable
        if "error" in body:
            raise AuthRejected
        if status >= 500:
            raise AuthUnavailable
        if status != 200:
            raise AuthRejected
        return body

    def exchange(self, auth: AuthCode) -> VkUser:
        tokens = self._post(
            "/oauth2/auth",
            {
                "grant_type": "authorization_code",
                "code": auth.code,
                "code_verifier": auth.code_verifier,
                "client_id": self.client_id,
                "device_id": auth.device_id,
                "redirect_uri": self.redirect_uri,
                "state": auth.state,
            },
        )
        access = tokens.get("access_token")
        if not access:
            raise AuthRejected
        info = self._post("/oauth2/user_info", {"client_id": self.client_id, "access_token": access})
        user = info.get("user") or {}
        user_id = str(user.get("user_id") or tokens.get("user_id") or "")
        if not user_id:
            raise AuthRejected
        return VkUser(user_id=user_id, phone=user.get("phone") or None, email=user.get("email") or None)
