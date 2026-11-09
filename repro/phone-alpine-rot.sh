#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb shell dumpsys package ru.palma.app | grep -m1 versionName
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell run-as ru.palma.app rm -rf files/downloads/alpine-xfce.archive.part files/downloads/alpine-xfce.archive.part.sha256
adb shell run-as ru.palma.app rm -f cache/proot/geometry-request
echo "== Alpine, портрет:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image alpine-xfce --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 24 ]; do
  sleep 30
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  echo "+$((($i+1)*30))s $s"
  case "$s" in *Live*) break;; esac
  i=$((i+1))
done
sleep 8
adb exec-out screencap -p > /tmp/a-port.png
echo "== лог клиента:"; adb logcat -d -s palma-vnc 2>/dev/null | tail -3
echo "== процессы гостя:"; adb shell ps -A 2>/dev/null | grep -E "Xvfb|x11vnc|xfce4-session|xfce4-panel|xfdesktop" | wc -l
echo "== в АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image alpine-xfce --es palma_rotation land >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/a-land.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -3
echo "== назад в ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image alpine-xfce --es palma_rotation port >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/a-back.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -4
ls -la /tmp/a-port.png /tmp/a-land.png /tmp/a-back.png