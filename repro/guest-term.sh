#!/bin/sh
# Открыть терминал в живой сессии гостя и выполнить в нём команды (проверка PTY).
export DISPLAY=:0
xfce4-terminal --disable-server --geometry=100x22 -x sh -c 'echo TERMINAL-OK; echo "пользователь: $(id -un)"; ls /root | head -5; sleep 30' >/tmp/term-anim.log 2>&1 &
echo "терминал запущен"
sleep 14
echo "окна:"; xwininfo -root -children 2>/dev/null | grep -i terminal | head -3
