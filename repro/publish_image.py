import hashlib
import shutil
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

PAIRS = {"alpine": "alpine-xfce", "debian": "debian-xfce"}
prefix = sys.argv[1] if len(sys.argv) > 1 else "alpine"
image_id = PAIRS[prefix]

backend = Path("/root/palma/backend")
build_dir = Path("/root/palma/image-build")
image = sorted(p for p in build_dir.iterdir() if p.suffix == ".zst" and p.name.startswith(prefix))[-1]

digest = hashlib.sha256(image.read_bytes()).hexdigest()
size = image.stat().st_size
images_dir = backend / "data" / "images"

db = backend / "data" / "palma.sqlite3"
conn = sqlite3.connect(db)
old_sha = conn.execute("select sha256 from images where id = ?", (image_id,)).fetchone()[0]
conn.close()

shutil.copy2(image, images_dir / digest)
now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

conn = sqlite3.connect(db)
conn.execute("update images set size_bytes = ?, sha256 = ?, created_at = ? where id = ?", (size, digest, now, image_id))
conn.execute("update catalog set version = version + 1, updated_at = ? where key = 1", (now,))
conn.commit()
rows = conn.execute("select id, name, size_bytes, sha256, pro, active from images").fetchall()
catalog = conn.execute("select * from catalog").fetchall()
conn.close()

for row in rows:
    print("image", row)
print("catalog", catalog)
print("выложен", image.name, digest, size)

if old_sha != digest:
    stale = images_dir / old_sha
    if stale.is_file():
        stale.unlink()
        print("удалён старый blob", old_sha)
print("файлы", sorted(p.name for p in images_dir.iterdir()))
