#!/bin/bash
# Живучесть окна при повороте: PID сессии/терминала до и после.
OUT=/tmp/rot-survive.txt
: > $OUT
LIB=$(adb shell pm path ru.palma.app 2>/dev/null | tr -d '\r' | sed 's#package:##; s#/base.apk#/lib/arm64#')
log() { echo "$@" | tee -a $OUT; }
log "LIB $LIB"
IMG="$1"
adb logcat -c
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 40 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  case "$s" in *Live*) log "LIVE $((i*15))s"; break;; esac
  case "$s" in *Failed*) log "FAILED"; break;; esac
  sleep 15; i=$((i+1))
done
sleep 20
pids() { adb shell "ps -A -o PID,NAME" 2>/dev/null | grep -E "xfce4-session|Xvnc|Xvfb|x11vnc|xfce4-terminal|xfwm4" | tr -d '\r' | tr '\n' ';'; }
adb push /tmp/ge2.sh /data/local/tmp/ge2.sh >/dev/null 2>&1
adb shell "run-as ru.palma.app sh /data/local/tmp/ge2.sh $LIB $IMG /bin/sh -c 'export DISPLAY=:0; nohup setsid xfce4-terminal >/dev/null 2>&1 & sleep 6; echo окно'" >/dev/null 2>&1
sleep 8
log "ДО: $(pids)"
adb exec-out screencap -p > /tmp/sv-$IMG-before.png
log "  кадр до $(stat -c%s /tmp/sv-$IMG-before.png 2>/dev/null)"

adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation land >/dev/null 2>&1
sleep 50
log "ПОСЛЕ: $(pids)"
adb exec-out screencap -p > /tmp/sv-$IMG-after.png
log "  кадр после $(stat -c%s /tmp/sv-$IMG-after.png 2>/dev/null)"
adb shell "run-as ru.palma.app cat cache/proot/trace.log" 2>/dev/null | grep -iE "geometry|fallback|xrandr" | tail -4 | sed 's/^/  /' | tee -a $OUT
log "ГОТОВО"