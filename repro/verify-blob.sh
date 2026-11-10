#!/bin/bash
# Локальная проверка ОПУБЛИКОВАННОГО блоба: распаковать, поднять стол под chroot+qemu,
# снять кадр. Аргументы: <sha256 блоба> <каталог> <имя системы>
set -u
SHA="${1:?sha блоба}"; D="${2:?каталог}"; NAME="${3:?имя}"
B="/root/palma/backend/data/images/$SHA"
[ -f "$B" ] || { echo "нет блоба $B"; exit 1; }

echo "== распаковка $NAME в $D =="
rm -rf "$D"; mkdir -p "$D"
zstd -dc "$B" | tar -xf - -C "$D" 2>/dev/null
cp /usr/bin/qemu-aarch64 "$D/usr/bin/qemu-aarch64"
for m in dev proc sys; do
  mountpoint -q "$D/$m" || mount --bind "/$m" "$D/$m" 2>/dev/null || true
done
echo "libc: $(stat -c%s "$D/usr/lib/aarch64-linux-gnu/libc.so.6" 2>/dev/null)"
echo "Xvnc: $(readlink -f "$D/usr/bin/Xvnc" 2>/dev/null || echo нет)"
echo "супервизор: $(sha256sum "$D/usr/local/bin/palma-session" | cut -c1-16)"

rm -f "$D/tmp/trace.log" "$D/tmp/xvnc.log" "$D/tmp/.X0-lock" "$D/tmp/.X11-unix/X0" 2>/dev/null
echo "== запуск стола =="
( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c \
  "export PALMA_GEOMETRY=900x2000 PALMA_VNC_PORT=5901 LANG=ru_RU.UTF-8 NO_AT_BRIDGE=1 GIO_USE_VFS=local TMPDIR=/tmp; exec /usr/local/bin/palma-session vnc" \
  >"/root/palma/repro/$NAME-local.log" 2>&1 & echo $! > "/root/palma/repro/$NAME.pid" )

for i in $(seq 1 40); do
  sleep 3
  if grep -q 'desktop up' "$D/tmp/trace.log" 2>/dev/null; then break; fi
done
echo "== trace =="; tail -12 "$D/tmp/trace.log" 2>/dev/null
echo "== xvnc.log =="; tail -5 "$D/tmp/xvnc.log" 2>/dev/null
echo "== порт 5901 =="; ss -ltn 2>/dev/null | grep 5901 || echo "не слушает"
echo "== кадр =="
python3 /root/palma/repro/vnc_shot.py "/root/palma/repro/$NAME-local.png" 5901 2>&1 | tail -3
ls -la "/root/palma/repro/$NAME-local.png" 2>/dev/null
