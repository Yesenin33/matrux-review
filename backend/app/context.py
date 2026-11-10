import re
from dataclasses import dataclass
from pathlib import Path

from fastapi import Request

from app.config import Settings
from app.db import Database
from app.errors import ApiError
from app.rustore import PurchaseStore
from app.signing import Signer
from app.vkid import VkIdClient

DEVICE_ID = re.compile(r"^[A-Za-z0-9._-]{1,128}$")


@dataclass(frozen=True)
class Context:
    settings: Settings
    db: Database
    signer: Signer
    store: PurchaseStore
    vkid: VkIdClient

    @property
    def images_dir(self) -> Path:
        return self.settings.data_dir / "images"

    @property
    def home_dir(self) -> Path:
        return self.settings.data_dir / "home"


def get_context(request: Request) -> Context:
    return request.app.state.context


def check_device_id(device_id: str | None) -> str:
    if device_id is None or not DEVICE_ID.fullmatch(device_id):
        raise ApiError(422, "invalid_device_id")
    return device_id
