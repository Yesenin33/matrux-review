#!/system/bin/sh
NAT=/data/app/~~fWMwhNYOXgNOlnWahqyRhQ==/ru.palma.app-JukKfUKwMDfS_3eN8Uo6Ig==/lib/arm64
DATA=/data/data/ru.palma.app/files
CACHE=/data/data/ru.palma.app/cache
ROOTFS=$DATA/systems/alpine-xfce
TMP=$CACHE/proot
mkdir -p $TMP
export PROOT_TMP_DIR=$TMP
export PROOT_LOADER=$NAT/libproot-loader.so
export LD_LIBRARY_PATH=$NAT
cd $CACHE
echo "--- старт пробы $(date +%H:%M:%S)"
$NAT/libproot.so --kill-on-exit --link2symlink -0 -r $ROOTFS \
  -b /dev -b /proc -b /sys -b $TMP:/tmp -w /root \
  /usr/bin/env -i HOME=/root USER=root LANG=ru_RU.UTF-8 TERM=xterm-256color \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  DISPLAY=:0 PALMA_VNC_PORT=5901 PALMA_GEOMETRY=720x1600 \
  /bin/sh /usr/local/bin/palma-session vnc 2>&1
echo "--- проба завершилась, код=$?"
echo "--- palma-session.log:"
cat $TMP/palma-session.log 2>/dev/null | tail -40
echo "--- x11vnc.log:"
tail -5 $TMP/x11vnc.log 2>/dev/null
echo "--- xvfb-0.log:"
tail -5 $TMP/xvfb-0.log 2>/dev/null
