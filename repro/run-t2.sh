#!/bin/sh
set -e
cd /root/palma/repro
pkill -9 -f "[q]emu-aarch64" 2>/dev/null || true
sleep 1
rm -rf t2/root/.config/xfce4
install -m 755 /root/palma/image-build/palma-session t2/usr/local/bin/palma-session
mkdir -p t2/root/.config/xfce4/xfconf/xfce-perchannel-xml
cp /root/palma/image-build/rootfs/root/.config/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml \
   t2/root/.config/xfce4/xfconf/xfce-perchannel-xml/
for m in dev proc sys; do
  mountpoint -q "t2/$m" || { mount -t "${m}fs" "t2/$m" "t2/$m" 2>/dev/null || mount --bind "/$m" "t2/$m" 2>/dev/null || true; }
done
exec chroot t2 /usr/bin/qemu-aarch64 /bin/sh -c "export PALMA_GEOMETRY=${GEOMETRY:-720x1600}; exec /usr/local/bin/palma-session vnc"
