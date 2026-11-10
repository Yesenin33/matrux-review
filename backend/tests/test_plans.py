from app.errors import JSON_UTF8


def test_plans_carry_prices_and_derived_discounts(client):
    response = client.get("/api/plans")
    assert response.status_code == 200
    assert response.headers["content-type"] == JSON_UTF8
    assert response.json() == {
        "currency": "RUB",
        "plans": [
            {"product_id": "palma_pro_month", "months": 1, "price_rub": 349},
            {"product_id": "palma_pro_quarter", "months": 3, "price_rub": 890, "discount_percent": 15},
            {"product_id": "palma_pro_half_year", "months": 6, "price_rub": 1466, "discount_percent": 30},
        ],
    }


def test_plans_drop_discount_when_price_is_not_lower(client, settings):
    from dataclasses import replace

    from app.routes.plans import plans_public

    flat = replace(settings, price_quarter=349 * 3).plan_prices
    quarter = plans_public(flat)[1]
    assert "discount_percent" not in quarter
