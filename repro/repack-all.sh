#!/bin/bash
# Пересборка всех образов с обновлённым palma-session (супервизор + толерантная проверка порта) и выкладка в каталог.
cd /root/palma/image-build || exit 1
for pair in "rootfs alpine-xfce" "debian13-rootfs debian-xfce" "ubuntu-rootfs ubuntu-xfce" "arch-rootfs arch-xfce" "dev-rootfs dev-xfce"; do
  set -- $pair
  root=$1
  img=$2
  echo "=== $img ($root)"
  for m in dev proc sys; do mountpoint -q "$root/$m" && umount -l "$root/$m"; done
  cp -f palma-session "$root/usr/local/bin/palma-session" || { echo "нет $root"; continue; }
  chmod 755 "$root/usr/local/bin/palma-session"
  rm -f "$root/usr/bin/qemu-aarch64" "$root/usr/bin/qemu-aarch64-static"
  rm -f "$root"/tmp/.X*-lock "$root"/tmp/.X11-unix/X* "$root"/tmp/palma-session.log "$root"/tmp/xvfb-*.log "$root"/tmp/x11vnc.log 2>/dev/null
  rm -rf "$root"/tmp/runtime-root "$root"/tmp/.cache "$root"/tmp/.ICE-unix 2>/dev/null
  size=$(du -sh "$root" | cut -f1)
  tar --exclude=./proc/* --exclude=./dev/* --exclude=./sys/* --exclude=./tmp/* -cf - -C "$root" . | zstd -8 -T0 -q -f -o "$img-aarch64.tar.zst"
  echo "$img: rootfs $size, архив $(stat -c%s "$img-aarch64.tar.zst" 2>/dev/null)"
  python3 /root/palma/repro/publish_catalog.py "$img" "$img-aarch64.tar.zst" 2>&1 | tail -2
  rm -f "$img-aarch64.tar.zst"
  python3 - <<'PY'
import json,os,glob,urllib.request
d=json.load(urllib.request.urlopen('http://localhost:8000/api/manifest', timeout=10))
keep={i['sha256'] for i in d.get('images',[])}
for f in glob.glob('/root/palma/backend/data/images/*'):
    h=os.path.basename(f)
    if len(h)==64 and h not in keep:
        os.remove(f); print('  вычищен старый блоб', h[:12], round(os.path.getsize(f) if os.path.exists(f) else 0))
PY
done
echo "=== готово"; curl -s --max-time 10 localhost:8000/api/manifest | python3 -c "
import sys,json
d=json.load(sys.stdin)
for i in d.get('images',[]): print(i['id'], i['name'], round(i['size_bytes']/1e6,1),'МБ')
print('version', d.get('version'))"
