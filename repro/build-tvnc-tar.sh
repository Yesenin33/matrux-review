#!/bin/bash
set -u
B=/root/palma/image-build
Q=/usr/bin/qemu-aarch64
R="$1"
D="$B/$R"
OUT="$2"

cp "$Q" "$D/usr/bin/qemu-aarch64-static"
cp /etc/resolv.conf "$D/etc/resolv.conf"
mount -t proc proc "$D/proc" 2>/dev/null

ELF=usr/bin/Xtigervnc
[ -e "$D/usr/bin/Xtigervnc" ] || ELF=usr/bin/Xvnc
LIBS=$(chroot "$D" /usr/bin/qemu-aarch64-static /bin/bash -c \
  "ldd /$ELF 2>/dev/null | grep -oE '/(usr/)?lib[^ ]*' | sort -u" )

echo "--- $R: библиотек найдено: $(echo "$LIBS" | grep -c . )"

LIST="usr/bin/Xvnc usr/bin/Xtigervnc usr/bin/tigervncserver usr/bin/vncpasswd usr/bin/vncconfig usr/sbin/tigervncsession etc/alternatives/Xvnc etc/alternatives/vncserver etc/alternatives/vncsession"
[ -d "$D/usr/lib/xorg/modules/extensions" ] && LIST="$LIST $(cd "$D" && ls -d usr/lib/xorg/modules/extensions/* 2>/dev/null)"
[ -d "$D/usr/share/fonts/X11/misc" ] && LIST="$LIST usr/share/fonts/X11/misc"
for l in $LIBS; do
  p="${l#/}"
  [ -e "$D/$p" ] || continue
  LIST="$LIST $p"
  if [ -L "$D/$p" ]; then
    t=$(readlink -f "$D/$p" 2>/dev/null)
    case "$t" in "$D"/*) r="${t#$D/}"; LIST="$LIST $r";; esac
  fi
done

cd "$D"
tar -cf "$OUT" --exclude='lib/ld-linux*' --exclude='usr/lib/ld-linux*' --exclude='lib/ld.so*' --exclude='usr/lib/ld.so*' $LIST 2>/dev/null
umount "$D/proc" 2>/dev/null
rm -f "$D/usr/bin/qemu-aarch64-static"
echo "--- $R: архив $(stat -c%s "$OUT" 2>/dev/null) байт, файлов: $(tar -tf "$OUT" 2>/dev/null | wc -l)"
