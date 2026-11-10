#!/bin/bash
# Проверка «влезают ли окна»: поднять стол в заданной геометрии, открыть терминал и файлы,
# снять кадр. Аргументы: <sha блоба> <каталог> <имя> <геометрия>
set -u
SHA="${1:?sha}"; D="${2:?каталог}"; NAME="${3:?имя}"; G="${4:?геометрия}"
B="/root/palma/backend/data/images/$SHA"
[ -f "$B" ] || { echo "нет блоба"; exit 1; }

echo "== распаковка $NAME =="
rm -rf "$D"; mkdir -p "$D"
zstd -dc "$B" | tar -xf - -C "$D" 2>/dev/null
cp /usr/bin/qemu-aarch64 "$D/usr/bin/qemu-aarch64"
for m in dev proc sys; do mountpoint -q "$D/$m" || mount --bind "/$m" "$D/$m" 2>/dev/null || true; done
rm -f "$D/tmp/trace.log" "$D/tmp/.X0-lock" 2>/dev/null

echo "== стол $G =="
( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c \
  "export PALMA_GEOMETRY=$G PALMA_VNC_PORT=5901 LANG=ru_RU.UTF-8 NO_AT_BRIDGE=1 GIO_USE_VFS=local TMPDIR=/tmp; exec /usr/local/bin/palma-session vnc" \
  >"/root/palma/repro/$NAME-$G.log" 2>&1 & )

for i in $(seq 1 40); do sleep 3; grep -q 'desktop up' "$D/tmp/trace.log" 2>/dev/null && break; done
tail -3 "$D/tmp/trace.log"
sleep 25

echo "== открываю терминал и файлы =="
chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c \
  "export DISPLAY=:0 LANG=ru_RU.UTF-8 TMPDIR=/tmp; (xfce4-terminal --disable-server >/tmp/term.log 2>&1 &); sleep 2; (thunar >/tmp/thunar.log 2>&1 &); sleep 1; echo ok" 2>&1 | tail -2
sleep 18

echo "== список окон =="
chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c "export DISPLAY=:0; xwininfo -root -children 2>/dev/null | sed -n '5,20p'" 2>&1 | head -18

echo "== кадр =="
python3 /root/palma/repro/vnc_shot.py "/root/palma/repro/$NAME-$G-windows.png" 5901 2>&1 | tail -2
ls -la "/root/palma/repro/$NAME-$G-windows.png" 2>/dev/null
