#!/bin/sh
# Поворот портрет -> альбом на живой сессии. Крутить СОБСТВЕННЫМ поворотом приложения
# (settings put user_rotation MIUI блокирует). Только adb, без вложенного ssh.
d="${1:-ubuntu-xfce}"
echo "== старт в портрете ($d) =="
adb logcat -c
adb shell am force-stop ru.palma.app; sleep 4
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$d" --es palma_rotation port >/dev/null 2>&1
i=0; while [ "$i" -lt 20 ]; do
  s=$(adb logcat -d -s palma-autotest 2>/dev/null | grep -oE "Live\(|Failed\(" | tail -1)
  [ -n "$s" ] && { echo "состояние портрет: $s за $((i*10))s"; break; }
  sleep 10; i=$((i+1))
done
sleep 15
adb shell "run-as ru.palma.app tail -n 6 cache/proot/trace.log" 2>/dev/null | tr -d "\r"
PID1=$(adb shell "run-as ru.palma.app grep -oE 'Xvnc [0-9]+x[0-9]+ на' cache/proot/trace.log" 2>/dev/null | tr -d "\r" | tail -1)
echo "геометрия до : $PID1"
adb exec-out screencap -p > /tmp/rot-port.png 2>/dev/null

echo "== поворот в альбом =="
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image "$d" --es palma_rotation land >/dev/null 2>&1
sleep 30
echo "--- trace после поворота ---"
adb shell "run-as ru.palma.app tail -n 14 cache/proot/trace.log" 2>/dev/null | tr -d "\r"
G2=$(adb shell "run-as ru.palma.app grep -oE 'Xvnc [0-9]+x[0-9]+ на' cache/proot/trace.log" 2>/dev/null | tr -d "\r" | tail -1)
echo "геометрия после: $G2"
echo "--- состояние приложения ---"
adb logcat -d -s palma-autotest 2>/dev/null | tr -d "\r" | tail -4
adb exec-out screencap -p > /tmp/rot-land.png 2>/dev/null
echo "кадр альбом: $(wc -c < /tmp/rot-land.png) байт"
echo "--- процессы гостя ---"
adb shell "ps -A -o PID,NAME" 2>/dev/null | tr -d "\r" | grep -iE 'Xvnc|xfce4-session|xfwm4' | head -5
