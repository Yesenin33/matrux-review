import os
from dataclasses import dataclass
from pathlib import Path


def _flag(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in {"1", "true", "yes", "on"}


def _int(name: str, default: int) -> int:
    raw = os.environ.get(name, "").strip()
    return int(raw) if raw else default


@dataclass(frozen=True)
class Settings:
    data_dir: Path
    admin_token: str
    signing_key: str
    rustore_stub: bool
    public_url: str = ""
    product_pro: str = "palma_pro_month"
    product_lifetime: str = "palma_lifetime"
    pro_period_days: int = 30
    home_max_bytes: int = 2 * 1024**3
    image_max_bytes: int = 8 * 1024**3

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            data_dir=Path(os.environ.get("PALMA_DATA_DIR", "./data")).resolve(),
            admin_token=os.environ.get("PALMA_ADMIN_TOKEN", ""),
            signing_key=os.environ.get("PALMA_SIGNING_KEY", ""),
            rustore_stub=_flag("RUSTORE_STUB"),
            public_url=os.environ.get("PALMA_PUBLIC_URL", "").rstrip("/"),
            product_pro=os.environ.get("PALMA_PRODUCT_PRO", "palma_pro_month"),
            product_lifetime=os.environ.get("PALMA_PRODUCT_LIFETIME", "palma_lifetime"),
            pro_period_days=_int("PALMA_PRO_PERIOD_DAYS", 30),
            home_max_bytes=_int("PALMA_HOME_MAX_BYTES", 2 * 1024**3),
            image_max_bytes=_int("PALMA_IMAGE_MAX_BYTES", 8 * 1024**3),
        )
