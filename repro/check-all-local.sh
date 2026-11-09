#!/bin/bash
# Локальная проверка всех пяти систем под chroot (телефонная геометрия 720x1600):
# стартует palma-session, ждёт 'vnc listening', снимает кадр и считает непустые пиксели.
cd /root/palma || exit 1
IMAGE_BUILD=/root/palma/image-build

kill_guests() {
  for pid in /proc/[0-9]*; do
    b=$(tr '\0' '\n' < "$pid/cmdline" 2>/dev/null | head -1)
    case "$b" in /usr/bin/qemu-aarch64) kill -9 "${pid#/proc/}" 2>/dev/null;; esac
  done
  sleep 3
}

check_one() {
  local dir=$1 id=$2 port=$3
  local root="$IMAGE_BUILD/$dir"
  echo "=== $id ($dir, порт $port)"
  [ -d "$root" ] || { echo "  нет каталога $root"; return 1; }
  kill_guests
  for m in dev proc sys; do mountpoint -q "$root/$m" || mount --bind "/$m" "$root/$m"; done
  cp -f /usr/bin/qemu-aarch64 "$root/usr/bin/qemu-aarch64"
  rm -f "$root"/tmp/.X*-lock "$root"/tmp/.X11-unix/X* "$root"/tmp/palma-session.log "$root"/tmp/xvfb-*.log "$root"/tmp/x11vnc.log 2>/dev/null
  ( PALMA_GEOMETRY=720x1600 PALMA_VNC_PORT=$port chroot "$root" /usr/bin/qemu-aarch64 /bin/sh -c 'exec /usr/local/bin/palma-session vnc' >/tmp/verify-$id.log 2>&1 & )
  local ok=""
  for i in $(seq 1 45); do
    sleep 3
    if grep -q "vnc listening" "$root/tmp/palma-session.log" 2>/dev/null; then ok="yes"; break; fi
  done
  echo "  лог: $(tail -3 "$root/tmp/palma-session.log" 2>/dev/null | tr '\n' ' ')"
  if [ -z "$ok" ]; then echo "  ИТОГ $id: VNC не поднялся"; fi
  python3 /root/palma/repro/vnc_shot.py "/root/palma/repro/check-$id.png" "$port" 2>&1 | tail -1
  python3 - "$id" <<'PY'
import sys
from PIL import Image
try:
    im = Image.open(f"/root/palma/repro/check-{sys.argv[1]}.png").convert("RGB")
    px = list(im.getdata())
    nonblack = sum(1 for p in px if p != (0, 0, 0))
    print(f"  кадр {im.size}, непустых пикселей {nonblack} из {len(px)} ({100*nonblack//len(px)}%)")
except Exception as e:
    print("  кадр снять не удалось:", e)
PY
  kill_guests
  for m in dev proc sys; do mountpoint -q "$root/$m" && umount -l "$root/$m"; done
  rm -f "$root/usr/bin/qemu-aarch64"
  rm -rf "$root"/tmp/.X11-unix/X* "$root"/tmp/runtime-root "$root"/tmp/.cache "$root"/tmp/.ICE-unix 2>/dev/null
  echo "  === $id завершён"
}

check_one rootfs         alpine-xfce  5951
check_one debian13-rootfs debian-xfce 5952
check_one ubuntu-rootfs  ubuntu-xfce  5953
check_one arch-rootfs    arch-xfce    5954
check_one dev-rootfs     dev-xfce     5955
echo "=== все пять проверены"
