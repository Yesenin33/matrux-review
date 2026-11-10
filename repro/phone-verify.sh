#!/bin/sh
d="${1:-debian-xfce}"
rot="${2:-port}"
adb logcat -c
adb shell am force-stop ru.palma.app
sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$d" --es palma_rotation "$rot" >/dev/null 2>&1
i=0
while [ "$i" -lt 24 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -oE 'Live\(|Failed\(' | tail -1)
  echo "t=$((i*10))s state=$s"
  case "$s" in
    Live*) echo "RESULT LIVE $((i*10))s"; break ;;
    Failed*) echo "RESULT FAILED $((i*10))s"; break ;;
  esac
  sleep 10
  i=$((i + 1))
done
sleep 25
echo "--- trace.log (хвост) ---"
adb shell "run-as ru.palma.app tail -n 14 cache/proot/trace.log" 2>/dev/null | tr -d '\r'
echo "--- палма-vnc ---"
adb logcat -d -s palma-vnc 2>/dev/null | tail -4 | tr -d '\r'
echo "--- процессоры гостя (через ps внутри proot) ---"
adb shell "run-as ru.palma.app sh -c 'ls files/systems/$d/usr/bin/Xvnc >/dev/null 2>&1 && echo Xvnc=present || echo Xvnc=absent'" 2>/dev/null | tr -d '\r'
adb exec-out screencap -p > /tmp/verify-$d-$rot.png 2>/dev/null
echo "кадр: $(wc -c < /tmp/verify-$d-$rot.png)"
