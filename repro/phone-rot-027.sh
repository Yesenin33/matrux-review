#!/bin/sh
SYS=arch-xfce
echo "== устройство:"; adb devices | tail -2
adb logcat -c 2>/dev/null
echo "== ставлю 0.1.27:"
adb install -r -d /tmp/matrux-0.1.27.apk 2>&1 | tail -1
adb shell dumpsys package ru.palma.app | grep -m1 versionName
adb shell am force-stop ru.palma.app; sleep 3
echo "== портрет:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $SYS --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 10 ]; do
  sleep 20
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
  echo "+$((($i+1)*20))s $s"
  case "$s" in *Live*) break;; esac
  i=$((i+1))
done
sleep 6
echo "-- запрос разрешения у гостя:"; adb shell run-as ru.palma.app cat cache/x11/geometry-request 2>/dev/null; echo
adb exec-out screencap -p > /tmp/n-port.png
echo "== АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $SYS --es palma_rotation land >/dev/null 2>&1
sleep 25
echo "-- запрос:"; adb shell run-as ru.palma.app cat cache/x11/geometry-request 2>/dev/null; echo
adb exec-out screencap -p > /tmp/n-land.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -2
echo "== обратно ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $SYS --es palma_rotation port >/dev/null 2>&1
sleep 25
echo "-- запрос:"; adb shell run-as ru.palma.app cat cache/x11/geometry-request 2>/dev/null; echo
adb exec-out screencap -p > /tmp/n-back.png
adb logcat -d -s palma-vnc 2>/dev/null | tail -3
ls -la /tmp/n-port.png /tmp/n-land.png /tmp/n-back.png