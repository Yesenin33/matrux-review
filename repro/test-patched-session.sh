#!/bin/bash
S="ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock root@127.0.0.1"
cd /root/palma || exit 1
scp -o BatchMode=yes -o ControlPath=/tmp/gol.sock dist/matrux-0.1.17.apk root@127.0.0.1:/tmp/ || exit 1
$S 'adb install -r -d /tmp/matrux-0.1.17.apk' 2>&1 | tail -2

for id in ubuntu-xfce debian-xfce; do
  $S "adb shell run-as ru.palma.app mkdir -p files/systems/$id/usr/local/bin"
  $S "adb shell run-as ru.palma.app sh -c 'cat > files/systems/$id/usr/local/bin/palma-session'" < /root/palma/image-build/palma-session
  $S "adb shell run-as ru.palma.app chmod 755 files/systems/$id/usr/local/bin/palma-session"
  $S "adb shell run-as ru.palma.app sh -c 'wc -l files/systems/$id/usr/local/bin/palma-session'"
done

for img in ubuntu-xfce debian-xfce; do
  $S 'adb logcat -c'
  $S 'adb shell am force-stop ru.palma.app'
  sleep 3
  $S "adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $img" >/dev/null 2>&1
  sleep 130
  echo "=== $img:"
  $S 'adb logcat -d -s palma-autotest:I | tail -5'
  $S 'adb shell "run-as ru.palma.app cat cache/proot/palma-session.log 2>&1 | head -6"'
  $S 'adb exec-out screencap -p' > "repro/phone-$img.png"
  ls -l "repro/phone-$img.png"
done
