#!/bin/bash
set -e
cd /root/palma/repro/alpine
for m in dev proc sys; do
  mountpoint -q "$m" || mount -t "${m}fs" "${m}" "${m}" 2>/dev/null || mount --bind "/${m}" "$m" 2>/dev/null || true
done
exec chroot . /usr/bin/qemu-aarch64 /bin/sh -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; exec /usr/local/bin/palma-session vnc'
