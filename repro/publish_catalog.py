import hashlib
import shutil
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

TITLES = {
    "alpine-xfce": ("Alpine + XFCE", "Alpine", "XFCE", 1024, 0),
    "debian-xfce": ("Debian 13 + XFCE", "Debian 13", "XFCE", 1536, 0),
    "arch-xfce": ("Arch Linux + XFCE", "Arch", "XFCE", 1536, 0),
    "ubuntu-xfce": ("Ubuntu + XFCE", "Ubuntu", "XFCE", 2048, 0),
    "dev-xfce": ("Matrux Dev — для кодинга", "Debian 13 + VS Code", "XFCE", 4096, 0),
}

if len(sys.argv) < 3:
    print("использование: publish_catalog.py <id образа> <путь к .tar.zst>")
    raise SystemExit(1)

image_id = sys.argv[1]
src = Path(sys.argv[2])
if image_id not in TITLES:
    print("неизвестный id:", image_id, "| допустимые:", ", ".join(TITLES))
    raise SystemExit(1)
if not src.is_file():
    print("нет файла:", src)
    raise SystemExit(1)

name, distro, desktop, ram, pro = TITLES[image_id]
backend = Path("/root/palma/backend")
images_dir = backend / "data" / "images"
db = backend / "data" / "palma.sqlite3"

digest = hashlib.sha256(src.read_bytes()).hexdigest()
size = src.stat().st_size
now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

conn = sqlite3.connect(db)
row = conn.execute("select sha256 from images where id = ?", (image_id,)).fetchone()
old_sha = row[0] if row else None

shutil.copy2(src, images_dir / digest)

if row is None:
    conn.execute(
        "insert into images (id, name, distro, desktop, size_bytes, sha256, min_ram_mb, pro, active, created_at)"
        " values (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
        (image_id, name, distro, desktop, size, digest, ram, pro, now),
    )
    print("добавлен образ", image_id)
else:
    conn.execute(
        "update images set name = ?, distro = ?, desktop = ?, size_bytes = ?, sha256 = ?, created_at = ? where id = ?",
        (name, distro, desktop, size, digest, now, image_id),
    )
    print("обновлён образ", image_id)

conn.execute("update catalog set version = version + 1, updated_at = ?", (now,))
conn.commit()
for r in conn.execute("select id, name, size_bytes, sha256, pro, active from images order by created_at, id"):
    print("образ:", r)
print("каталог:", conn.execute("select version, updated_at from catalog").fetchone())
conn.close()

if old_sha and old_sha != digest:
    conn = sqlite3.connect(db)
    used = conn.execute("select count(*) from images where sha256 = ?", (old_sha,)).fetchone()[0]
    conn.close()
    stale = images_dir / old_sha
    if used == 0 and stale.is_file():
        stale.unlink()
        print("удалён старый blob", old_sha)
print("blobs:", sorted(p.name[:12] for p in images_dir.iterdir()))
print("выложен", src.name, digest, size)
