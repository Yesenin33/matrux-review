#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb shell settings put global adb_install_need_confirm 0 >/dev/null 2>&1
echo "== ставлю 0.1.25:"
adb install -r /tmp/matrux-0.1.25.apk 2>&1 | tail -2
G=files/systems/arch-xfce
adb shell run-as ru.palma.app cp /data/local/tmp/palma-session $G/usr/local/bin/palma-session
adb shell run-as ru.palma.app chmod 755 $G/usr/local/bin/palma-session
echo "== перезапуск в портрете:"
adb shell am force-stop ru.palma.app; sleep 4
adb shell run-as ru.palma.app rm -f cache/proot/geometry-request
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
i=0
while [ $i -lt 10 ]; do
  sleep 20
  n=$(adb shell ps -A 2>/dev/null | grep -cE "xfce4-session|xfce4-panel")
  echo "+$((($i+1)*20))s процессов XFCE: $n"
  [ "$n" -ge 2 ] && break
  i=$((i+1))
done
sleep 8
adb exec-out screencap -p > /tmp/r-port.png
echo "== в АЛЬБОМ без перезапуска:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/r-land.png
echo "-- процессы XFCE после поворота:"; adb shell ps -A 2>/dev/null | grep -cE "xfce4-session|xfce4-panel"
echo "== назад в ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
sleep 25
adb exec-out screencap -p > /tmp/r-back.png
echo "-- журнал x11vnc (xrandr):"; adb shell run-as ru.palma.app cat cache/proot/x11vnc.log 2>/dev/null | grep -i "xrandr\|DesktopSize\|resize" | tail -6
ls -la /tmp/r-port.png /tmp/r-land.png /tmp/r-back.png