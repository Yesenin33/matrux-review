import json

from fastapi import Response

from app.context import Context
from app.errors import JSON_UTF8


def signed_json(ctx: Context, document: dict, status_code: int = 200) -> Response:
    body = json.dumps(document, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    headers = {
        "X-Matrux-Signature": ctx.signer.sign(body),
        "X-Palma-Signature": ctx.signer.sign(body),
        "Cache-Control": "no-store",
    }
    return Response(body, status_code=status_code, media_type=JSON_UTF8, headers=headers)
