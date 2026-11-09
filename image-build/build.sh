#!/bin/bash
set -e
cd /root/palma/image-build
ROOTFS=rootfs
rm -rf $ROOTFS alpine.tar.gz
mkdir -p $ROOTFS
curl -sL --max-time 300 -o alpine.tar.gz "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/aarch64/alpine-minirootfs-3.20.3-aarch64.tar.gz"
tar xzf alpine.tar.gz -C $ROOTFS
cp /usr/bin/qemu-aarch64-static $ROOTFS/usr/bin/ 2>/dev/null || cp /usr/bin/qemu-aarch64 $ROOTFS/usr/bin/qemu-aarch64-static
cp /etc/resolv.conf $ROOTFS/etc/resolv.conf
cat > $ROOTFS/etc/apk/repositories <<EOF
https://dl-cdn.alpinelinux.org/alpine/v3.20/main
https://dl-cdn.alpinelinux.org/alpine/v3.20/community
EOF
mount -t proc proc $ROOTFS/proc 2>/dev/null || true
mount -t devtmpfs dev $ROOTFS/dev 2>/dev/null || mount --bind /dev $ROOTFS/dev 2>/dev/null || true
chroot $ROOTFS /usr/bin/qemu-aarch64-static /sbin/apk add --no-cache \
  xfce4 xfce4-terminal xfce4-taskmanager thunar mousepad \
  xvfb x11vnc dbus ttf-dejavu font-noto font-noto-emoji \
  tzdata sudo curl wget nano htop ca-certificates \
  python3 nodejs git xterm firefox 2>&1 | tail -20
chroot $ROOTFS /usr/bin/qemu-aarch64-static /bin/sh -c 'ln -sf /usr/share/zoneinfo/Europe/Moscow /etc/localtime; echo "Europe/Moscow" > /etc/timezone'
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
tar cJf alpine-xfce-aarch64.tar.xz -C $ROOTFS . 2>/dev/null
ls -la alpine-xfce-aarch64.tar.xz
sha256sum alpine-xfce-aarch64.tar.xz
