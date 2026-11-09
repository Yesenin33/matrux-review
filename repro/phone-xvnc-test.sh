#!/bin/sh
# Проверка Xvnc на телефоне: ставим tigervnc в Alpine-гостя, поднимаем, снимаем CPU и кадр.
LIB="$1"
IMG="${2:-alpine-xfce}"
if [ -z "$LIB" ]; then LIB=$(adb shell pm path ru.palma.app | sed 's#package:##; s#/base.apk##'); LIB="${LIB%/base.apk}/lib/arm64"; fi
echo "== lib: $LIB"

echo "== ставлю tigervnc в гостя"
adb shell run-as ru.palma.app sh /data/local/tmp/ge.sh "$LIB" "$IMG" /bin/sh -c 'apk add --no-cache tigervnc 2>&1 | tail -3; command -v Xvnc || echo "НЕТ Xvnc"'

echo "== перезапуск"
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 30 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  case "$s" in *Live*) echo "  Live через $((i*15))s"; break;; esac
  sleep 15; i=$((i+1))
done
sleep 45
echo "== чем поднят стол:"
adb shell "run-as ru.palma.app cat cache/proot/trace.log" 2>/dev/null | grep -E "Xvnc|vnc listening|x11vnc" | tail -4
echo "== процессы и CPU:"
adb shell top -b -n 1 -d 1 -o PID,%CPU,ARGS 2>/dev/null | grep -E "Xvnc|Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel" | head -8
adb exec-out screencap -p > "/tmp/x-$IMG-port.png"
echo "== кадр: $(adb logcat -d -s palma-vnc 2>/dev/null | tail -1)"
echo "== портрет: $(ls -la /tmp/x-$IMG-port.png | awk '{print $5}') б"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation land >/dev/null 2>&1
sleep 80
adb exec-out screencap -p > "/tmp/x-$IMG-land.png"
echo "== альбом: $(ls -la /tmp/x-$IMG-land.png | awk '{print $5}') б"
adb logcat -d -s palma-vnc 2>/dev/null | tail -2