#!/bin/sh
J() { adb logcat -d -s "$1" 2>/dev/null; }
wait_live() {
  i=0
  while [ $i -lt 60 ]; do
    s=$(J palma-autotest | grep -o "состояние: [A-Za-z]*" | tail -1)
    case "$s" in *Live*) echo "LIVE через $((i*15))s"; return 0;; esac
    sleep 15
    i=$((i+1))
  done
  echo "не дождался Live: $s"; return 1
}
shot() { adb exec-out screencap -p > "$1"; }
size() { adb dumpsys window 2>/dev/null | grep -m1 mRotation || true; }

echo "== версия:"; adb shell dumpsys package ru.palma.app | grep -m1 versionName
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
wait_live || exit 1
sleep 15
shot /tmp/t2-port.png
echo "== кадр (портрет):"; J palma-vnc | tail -1
echo "== гостевые процессы:"; adb shell ps -A -o NAME 2>/dev/null | grep -E "Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel|xfce4-power|xfce4-notifyd|screensaver" | tr '\n' ' '; echo
echo "== CPU (портрет):"; adb shell dumpsys cpuinfo 2>/dev/null | grep -E "Xvfb|x11vnc|xfwm|xfdesktop|xfce4|proot" | head -6

echo "== ПОВОРОТ В АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land >/dev/null 2>&1
i=0
while [ $i -lt 12 ]; do
  sleep 10
  last=$(J palma-vnc | tail -1)
  echo "  +$((($i+1)*10))s $last"
  case "$last" in *2400x1080*|*2000x900*|*1800x900*|*"1600x720"*) break;; esac
  i=$((i+1))
done
shot /tmp/t2-land.png
echo "== кадр:"; J palma-vnc | tail -1
echo "== гостевые процессы:"; adb shell ps -A -o NAME 2>/dev/null | grep -E "Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel" | tr '\n' ' '; echo

echo "== НАЗАД В ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 12 ]; do
  sleep 10
  last=$(J palma-vnc | tail -1)
  echo "  +$((($i+1)*10))s $last"
  case "$last" in *900x2000*|*720x1600*|*1080x2400*) break;; esac
  i=$((i+1))
done
shot /tmp/t2-back.png
echo "== хвост клиента:"; J palma-vnc | tail -4
ls -la /tmp/t2-port.png /tmp/t2-land.png /tmp/t2-back.png 2>/dev/null