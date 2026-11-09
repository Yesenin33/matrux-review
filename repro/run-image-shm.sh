#!/bin/bash
set -e
dir=/root/palma/repro/built
cd "$dir"
for m in dev proc sys; do
  mountpoint -q "$m" || { mount -t "${m}fs" "$m" "$m" 2>/dev/null || mount --bind "/$m" "$m" 2>/dev/null || true; }
done
cp -f /root/palma/image-build/extra/libandroid-shmem.so ./usr/local/lib/android-shmem.so
exec chroot . /usr/bin/qemu-aarch64 /bin/sh -c 'export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; export PALMA_SHM=/usr/local/lib/android-shmem.so; exec /usr/local/bin/palma-session vnc'
