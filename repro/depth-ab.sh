#!/bin/bash
# A/B по битам на кадр: одинаковое движение окон, разная глубина Xvnc. Аргументы: <sha> <каталог> <геометрия>
set -u
SHA="${1:?sha}"; D="${2:-/root/palma/repro/da}"; G="${3:-900x2000}"
B="/root/palma/backend/data/images/$SHA"
[ -f "$B" ] || { echo "нет блоба"; exit 1; }

echo "== распаковка =="
rm -rf "$D"; mkdir -p "$D"
zstd -dc "$B" | tar -xf - -C "$D" 2>/dev/null
cp /usr/bin/qemu-aarch64 "$D/usr/bin/qemu-aarch64"
cp /root/palma/image-build/palma-session "$D/usr/local/bin/palma-session"; chmod 755 "$D/usr/local/bin/palma-session"
for m in dev proc sys; do mountpoint -q "$D/$m" || mount --bind "/$m" "$D/$m" 2>/dev/null || true; done

stop_all() {
  for p in $(ps -eo pid,comm | awk '$2=="Xvnc" || $2=="qemu-aarch64" || $2=="xfce4-session" || $2=="xfwm4" || $2=="xmessage" {print $1}'); do
    kill -9 "$p" 2>/dev/null
  done
  sleep 1
}

run_one() {
  local name="$1" depth="$2" extra="$3"
  stop_all
  rm -f "$D/tmp/trace.log" "$D/tmp/.X0-lock" "$D/tmp/xvnc.log"
  ( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c \
    "export PALMA_GEOMETRY=$G PALMA_VNC_PORT=5901 LANG=ru_RU.UTF-8 NO_AT_BRIDGE=1 GIO_USE_VFS=local TMPDIR=/tmp PALMA_XVNC_DEPTH=$depth PALMA_XVNC_EXTRA='$extra'; exec /usr/local/bin/palma-session vnc" \
    >"/root/palma/repro/da-$name.log" 2>&1 & )
  for i in $(seq 1 40); do sleep 3; grep -q 'desktop up' "$D/tmp/trace.log" 2>/dev/null && break; done
  grep -q 'desktop up' "$D/tmp/trace.log" 2>/dev/null || { echo "== $name: стол не поднялся"; tail -2 "$D/tmp/trace.log"; return 1; }
  grep -m1 'Xvnc' "$D/tmp/xvnc.log" >/dev/null 2>&1
  echo "== $name (depth=$depth extra='$extra') =="
  ( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c \
    "export DISPLAY=:0 TMPDIR=/tmp; i=0; while [ \$i -lt 80 ]; do xmessage -timeout 1 -geometry 600x400 \"окно \$i\" >/dev/null 2>&1 & i=\$((i+1)); sleep 0.25; done" >/dev/null 2>&1 & )
  sleep 4
  python3 /root/palma/repro/vnc_flow.py 5901 20 | sed 's/^/   /'
}

run_one depth24 24 ""
run_one depth16 16 ""
run_one cfb16   16 "-CompareFB=2"
run_one cfb24   24 "-CompareFB=2"

echo "== уборка =="
stop_all
for m in $(mount | awk '/palma\/repro\/da/{print $3}' | sort -r); do umount -l "$m" 2>/dev/null; done
rm -rf "$D"
echo ok
