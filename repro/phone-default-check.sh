#!/bin/sh
# Проверка дефолтного вида системы на телефоне: снимок портрет + альбом, состояние гостя.
IMG="$1"
echo "== система: $IMG"
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation port >/dev/null 2>&1
i=0; live=""
while [ $i -lt 40 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  case "$s" in *Live*) live=1; echo "  Live через $((i*15))s"; break;; esac
  sleep 15; i=$((i+1))
done
[ -z "$live" ] && { echo "  не поднялось: $s"; adb logcat -d -s palma-autotest 2>/dev/null | tail -5; exit 1; }
sleep 40
adb exec-out screencap -p > "/tmp/def-$IMG-port.png"
echo "  портрет: $(ls -la /tmp/def-$IMG-port.png | awk '{print $5}') б"
echo "  кадр: $(adb logcat -d -s palma-vnc 2>/dev/null | tail -1)"
echo "  процессы: $(adb shell ps -A -o NAME 2>/dev/null | grep -cE 'Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel')"
echo "  язык: $(adb shell "run-as ru.palma.app cat cache/proot/xfce.log" 2>/dev/null | grep -ciE 'ru_RU')"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation land >/dev/null 2>&1
sleep 85
adb exec-out screencap -p > "/tmp/def-$IMG-land.png"
echo "  альбом: $(ls -la /tmp/def-$IMG-land.png | awk '{print $5}') б"
adb logcat -d -s palma-vnc 2>/dev/null | tail -2