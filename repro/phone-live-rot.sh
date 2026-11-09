#!/bin/sh
echo "== ждём Live:"
i=0
while [ $i -lt 14 ]; do
  s=$(adb logcat -d 2>/dev/null | grep -o "состояние: .*" | tail -1)
  echo "+$((i*30))s $s"
  case "$s" in *Live*) echo "LIVE-ЕСТЬ"; break;; esac
  i=$((i+1)); sleep 30
done
adb exec-out screencap -p > /tmp/l-port.png
echo "== без перезапуска в АЛЬБОМ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation land >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/l-land.png
echo "== обратно в ПОРТРЕТ:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce --es palma_rotation port >/dev/null 2>&1
sleep 30
adb exec-out screencap -p > /tmp/l-back.png
echo "== процессы гостя:"
adb shell run-as ru.palma.app sh -c 'cat /proc/[0-9]*/comm 2>/dev/null | sort | uniq -c | sort -rn | head -8'
ls -la /tmp/l-port.png /tmp/l-land.png /tmp/l-back.png