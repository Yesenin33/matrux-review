from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import StrEnum
from typing import Protocol


class PurchaseState(StrEnum):
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    TERMINATED = "TERMINATED"


@dataclass(frozen=True)
class StoreAnswer:
    state: PurchaseState
    paid_until: datetime


class PurchaseNotFound(Exception):
    pass


class StoreUnavailable(Exception):
    pass


class PurchaseStore(Protocol):
    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer: ...


class StubStore:
    def __init__(self, period: timedelta) -> None:
        self.period = period

    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer:
        if purchase_id.startswith("ok"):
            return StoreAnswer(PurchaseState.ACTIVE, now + self.period)
        if purchase_id.startswith("pause"):
            return StoreAnswer(PurchaseState.PAUSED, now + self.period)
        if purchase_id.startswith("stop"):
            return StoreAnswer(PurchaseState.TERMINATED, now)
        raise PurchaseNotFound


class UnconfiguredStore:
    def check(self, purchase_id: str, product_id: str, now: datetime) -> StoreAnswer:
        raise StoreUnavailable
