import hashlib
import os
import uuid
from collections.abc import Iterator
from dataclasses import dataclass
from pathlib import Path
from typing import BinaryIO

from fastapi.responses import StreamingResponse
from starlette.responses import Response

from app.errors import ApiError, error_response

CHUNK = 1024 * 1024


@dataclass(frozen=True)
class ByteRange:
    start: int
    end: int

    @property
    def length(self) -> int:
        return self.end - self.start + 1


class RangeNotSatisfiable(Exception):
    pass


class BlobTooLarge(Exception):
    pass


def parse_range(header: str | None, size: int) -> ByteRange | None:
    if not header:
        return None
    unit, _, spec = header.partition("=")
    spec = spec.strip()
    if unit.strip().lower() != "bytes" or "," in spec:
        return None
    first, sep, last = spec.partition("-")
    first, last = first.strip(), last.strip()
    if not sep or not (first.isdigit() or last.isdigit()):
        return None
    if (first and not first.isdigit()) or (last and not last.isdigit()):
        return None
    if not first:
        suffix = int(last)
        if suffix == 0 or size == 0:
            raise RangeNotSatisfiable
        return ByteRange(max(size - suffix, 0), size - 1)
    start = int(first)
    end = int(last) if last else size - 1
    if last and end < start:
        return None
    if start >= size:
        raise RangeNotSatisfiable
    return ByteRange(start, min(end, size - 1))


def _iter_file(handle: BinaryIO, start: int, length: int) -> Iterator[bytes]:
    try:
        handle.seek(start)
        remaining = length
        while remaining > 0:
            chunk = handle.read(min(CHUNK, remaining))
            if not chunk:
                break
            remaining -= len(chunk)
            yield chunk
    finally:
        handle.close()


def serve_blob(
    path: Path,
    sha256: str,
    download_name: str,
    range_header: str | None,
    if_range: str | None,
) -> Response:
    try:
        handle = path.open("rb")
    except FileNotFoundError as exc:
        raise ApiError(503, "file_unavailable") from exc
    size = os.fstat(handle.fileno()).st_size
    etag = f'"{sha256}"'
    headers = {
        "Accept-Ranges": "bytes",
        "ETag": etag,
        "Cache-Control": "private, no-transform",
        "Content-Disposition": f'attachment; filename="{download_name}"',
    }
    honour_range = if_range is None or if_range.strip() == etag
    byte_range = None
    if range_header and honour_range:
        try:
            byte_range = parse_range(range_header, size)
        except RangeNotSatisfiable:
            handle.close()
            return error_response(416, "range_not_satisfiable", {**headers, "Content-Range": f"bytes */{size}"})
    if byte_range is None:
        headers["Content-Length"] = str(size)
        return StreamingResponse(_iter_file(handle, 0, size), 200, headers, "application/octet-stream")
    headers["Content-Length"] = str(byte_range.length)
    headers["Content-Range"] = f"bytes {byte_range.start}-{byte_range.end}/{size}"
    return StreamingResponse(
        _iter_file(handle, byte_range.start, byte_range.length), 206, headers, "application/octet-stream"
    )


def store_blob(source: BinaryIO, directory: Path, max_bytes: int) -> tuple[str, int, Path]:
    directory.mkdir(parents=True, exist_ok=True)
    temp = directory / f".upload-{uuid.uuid4().hex}"
    digest = hashlib.sha256()
    size = 0
    try:
        with temp.open("wb") as out:
            while chunk := source.read(CHUNK):
                size += len(chunk)
                if size > max_bytes:
                    raise BlobTooLarge
                digest.update(chunk)
                out.write(chunk)
            out.flush()
            os.fsync(out.fileno())
    except BaseException:
        temp.unlink(missing_ok=True)
        raise
    return digest.hexdigest(), size, temp


def commit_blob(temp: Path, directory: Path, sha256: str) -> Path:
    final = directory / sha256
    if final.exists():
        temp.unlink(missing_ok=True)
    else:
        os.replace(temp, final)
    return final
