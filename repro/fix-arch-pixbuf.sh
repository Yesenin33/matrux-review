#!/bin/sh
set -e
R=/root/palma/image-build/arch-rootfs
cd /root/palma/image-build

cp -f /usr/bin/qemu-aarch64 "$R/usr/bin/qemu-aarch64-static"
rm -f "$R/etc/resolv.conf"
cp -f /etc/resolv.conf "$R/etc/resolv.conf"

for d in dev proc sys; do
  mountpoint -q "$R/$d" || mount --bind "/$d" "$R/$d"
done

chroot "$R" /usr/bin/qemu-aarch64-static /bin/sh -c '
set -e
echo "== до:"
ls /usr/lib/gdk-pixbuf-2.0/2.10.0/loaders/ 2>&1 | head -3
echo "== переустановка пакетов с загрузчиками:"
pacman -S --noconfirm --overwrite="*" gdk-pixbuf2 librsvg shared-mime-info 2>&1 | tail -12
echo "== загрузчики после:"
ls -la /usr/lib/gdk-pixbuf-2.0/2.10.0/loaders/ 2>&1 | head -12
echo "== кэш:"
gdk-pixbuf-query-loaders --update-cache && ls -la /usr/lib/gdk-pixbuf-2.0/2.10.0/loaders.cache
echo "== mime:"
update-mime-database /usr/share/mime && ls -la /usr/share/mime/mime.cache
echo "== проверка SVG/PNG:"
python3 - <<PY 2>&1 | tail -5 || true
import gi
gi.require_version("GdkPixbuf","2.0")
from gi.repository import GdkPixbuf
for f in ("/usr/share/icons/Adwaita/16x16/places/user-home.svg",
          "/usr/share/icons/Adwaita/16x16/places/user-home.png"):
    try:
        p = GdkPixbuf.Pixbuf.new_from_file(f)
        print("ok", f, p.get_width(), p.get_height())
    except Exception as e:
        print("FAIL", f, e)
PY
'

for d in dev proc sys; do
  mountpoint -q "$R/$d" && umount "$R/$d" || true
done
rm -f "$R/usr/bin/qemu-aarch64-static"
echo "ГОТОВО"
