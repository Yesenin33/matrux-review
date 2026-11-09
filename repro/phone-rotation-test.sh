#!/bin/sh
# Кладёт xrandr и новый palma-session в установленный Arch и проверяет поворот.
echo "== устройство:"; adb devices | tail -2
adb push /tmp/xrandr /data/local/tmp/xrandr | tail -1
adb push /tmp/libXrandr.so.2 /data/local/tmp/libXrandr.so.2 | tail -1
adb push /tmp/palma-session /data/local/tmp/palma-session | tail -1
adb shell chmod 755 /data/local/tmp/xrandr /data/local/tmp/palma-session
adb shell chmod 644 /data/local/tmp/libXrandr.so.2
G=files/systems/arch-xfce
adb shell run-as ru.palma.app cp /data/local/tmp/xrandr $G/usr/bin/xrandr
adb shell run-as ru.palma.app cp /data/local/tmp/libXrandr.so.2 $G/usr/lib/libXrandr.so.2
adb shell run-as ru.palma.app cp /data/local/tmp/palma-session $G/usr/local/bin/palma-session
adb shell run-as ru.palma.app chmod 755 $G/usr/bin/xrandr $G/usr/local/bin/palma-session
adb shell run-as ru.palma.app chmod 644 $G/usr/lib/libXrandr.so.2
adb shell run-as ru.palma.app rm -f cache/proot/geometry-request cache/proot/trace.log
adb shell am force-stop ru.palma.app; sleep 3
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce | tail -1
sleep 75
echo "== ПОВОРОТ 1 (портрет по факту загрузки):"
adb shell run-as ru.palma.app sh -c 'grep -E "framebuffer|geometry |resize" cache/proot/trace.log | tail -3'
adb exec-out screencap -p > /tmp/rot-port.png
echo "== переключаю систему в альбомную:"
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 30
adb shell run-as ru.palma.app sh -c 'grep -E "resize|framebuffer" cache/proot/trace.log | tail -3'
adb exec-out screencap -p > /tmp/rot-land.png
echo "== назад в портрет:"
adb shell settings put system user_rotation 0
sleep 30
adb shell run-as ru.palma.app sh -c 'grep -E "resize|framebuffer" cache/proot/trace.log | tail -3'
adb exec-out screencap -p > /tmp/rot-back.png
ls -la /tmp/rot-port.png /tmp/rot-land.png /tmp/rot-back.png
