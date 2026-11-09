#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb shell settings put global adb_install_need_confirm 0 >/dev/null 2>&1
echo "== ставлю APK 0.1.22:"
adb install -r /tmp/palma-test-0.1.22.apk 2>&1 | tail -2
G=files/systems/arch-xfce
adb shell run-as ru.palma.app cp /data/local/tmp/xrandr $G/usr/bin/xrandr 2>/dev/null
adb shell run-as ru.palma.app cp /data/local/tmp/palma-session $G/usr/local/bin/palma-session 2>/dev/null
adb shell run-as ru.palma.app chmod 755 $G/usr/bin/xrandr $G/usr/local/bin/palma-session 2>/dev/null
echo "== старт в ПОРТРЕТЕ:"
adb shell am force-stop ru.palma.app; sleep 3
adb shell run-as ru.palma.app rm -f cache/proot/geometry-request cache/proot/trace.log 2>/dev/null
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port | tail -1
sleep 80
echo "-- журнал:"; adb shell run-as ru.palma.app sh -c 'grep -E "framebuffer|resize|geometry" cache/proot/trace.log | tail -4'
adb exec-out screencap -p > /tmp/o-port.png
echo "== БЕЗ ПЕРЕЗАПУСКА перевожу в АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land | tail -1
sleep 35
echo "-- журнал:"; adb shell run-as ru.palma.app sh -c 'grep -E "framebuffer|resize|geometry" cache/proot/trace.log | tail -4'
echo "-- процессы гостя:"; adb shell run-as ru.palma.app sh -c 'for p in /proc/[0-9]*; do c=$(cat $p/comm 2>/dev/null); case "$c" in Xvfb|x11vnc|xfce4-session|xfwm4|xfce4-panel|xfdesktop) echo "  ${p#/proc/} $c";; esac; done'
adb exec-out screencap -p > /tmp/o-land.png
echo "== назад в ПОРТРЕТ (без перезапуска):"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port | tail -1
sleep 35
echo "-- журнал:"; adb shell run-as ru.palma.app sh -c 'grep -E "framebuffer|resize|geometry" cache/proot/trace.log | tail -4'
adb exec-out screencap -p > /tmp/o-back.png
ls -la /tmp/o-port.png /tmp/o-land.png /tmp/o-back.png
