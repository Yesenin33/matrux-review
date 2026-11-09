import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path

from app.clock import to_iso, utcnow

SCHEMA = """
CREATE TABLE IF NOT EXISTS images (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    distro TEXT NOT NULL,
    desktop TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    sha256 TEXT NOT NULL UNIQUE,
    min_ram_mb INTEGER NOT NULL,
    pro INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS purchases (
    purchase_id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    tier TEXT NOT NULL,
    state TEXT NOT NULL,
    expires_at TEXT,
    checked_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS purchases_device ON purchases (device_id);
CREATE TABLE IF NOT EXISTS home_backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    sha256 TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS home_backups_device ON home_backups (device_id, id);
CREATE TABLE IF NOT EXISTS devices (
    device_id TEXT PRIMARY KEY,
    key_hash TEXT NOT NULL,
    name TEXT,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS catalog (
    key INTEGER PRIMARY KEY CHECK (key = 1),
    version INTEGER NOT NULL,
    updated_at TEXT NOT NULL
);
"""


class Database:
    def __init__(self, path: Path) -> None:
        self.path = path
        with self.connect() as conn:
            conn.execute("PRAGMA journal_mode=WAL")
            conn.executescript(SCHEMA)
            conn.execute(
                "INSERT OR IGNORE INTO catalog (key, version, updated_at) VALUES (1, 1, ?)",
                (to_iso(utcnow()),),
            )

    @contextmanager
    def connect(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self.path, timeout=10)
        conn.row_factory = sqlite3.Row
        try:
            with conn:
                yield conn
        finally:
            conn.close()


def bump_catalog(conn: sqlite3.Connection) -> None:
    conn.execute(
        "UPDATE catalog SET version = version + 1, updated_at = ? WHERE key = 1",
        (to_iso(utcnow()),),
    )
