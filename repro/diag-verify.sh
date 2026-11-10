#!/bin/sh
# Диагностика системы. Запускается НА Голе, только adb — без вложенного ssh.
d="${1:-arch-xfce}"
adb logcat -c
adb shell am force-stop ru.palma.app
sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$d" --es palma_rotation port >/dev/null 2>&1
i=0; s=""
while [ "$i" -lt 20 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -oE 'Live\(|Failed\(' | tail -1)
  [ -n "$s" ] && { echo "RESULT $s за $((i*10))s"; break; }
  sleep 10; i=$((i+1))
done
[ -z "$s" ] && echo "RESULT нет состояния за 200с"
sleep 15
for f in trace.log xvnc.log x11vnc.log xfce.log xvfb-0.log; do
  echo "===== cache/proot/$f"
  adb shell "run-as ru.palma.app tail -n 12 cache/proot/$f 2>/dev/null" | tr -d '\r'
done
echo "===== /tmp внутри гостя"
adb shell "run-as ru.palma.app tail -n 6 files/systems/$d/tmp/xvnc.log 2>/dev/null" | tr -d '\r'
echo "===== процессы"
adb shell "ps -A -o PID,NAME | grep -iE 'xvnc|x11vnc|Xvfb|xfce4-session|proot'" | tr -d '\r'
echo "===== палма-vnc"
adb logcat -d -s palma-vnc 2>/dev/null | tail -5 | tr -d '\r'
adb exec-out screencap -p > /tmp/diag-$d.png 2>/dev/null
echo "кадр: $(wc -c < /tmp/diag-$d.png)"
echo "ГОТОВО"
