import base64
import binascii

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat


class SigningKeyError(ValueError):
    pass


class Signer:
    def __init__(self, encoded_seed: str) -> None:
        try:
            seed = base64.b64decode(encoded_seed.strip(), validate=True)
        except binascii.Error as exc:
            raise SigningKeyError("PALMA_SIGNING_KEY is not valid base64") from exc
        if len(seed) != 32:
            raise SigningKeyError("PALMA_SIGNING_KEY must decode to 32 bytes")
        self._key = Ed25519PrivateKey.from_private_bytes(seed)
        raw_public = self._key.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
        self.public_key_b64 = base64.b64encode(raw_public).decode()

    def sign(self, payload: bytes) -> str:
        return base64.b64encode(self._key.sign(payload)).decode()


def generate_seed() -> str:
    raw = Ed25519PrivateKey.generate().private_bytes_raw()
    return base64.b64encode(raw).decode()
