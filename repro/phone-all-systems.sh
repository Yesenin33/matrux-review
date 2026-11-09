#!/bin/sh
J() { adb logcat -d -s "$1" 2>/dev/null; }
for SYS in alpine-xfce debian-xfce ubuntu-xfce; do
  echo "=== $SYS"
  adb logcat -c 2>/dev/null
  adb shell am force-stop ru.palma.app; sleep 4
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$SYS" --es palma_rotation port >/dev/null 2>&1
  i=0; live=""
  while [ $i -lt 28 ]; do
    s=$(J palma-autotest | grep -o "состояние: [A-Za-z]*" | tail -1)
    case "$s" in *Live*) live=1; echo "  Live через $((i*15))s"; break;; esac
    sleep 15; i=$((i+1))
  done
  [ -z "$live" ] && { echo "  не поднялось ($s)"; continue; }
  sleep 20
  adb exec-out screencap -p > "/tmp/m-$SYS-port.png"
  echo "  кадр: $(J palma-vnc | tail -1)"
  echo "  процессы: $(adb shell ps -A -o NAME 2>/dev/null | grep -cE 'Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel')"
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$SYS" --es palma_rotation land >/dev/null 2>&1
  i=0
  while [ $i -lt 10 ]; do
    sleep 10
    last=$(J palma-vnc | tail -1)
    case "$last" in *x900*|*x720*) break;; esac
    i=$((i+1))
  done
  sleep 25
  adb exec-out screencap -p > "/tmp/m-$SYS-land.png"
  echo "  альбом: $(J palma-vnc | tail -1)"
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$SYS" --es palma_rotation port >/dev/null 2>&1
  sleep 25
  echo "  назад: $(J palma-vnc | tail -1)"
  ls -la "/tmp/m-$SYS-port.png" "/tmp/m-$SYS-land.png" 2>/dev/null | awk '{print "  "$5, $9}'
done
echo "=== готово"