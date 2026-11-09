#!/bin/sh
cd /data/data/ru.palma.app || exit 9
LIB="$1"
id="${2:-arch-xfce}"
shift 2
[ $# -eq 0 ] && set -- /bin/sh -c 'id'
echo "== exec in $id"
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
  TMPDIR=/tmp XDG_CACHE_HOME=/tmp/.cache XDG_RUNTIME_DIR=/tmp/runtime-root \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  "$@"
echo "== proot вышел: $?"
