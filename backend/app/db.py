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
CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vk_user_id TEXT NOT NULL UNIQUE,
    phone TEXT,
    email TEXT,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS account_devices (
    device_id TEXT PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts (id),
    linked_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS account_devices_account ON account_devices (account_id);
CREATE TABLE IF NOT EXISTS account_sessions (
    token_hash TEXT PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts (id),
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS account_sessions_account ON account_sessions (account_id);
CREATE TABLE IF NOT EXISTS entitlements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts (id),
    tier TEXT NOT NULL,
    state TEXT NOT NULL,
    expires_at TEXT,
    source TEXT NOT NULL CHECK (source IN ('purchase', 'device')),
    invoice_id TEXT UNIQUE,
    purchase_id TEXT NOT NULL UNIQUE,
    product_id TEXT NOT NULL,
    checked_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS entitlements_account ON entitlements (account_id);
CREATE TABLE IF NOT EXISTS catalog (
    key INTEGER PRIMARY KEY CHECK (key = 1),
    version INTEGER NOT NULL,
    updated_at TEXT NOT NULL
);
"""

COLUMNS = {
    "images": {"sort": "INTEGER NOT NULL DEFAULT 100"},
    "devices": {"last_seen_at": "TEXT"},
    "accounts": {"last_seen_at": "TEXT"},
}


def add_missing_columns(conn: sqlite3.Connection) -> None:
    for table, columns in COLUMNS.items():
        present = {row["name"] for row in conn.execute(f"PRAGMA table_info({table})")}
        for name, definition in columns.items():
            if name not in present:
                conn.execute(f"ALTER TABLE {table} ADD COLUMN {name} {definition}")


class Database:
    def __init__(self, path: Path) -> None:
        self.path = path
        with self.connect() as conn:
            conn.execute("PRAGMA journal_mode=WAL")
            conn.executescript(SCHEMA)
            add_missing_columns(conn)
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
