#!/bin/bash
set -e
cd /root/palma/image-build
ROOTFS=debian-rootfs
rm -rf $ROOTFS debian-xfce-aarch64.tar.zst
mkdir -p $ROOTFS
cp /usr/bin/qemu-aarch64 /tmp/qemu-aarch64-static
debootstrap --arch=arm64 --variant=minbase --foreign bookworm $ROOTFS http://deb.debian.org/debian > debootstrap.log 2>&1
cp /tmp/qemu-aarch64-static $ROOTFS/usr/bin/qemu-aarch64-static
cp /etc/resolv.conf $ROOTFS/etc/resolv.conf
chroot $ROOTFS /usr/bin/qemu-aarch64-static /debootstrap/debootstrap --second-stage >> debootstrap.log 2>&1
chroot $ROOTFS /usr/bin/qemu-aarch64-static /bin/sh -c "export DEBIAN_FRONTEND=noninteractive; apt-get update -qq && apt-get install -y -qq --no-install-recommends xfce4 xfce4-terminal thunar mousepad xvfb x11vnc dbus-x11 fonts-dejavu fonts-noto-core fonts-noto-color-emoji tzdata sudo curl wget nano htop ca-certificates python3 nodejs git firefox-esr xterm" >> debootstrap.log 2>&1
chroot $ROOTFS /usr/bin/qemu-aarch64-static /bin/sh -c "ln -sf /usr/share/zoneinfo/Europe/Moscow /etc/localtime; echo Europe/Moscow > /etc/timezone"
mkdir -p $ROOTFS/usr/local/bin
cat > $ROOTFS/usr/local/bin/palma-session <<'EOF'
#!/bin/sh
mode="${1:-vnc}"
export DISPLAY=:0
export XDG_RUNTIME_DIR=/tmp/runtime-root
mkdir -p "$XDG_RUNTIME_DIR"
chmod 700 "$XDG_RUNTIME_DIR"
Xvfb :0 -screen 0 1280x800x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
sleep 2
if [ "$mode" = "vnc" ]; then
  x11vnc -display :0 -forever -shared -nopw -rfbport "${PALMA_VNC_PORT:-5901}" -localhost no -noxdamage >/tmp/x11vnc.log 2>&1 &
  sleep 1
fi
exec dbus-run-session startxfce4
EOF
chmod +x $ROOTFS/usr/local/bin/palma-session
rm -f $ROOTFS/usr/bin/qemu-aarch64-static
tar --exclude=./proc/* --exclude=./dev/* --exclude=./sys/* -cf - -C $ROOTFS . | zstd -8 -T0 -q -o debian-xfce-aarch64.tar.zst
ls -la debian-xfce-aarch64.tar.zst
sha256sum debian-xfce-aarch64.tar.zst
