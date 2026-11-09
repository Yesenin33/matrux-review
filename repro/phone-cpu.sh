#!/bin/sh
# Замер: поднять Alpine, дождаться Live, снять CPU гостевых процессов дважды.
IMG="${1:-alpine-xfce}"
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$IMG" --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 30 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  case "$s" in *Live*) echo "Live через $((i*15))s"; break;; esac
  sleep 15; i=$((i+1))
done
sleep 45
echo "--- CPU (1-й замер):"
adb shell top -b -n 1 -d 1 -o PID,%CPU,ARGS 2>/dev/null | grep -E "x11vnc|Xvfb|xfwm4|xfdesktop|xfce4-panel" | head -6
sleep 15
echo "--- CPU (2-й замер):"
adb shell top -b -n 1 -d 1 -o PID,%CPU,ARGS 2>/dev/null | grep -E "x11vnc|Xvfb|xfwm4|xfdesktop|xfce4-panel" | head -6
echo "--- кадр:"
adb logcat -d -s palma-vnc 2>/dev/null | tail -2