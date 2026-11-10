from typing import Annotated

from fastapi import APIRouter, Depends

from app.context import Context, get_context
from app.errors import JsonUtf8Response

router = APIRouter(prefix="/api")

Ctx = Annotated[Context, Depends(get_context)]


def discount(months: int, price: int, month_price: int) -> int | None:
    full = month_price * months
    if months <= 1 or full <= 0 or price >= full:
        return None
    return round((full - price) * 100 / full)


def plans_public(prices: list[tuple[str, int, int]]) -> list[dict]:
    month_price = next((price for _, months, price in prices if months == 1), 0)
    plans = []
    for product_id, months, price in prices:
        plan = {"product_id": product_id, "months": months, "price_rub": price}
        off = discount(months, price, month_price)
        if off:
            plan["discount_percent"] = off
        plans.append(plan)
    return plans


@router.get("/plans")
def plans(ctx: Ctx) -> JsonUtf8Response:
    return JsonUtf8Response(
        {"currency": "RUB", "plans": plans_public(ctx.settings.plan_prices)},
        headers={"Cache-Control": "public, max-age=300"},
    )
