#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb install -r -d /tmp/matrux-0.1.30.apk 2>&1 | tail -1
adb shell dumpsys package ru.palma.app | grep -m1 versionName
adb push /tmp/palma-session /data/local/tmp/palma-session >/dev/null 2>&1
echo "== ставлю супервизор во все установленные системы:"
for d in $(adb shell run-as ru.palma.app ls files/systems 2>/dev/null | tr -d '\r'); do
  adb shell run-as ru.palma.app sh -c "cat /data/local/tmp/palma-session > files/systems/$d/usr/local/bin/palma-session && chmod 755 files/systems/$d/usr/local/bin/palma-session" 2>/dev/null && echo "  $d ok"
done
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 3
echo "== запуск в портрете:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 12 ]; do
  sleep 15
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  echo "+$((($i+1)*15))s $s"
  case "$s" in *Live*) break;; esac
  i=$((i+1))
done
sleep 10
adb exec-out screencap -p > /tmp/s-port.png
echo "== кадр:"; adb logcat -d -s palma-vnc 2>/dev/null | tail -2
echo "== процессы гостя:"; adb shell ps -A -o PID,NAME 2>/dev/null | grep -E "Xvfb|x11vnc|xfwm4|xfdesktop|xfce4-panel|xfce4-power|notifyd" | head -8
echo "== CPU:"; adb shell dumpsys cpuinfo 2>/dev/null | grep -E "Xvfb|x11vnc|xfwm|xfdesktop|xfce4|proot" | head -8
echo "== В АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land >/dev/null 2>&1
sleep 45
adb exec-out screencap -p > /tmp/s-land.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -2
echo "== назад в ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
sleep 40
adb exec-out screencap -p > /tmp/s-back.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -3
ls -la /tmp/s-port.png /tmp/s-land.png /tmp/s-back.png