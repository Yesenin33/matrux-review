#!/bin/bash
# Полная проверка: все системы, портрет/альбом, окна целиком, кадры.
OUT=/tmp/report.txt
LIB=$(adb shell pm path ru.palma.app 2>/dev/null | tr -d '\r' | sed 's#package:##; s#/base.apk#/lib/arm64#')
[ -z "$LIB" ] && { echo "нет пути к lib"; exit 1; }
: > $OUT
echo "LIB $LIB" | tee -a $OUT
adb push /tmp/wincheck.sh /data/local/tmp/wincheck.sh >/dev/null 2>&1
adb shell 'run-as ru.palma.app cp /data/local/tmp/wincheck.sh cache/proot/wincheck.sh' >/dev/null 2>&1
adb push /tmp/ge.sh /data/local/tmp/ge.sh >/dev/null 2>&1

log() { echo "$@" | tee -a $OUT; }

open_windows() {
  adb shell "run-as ru.palma.app sh /data/local/tmp/ge.sh $LIB $1 /bin/sh -c 'export DISPLAY=:0; (xfce4-terminal >/dev/null 2>&1 &); sleep 5; (thunar >/dev/null 2>&1 &); sleep 4; echo ok'" >/dev/null 2>&1
}

check() {
  adb shell "run-as ru.palma.app sh /data/local/tmp/ge.sh $LIB $1 /bin/sh /tmp/wincheck.sh" 2>/dev/null | tr -d '\r' | tee -a $OUT
}

for img in "$@"; do
  log "===== $img"
  adb logcat -c
  adb shell am force-stop ru.palma.app; sleep 4
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation port >/dev/null 2>&1
  i=0; live=no
  while [ $i -lt 70 ]; do
    s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
    case "$s" in *Live*) live=yes; log "LIVE $((i*15))s"; break;; esac
    case "$s" in *Failed*) log "FAILED launch"; break;; esac
    sleep 15; i=$((i+1))
  done
  [ "$live" = yes ] || { log "ИТОГ $img пропущен"; continue; }
  sleep 20
  adb shell "run-as ru.palma.app cat cache/proot/trace.log" 2>/dev/null | grep -E "Xvnc|listening" | tail -1 | sed 's/^/  /' | tee -a $OUT
  open_windows "$img"
  sleep 10
  log "-- портрет"
  check "$img"
  adb exec-out screencap -p > "/tmp/f-$img-p.png"
  log "  кадр портрет $(stat -c%s /tmp/f-$img-p.png 2>/dev/null)"

  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation land >/dev/null 2>&1
  sleep 55
  log "-- альбом"
  check "$img"
  adb exec-out screencap -p > "/tmp/f-$img-l.png"
  log "  кадр альбом $(stat -c%s /tmp/f-$img-l.png 2>/dev/null)"

  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation port >/dev/null 2>&1
  sleep 55
  log "-- портрет обратно"
  check "$img"
  adb exec-out screencap -p > "/tmp/f-$img-p2.png"
  log "  кадр обратно $(stat -c%s /tmp/f-$img-p2.png 2>/dev/null)"
done
log "===== ГОТОВО"