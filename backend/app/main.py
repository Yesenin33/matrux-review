from datetime import timedelta
from pathlib import Path

from fastapi import FastAPI
from starlette.responses import FileResponse

from app.access_log import AccessLogMiddleware
from app.config import Settings
from app.context import Context
from app.db import Database
from app.errors import ApiError, JsonUtf8Response, install_error_handlers
from app.routes import account, admin, catalog, devices, home, plans, subs
from app.rustore import PurchaseStore, RuStoreApi, StubStore, UnconfiguredStore
from app.signing import Signer
from app.vkid import StubVkId, UnconfiguredVkId, VkIdApi, VkIdClient

ADMIN_DIR = Path(__file__).resolve().parent.parent / "admin"
FONTS = {
    "inter-latin.woff2": "inter-latin-wght-normal.woff2",
    "inter-cyrillic.woff2": "inter-cyrillic-wght-normal.woff2",
}


def build_store(settings: Settings) -> PurchaseStore:
    if settings.rustore_stub:
        return StubStore(timedelta(days=settings.pro_period_days), settings.pro_months)
    if settings.rustore_key_id and settings.rustore_key_file:
        return RuStoreApi(
            key_id=settings.rustore_key_id,
            key_file=settings.rustore_key_file,
            package_name=settings.package_name,
            sandbox=settings.rustore_sandbox,
            base=settings.rustore_api_base,
        )
    return UnconfiguredStore()


def build_vkid(settings: Settings) -> VkIdClient:
    if settings.vkid_stub:
        return StubVkId()
    if settings.vkid_client_id:
        return VkIdApi(client_id=settings.vkid_client_id, redirect_uri=settings.vkid_redirect)
    return UnconfiguredVkId()


def build_context(settings: Settings) -> Context:
    settings.data_dir.mkdir(parents=True, exist_ok=True)
    return Context(
        settings=settings,
        db=Database(settings.data_dir / "palma.sqlite3"),
        signer=Signer(settings.signing_key),
        store=build_store(settings),
        vkid=build_vkid(settings),
    )


def create_app(settings: Settings | None = None) -> FastAPI:
    app = FastAPI(
        title="Matrux",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        default_response_class=JsonUtf8Response,
    )
    app.state.context = build_context(settings or Settings.from_env())
    install_error_handlers(app)
    app.add_middleware(AccessLogMiddleware)
    app.include_router(catalog.router)
    app.include_router(plans.router)
    app.include_router(devices.router)
    app.include_router(subs.router)
    app.include_router(account.router)
    app.include_router(account.admin_router)
    app.include_router(home.router)
    app.include_router(admin.router)

    @app.get("/admin", include_in_schema=False)
    def admin_page() -> FileResponse:
        return FileResponse(
            ADMIN_DIR / "index.html",
            media_type="text/html; charset=utf-8",
            headers={"Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"},
        )

    @app.get("/admin/fonts/{name}", include_in_schema=False)
    def admin_font(name: str) -> FileResponse:
        filename = FONTS.get(name)
        if filename is None:
            raise ApiError(404, "not_found")
        return FileResponse(
            ADMIN_DIR / "fonts" / filename,
            media_type="font/woff2",
            headers={"Cache-Control": "public, max-age=31536000, immutable"},
        )

    return app
