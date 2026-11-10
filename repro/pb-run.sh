#!/bin/bash
set -u
D=/root/palma/repro/pb
B=/root/palma/backend/data/images/328c19447e9a9b0d659c472f4b2f53bed06f02b658bf6ac3f5311fa2e380a087
rm -rf "$D"; mkdir -p "$D"
zstd -dc "$B" | tar -xf - -C "$D" 2>/dev/null
cp /usr/bin/qemu-aarch64 "$D/usr/bin/qemu-aarch64"
cp /root/palma/image-build/palma-session "$D/usr/local/bin/palma-session"; chmod 755 "$D/usr/local/bin/palma-session"
for m in dev proc sys; do mountpoint -q "$D/$m" || mount --bind "/$m" "$D/$m" 2>/dev/null || true; done
rm -f "$D/tmp/trace.log"
( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c "export PALMA_GEOMETRY=900x2000 PALMA_VNC_PORT=5901 LANG=ru_RU.UTF-8 NO_AT_BRIDGE=1 GIO_USE_VFS=local TMPDIR=/tmp; exec /usr/local/bin/palma-session vnc" >/tmp/pb.log 2>&1 & )
for i in $(seq 1 40); do sleep 3; grep -q 'desktop up' "$D/tmp/trace.log" 2>/dev/null && break; done
tail -2 "$D/tmp/trace.log"
( chroot "$D" /usr/bin/qemu-aarch64 /bin/sh -c "export DISPLAY=:0 TMPDIR=/tmp; i=0; while [ \$i -lt 40 ]; do xmessage -timeout 1 -geometry 600x400 \"w \$i\" >/dev/null 2>&1 & i=\$((i+1)); sleep 0.2; done" >/dev/null 2>&1 & )
sleep 3
python3 /root/palma/repro/probe.py 5901
