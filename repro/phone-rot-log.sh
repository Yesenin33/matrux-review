#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb shell settings put global adb_install_need_confirm 0 >/dev/null 2>&1
adb install -r /tmp/matrux-0.1.26.apk 2>&1 | tail -1
G=files/systems/arch-xfce
adb shell run-as ru.palma.app cp /data/local/tmp/palma-session $G/usr/local/bin/palma-session
adb shell run-as ru.palma.app chmod 755 $G/usr/local/bin/palma-session
adb logcat -c 2>/dev/null
adb shell am force-stop ru.palma.app; sleep 4
adb shell run-as ru.palma.app rm -f cache/proot/geometry-request
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 10 ]; do
  sleep 15
  n=$(adb shell ps -A 2>/dev/null | grep -cE "xfce4-session|xfce4-panel")
  [ "$n" -ge 2 ] && break
  i=$((i+1))
done
sleep 6
adb exec-out screencap -p > /tmp/t-port.png
echo "== лог клиента (портрет):"; adb logcat -d -s palma-vnc 2>/dev/null | tail -4
echo "== в АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/t-land.png
echo "== лог клиента (альбом):"; adb logcat -d -s palma-vnc 2>/dev/null | tail -4
echo "== назад в ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/t-back.png
echo "== лог клиента (обратно):"; adb logcat -d -s palma-vnc 2>/dev/null | tail -6
ls -la /tmp/t-port.png /tmp/t-land.png /tmp/t-back.png