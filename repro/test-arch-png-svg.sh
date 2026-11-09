#!/bin/sh
set -e
R=/root/palma/image-build/arch-rootfs
cd /root/palma/image-build
cp -f /usr/bin/qemu-aarch64 "$R/usr/bin/qemu-aarch64-static"
for d in dev proc sys; do mountpoint -q "$R/$d" || mount --bind "/$d" "$R/$d"; done
chroot "$R" /usr/bin/qemu-aarch64-static /bin/sh -c '
P=/usr/share/icons/Adwaita
echo "== PNG грузится?"
gdk-pixbuf-pixdata $P/16x16/places/folder-documents.png /tmp/t1.pixdata 2>&1 | head -3 && echo "PNG-OK"
echo "== SVG грузится?"
S=$(find $P/scalable -name "*.svg" 2>/dev/null | head -1); echo "файл: $S"
gdk-pixbuf-pixdata "$S" /tmp/t2.pixdata 2>&1 | head -3 && echo "SVG-OK"
echo "== какие каталоги иконок в Adwaita:"
ls $P/scalable/ 2>&1 | head -4
echo "== PNG-наборы в темах:"
for t in Adwaita elementary hicolor locolor HighContrast; do printf "%s: png=%s svg=%s\n" "$t" "$(find /usr/share/icons/$t -name "*.png" 2>/dev/null | wc -l)" "$(find /usr/share/icons/$t -name "*.svg" 2>/dev/null | wc -l)"; done
' || true
for d in dev proc sys; do mountpoint -q "$R/$d" && umount "$R/$d" || true; done
rm -f "$R/usr/bin/qemu-aarch64-static"
echo ТЕСТ2-ГОТОВО
