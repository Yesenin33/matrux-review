#!/bin/sh
set -e
me=$$
ps -eo pid,ppid,args --no-headers | awk -v me="$me" '$1 != me && $2 != me && $3 ~ /qemu-aarch64/ {print $1}' | while read p; do kill -9 "$p" 2>/dev/null || true; done
sleep 1

for d in /root/palma/repro/t2 /root/palma/image-build/rootfs /root/palma/image-build/debian-rootfs; do
  for m in dev proc sys; do
    if mountpoint -q "$d/$m"; then umount -l "$d/$m" || true; fi
  done
done

still=""
for m in dev proc sys; do
  if mountpoint -q "/root/palma/repro/t2/$m"; then still=1; fi
done
if [ -n "$still" ]; then
  echo "МОНТАЖ ЖИВ, не удаляю repro/t2"
  exit 1
fi
rm -rf /root/palma/repro/t2 /root/palma/repro/t2.png

install -m 755 /root/palma/image-build/palma-session /root/palma/image-build/rootfs/usr/local/bin/palma-session
install -m 755 /root/palma/image-build/palma-session /root/palma/image-build/debian-rootfs/usr/local/bin/palma-session

cd /root/palma/image-build
for pair in "rootfs alpine-xfce-aarch64.tar.zst" "debian-rootfs debian-xfce-aarch64.tar.zst"; do
  set -- $pair
  rm -f "$2"
  tar --exclude=./proc/* --exclude=./dev/* --exclude=./sys/* -cf - -C "$1" . | zstd -8 -T0 -q -o "$2"
done
stat -c "%n %s" alpine-xfce-aarch64.tar.zst debian-xfce-aarch64.tar.zst
sha256sum alpine-xfce-aarch64.tar.zst debian-xfce-aarch64.tar.zst

cd /root/palma
python3 repro/publish_image.py alpine
python3 repro/publish_image.py debian
