#!/bin/sh
set -e
R=/root/palma/image-build/arch-rootfs
cd /root/palma/image-build
cp -f /usr/bin/qemu-aarch64 "$R/usr/bin/qemu-aarch64-static"
for d in dev proc sys; do mountpoint -q "$R/$d" || mount --bind "/$d" "$R/$d"; done
chroot "$R" /usr/bin/qemu-aarch64-static /bin/sh -c '
echo "== список загрузчиков (query-loaders):"
gdk-pixbuf-query-loaders 2>&1 | grep -E "^\"|\.so|png|jpeg|svg" | head -20
echo "== сколько загрузчиков:"
gdk-pixbuf-query-loaders 2>/dev/null | grep -cE "^\""
echo "== версия gdk-pixbuf:"
pacman -Q gdk-pixbuf2 librsvg shared-mime-info adwaita-icon-theme gtk3 2>&1
echo "== обои на месте:"
ls -la /usr/share/backgrounds/xfce/ 2>&1 | head -5
echo "== шаблоны стола:"
ls -la /etc/palma/ 2>&1 | head -8
' || true
for d in dev proc sys; do mountpoint -q "$R/$d" && umount "$R/$d" || true; done
rm -f "$R/usr/bin/qemu-aarch64-static"
echo ТЕСТ-ГОТОВО
