#!/bin/bash
set -x
APK=/root/palma/dist/matrux-0.1.12.apk
S="ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock root@127.0.0.1"
scp -o BatchMode=yes -o ControlPath=/tmp/gol.sock "$APK" root@127.0.0.1:/tmp/matrux-0.1.12.apk || exit 1
$S 'adb install -r -d /tmp/matrux-0.1.12.apk' 2>&1 | tail -3
$S 'adb logcat -c'
$S 'adb shell am force-stop ru.palma.app'
sleep 2
$S 'adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image ubuntu-xfce' 2>&1 | tail -3
sleep 100
$S 'adb logcat -d -s palma-autotest:I palma-session:I | tail -40'
$S 'adb exec-out screencap -p' > /root/palma/repro/phone-ubuntu.png
ls -l /root/palma/repro/phone-ubuntu.png
