import os
from dataclasses import dataclass
from pathlib import Path


def _flag(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in {"1", "true", "yes", "on"}


def _int(name: str, default: int) -> int:
    raw = os.environ.get(name, "").strip()
    return int(raw) if raw else default


def _text(name: str, default: str = "") -> str:
    return os.environ.get(name, "").strip() or default


@dataclass(frozen=True)
class Settings:
    data_dir: Path
    admin_token: str
    signing_key: str
    rustore_stub: bool
    public_url: str = ""
    product_pro: str = "palma_pro_month"
    product_lifetime: str = "palma_lifetime"
    product_quarter: str = "palma_pro_quarter"
    product_half_year: str = "palma_pro_half_year"
    pro_period_days: int = 30
    home_max_bytes: int = 2 * 1024**3
    image_max_bytes: int = 8 * 1024**3
    package_name: str = "ru.palma.app"
    rustore_key_id: str = ""
    rustore_key_file: str = ""
    rustore_sandbox: bool = False
    rustore_api_base: str = "https://public-api.rustore.ru"
    vkid_stub: bool = False
    vkid_client_id: str = ""
    vkid_redirect_uri: str = ""
    session_days: int = 180
    price_month: int = 349
    price_quarter: int = 890
    price_half_year: int = 1466

    @property
    def pro_months(self) -> dict[str, int]:
        return {self.product_pro: 1, self.product_quarter: 3, self.product_half_year: 6}

    @property
    def plan_prices(self) -> list[tuple[str, int, int]]:
        return [
            (self.product_pro, 1, self.price_month),
            (self.product_quarter, 3, self.price_quarter),
            (self.product_half_year, 6, self.price_half_year),
        ]

    @property
    def vkid_redirect(self) -> str:
        if self.vkid_redirect_uri:
            return self.vkid_redirect_uri
        return f"vk{self.vkid_client_id}://vk.ru/blank.html" if self.vkid_client_id else ""

    @classmethod
    def from_env(cls) -> "Settings":
        month = _text("PALMA_PRODUCT_1M", _text("PALMA_PRODUCT_PRO", "palma_pro_month"))
        return cls(
            data_dir=Path(os.environ.get("PALMA_DATA_DIR", "./data")).resolve(),
            admin_token=os.environ.get("PALMA_ADMIN_TOKEN", ""),
            signing_key=os.environ.get("PALMA_SIGNING_KEY", ""),
            rustore_stub=_flag("RUSTORE_STUB"),
            public_url=os.environ.get("PALMA_PUBLIC_URL", "").rstrip("/"),
            product_pro=month,
            product_lifetime=_text("PALMA_PRODUCT_LIFETIME", "palma_lifetime"),
            product_quarter=_text("PALMA_PRODUCT_3M", "palma_pro_quarter"),
            product_half_year=_text("PALMA_PRODUCT_6M", "palma_pro_half_year"),
            pro_period_days=_int("PALMA_PRO_PERIOD_DAYS", 30),
            home_max_bytes=_int("PALMA_HOME_MAX_BYTES", 2 * 1024**3),
            image_max_bytes=_int("PALMA_IMAGE_MAX_BYTES", 8 * 1024**3),
            package_name=_text("PALMA_PACKAGE_NAME", "ru.palma.app"),
            rustore_key_id=_text("RUSTORE_KEY_ID"),
            rustore_key_file=_text("RUSTORE_KEY_FILE"),
            rustore_sandbox=_flag("RUSTORE_SANDBOX"),
            rustore_api_base=_text("RUSTORE_API_BASE", "https://public-api.rustore.ru").rstrip("/"),
            vkid_stub=_flag("VKID_STUB"),
            vkid_client_id=_text("VKID_CLIENT_ID"),
            vkid_redirect_uri=_text("VKID_REDIRECT_URI"),
            session_days=_int("PALMA_SESSION_DAYS", 180),
            price_month=_int("PALMA_PRICE_1M", 349),
            price_quarter=_int("PALMA_PRICE_3M", 890),
            price_half_year=_int("PALMA_PRICE_6M", 1466),
        )
