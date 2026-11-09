#!/bin/bash
# Прогон всех систем на телефоне: установка APK, автотест, скриншот, логи гостя.
# Требует живого туннеля (sudo /root/scripts/golrun.sh на Голе, адрес в ntfy gole-k7qm4x2v).
APK=${1:-/root/palma/dist/matrux-0.1.18.apk}
SSH="ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=15 root@127.0.0.1"
cd /root/palma || exit 1
scp -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=15 "$APK" root@127.0.0.1:/tmp/m.apk 2>&1 | tail -2
$SSH 'adb install -r -d /tmp/m.apk 2>&1 | tail -2'
$SSH 'adb shell dumpsys package ru.palma.app | grep versionName | head -1'

for img in alpine-xfce debian-xfce ubuntu-xfce arch-xfce dev-xfce; do
  echo "=== $img"
  $SSH 'adb logcat -c' >/dev/null 2>&1
  $SSH 'adb shell am force-stop ru.palma.app' >/dev/null 2>&1
  sleep 3
  $SSH "adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $img" >/dev/null 2>&1
  state=""
  for i in $(seq 1 60); do
    sleep 10
    line=$($SSH 'adb logcat -d -s palma-autotest:I | tail -1')
    case "$line" in
      *Live*|*Lost*|*live*|*lost*) state="$line"; break;;
    esac
  done
  echo "итог: ${state:-нет сигнала за 600 с}"
  $SSH 'adb logcat -d -s palma-autotest:I | tail -4'
  $SSH 'adb shell run-as ru.palma.app sh -c "tail -8 cache/proot/palma-session.log"' 2>/dev/null
  $SSH 'adb exec-out screencap -p' > "repro/phone-$img-018.png" 2>/dev/null
  s=$(stat -c%s "repro/phone-$img-018.png" 2>/dev/null)
  echo "скриншот repro/phone-$img-018.png: ${s:-0} Б"
done
echo "=== готово"
