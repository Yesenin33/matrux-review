#!/bin/bash
# Повтор телефонного запуска локально через proot (тот же набор аргументов, что в приложении).
cd /root/palma/image-build || exit 1
pat="qemu-""aarch64 /usr/"
for pid in /proc/[0-9]*; do
  c=$(tr '\0' ' ' < $pid/cmdline 2>/dev/null)
  case "$c" in *"$pat"*) kill -9 ${pid#/proc/} 2>/dev/null;; esac
done
sleep 2
for m in dev proc sys; do mountpoint -q ubuntu-rootfs/$m && umount -l ubuntu-rootfs/$m; done
rm -rf /tmp/palmashm /tmp/palmatmp
mkdir -p /tmp/palmashm /tmp/palmatmp
chmod 1777 /tmp/palmashm /tmp/palmatmp
rm -f ubuntu-rootfs/tmp/.X*-lock
exec proot -q /usr/bin/qemu-aarch64 --kill-on-exit --link2symlink -0 \
  -r /root/palma/image-build/ubuntu-rootfs \
  -b /tmp/palmashm:/dev/shm -b /dev -b /dev/pts -b /proc -b /sys -b /tmp/palmatmp:/tmp \
  -w /root /usr/bin/env -i HOME=/root USER=root LANG=ru_RU.UTF-8 TERM=xterm-256color \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  DISPLAY=:0 PALMA_VNC_PORT=5950 PALMA_GEOMETRY=720x1510 \
  /bin/sh /usr/local/bin/palma-session vnc
