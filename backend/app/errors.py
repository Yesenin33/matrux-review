import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

JSON_UTF8 = "application/json; charset=utf-8"

HTTP_CODES = {
    400: "bad_request",
    404: "not_found",
    405: "method_not_allowed",
    413: "payload_too_large",
    415: "unsupported_media_type",
}

log = logging.getLogger("palma.error")


class JsonUtf8Response(JSONResponse):
    media_type = JSON_UTF8


class ApiError(Exception):
    def __init__(self, status: int, code: str, headers: dict[str, str] | None = None) -> None:
        super().__init__(code)
        self.status = status
        self.code = code
        self.headers = headers or {}


def error_response(status: int, code: str, headers: dict[str, str] | None = None) -> JsonUtf8Response:
    return JsonUtf8Response({"error": code}, status_code=status, headers=headers)


async def _api_error(_: Request, exc: ApiError) -> JsonUtf8Response:
    return error_response(exc.status, exc.code, exc.headers)


async def _http_error(_: Request, exc: StarletteHTTPException) -> JsonUtf8Response:
    code = HTTP_CODES.get(exc.status_code, f"http_{exc.status_code}")
    return error_response(exc.status_code, code, getattr(exc, "headers", None))


async def _validation_error(_: Request, __: RequestValidationError) -> JsonUtf8Response:
    return error_response(422, "invalid_request")


async def _unhandled(_: Request, exc: Exception) -> JsonUtf8Response:
    log.error("unhandled %s", type(exc).__name__)
    return error_response(500, "internal_error")


def install_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(ApiError, _api_error)
    app.add_exception_handler(StarletteHTTPException, _http_error)
    app.add_exception_handler(RequestValidationError, _validation_error)
    app.add_exception_handler(Exception, _unhandled)
