#!/bin/sh
cd /data/data/ru.palma.app || exit 9
LIB="$1"
id="${2:-debian-xfce}"
port="${3:-5901}"
geom="${4:-720x1600}"
echo "== запуск $id порт $port дисплей :0"
ls -la files/systems/$id/usr/local/bin/palma-session || echo "скрипта нет"
rm -f cache/proot/trace.log cache/proot/x11vnc.log
PROOT_TMP_DIR=/data/data/ru.palma.app/cache/proot \
PROOT_LOADER=$LIB/libproot-loader.so \
LD_LIBRARY_PATH=$LIB \
$LIB/libproot.so --kill-on-exit --link2symlink --sysvipc -0 \
  -r files/systems/$id \
  -b /data/data/ru.palma.app/cache/proot/shm:/dev/shm \
  -b /dev -b /dev/pts -b /proc -b /sys \
  -b /data/data/ru.palma.app/cache/proot:/tmp \
  -w /root \
  /usr/bin/env -i HOME=/root USER=root LANG=ru_RU.UTF-8 TERM=xterm-256color \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  DISPLAY=:0 PALMA_VNC_PORT=$port PALMA_GEOMETRY=$geom \
  /bin/sh /usr/local/bin/palma-session vnc
echo "== proot вышел: $?"
