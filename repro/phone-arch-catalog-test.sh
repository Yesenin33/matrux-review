#!/bin/sh
echo "== устройство:"; adb devices | tail -2
adb shell am force-stop ru.palma.app; sleep 2
for p in $(adb shell "ps -A -o PID,NAME | grep -E 'Xvfb|x11vnc|xfce4|xfwm|xfdesktop' | awk '{print \$1}'" | tr -d '\r'); do adb shell "run-as ru.palma.app kill -9 $p" >/dev/null 2>&1; done
adb shell "run-as ru.palma.app sh -c 'rm -f cache/proot/trace.log cache/proot/bwrap.log'" >/dev/null 2>&1
echo "== запускаю установку из каталога:"
adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image arch-xfce | tail -1
for i in $(seq 1 40); do
  sleep 30
  st=$(adb logcat -d -s palma-autotest:I 2>/dev/null | tail -1 | tr -d '\r')
  echo "+$((i*30))s: $st"
  case "$st" in *Live*) break ;; esac
done
sleep 60
echo "== процессы гостя:"
adb shell "ps -A -o PID,NAME | grep -E 'Xvfb|x11vnc|xfce4|xfwm|xfdesktop'"
echo "== ошибок pixbuf:"
adb shell "run-as ru.palma.app sh -c 'grep -icE pixbuf cache/proot/xfce.log'"
echo "== вызовов обёртки:"
adb shell "run-as ru.palma.app sh -c 'grep -c glycin cache/proot/bwrap.log'"
echo "== перезапусков и хвост:"
adb shell "run-as ru.palma.app sh -c 'grep -cE \"desktop lost\" cache/proot/trace.log; tail -2 cache/proot/trace.log'"
echo "== sha установленного образа:"
adb shell "run-as ru.palma.app cat files/systems/arch-xfce/.palma-ready"
adb exec-out screencap -p > /tmp/arch-final.png
ls -la /tmp/arch-final.png
