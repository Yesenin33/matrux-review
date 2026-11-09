#!/bin/bash
# Прогон всех систем: портрет -> окна -> альбом -> обратно, со снятием кадров.
LIB="$1"
[ -z "$LIB" ] && { echo "нет LIB"; exit 1; }
for img in alpine-xfce debian-xfce ubuntu-xfce arch-xfce; do
  echo "=== $img"
  adb logcat -c
  adb shell am force-stop ru.palma.app; sleep 4
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation port >/dev/null 2>&1
  i=0
  while [ $i -lt 70 ]; do
    s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -o "состояние: [A-Za-z]*" | tail -1)
    case "$s" in *Live*) echo "$img Live $((i*15))s"; break;; esac
    sleep 15; i=$((i+1))
  done
  sleep 25
  adb exec-out screencap -p > "/tmp/r-$img-p.png"
  adb push /tmp/ge.sh /data/local/tmp/ge.sh >/dev/null 2>&1
  adb shell "run-as ru.palma.app sh /data/local/tmp/ge.sh $LIB $img /bin/sh -c 'export DISPLAY=:0; (xfce4-terminal >/dev/null 2>&1 &); sleep 4; (thunar >/dev/null 2>&1 &); sleep 4; echo окна-запущены'" 2>&1 | tail -2
  sleep 15
  adb exec-out screencap -p > "/tmp/r-$img-pw.png"
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation land >/dev/null 2>&1
  sleep 50
  adb exec-out screencap -p > "/tmp/r-$img-l.png"
  adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$img" --es palma_rotation port >/dev/null 2>&1
  sleep 50
  adb exec-out screencap -p > "/tmp/r-$img-p2.png"
  adb shell "run-as ru.palma.app cat cache/proot/trace.log" 2>/dev/null | grep -E "Xvnc|vnc listening" | tail -2
  for f in p pw l p2; do echo "$img $f $(stat -c%s /tmp/r-$img-$f.png 2>/dev/null)"; done
done
echo "=== всё"
