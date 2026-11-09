#!/bin/sh
export DISPLAY=:0
S=$(pgrep -f xfce4-session | head -1)
if [ -n "$S" ]; then
  for v in $(tr '\0' '\n' < /proc/$S/environ 2>/dev/null | grep -E '^(DBUS_SESSION_BUS_ADDRESS|XDG_RUNTIME_DIR)='); do
    export "$v"
  done
fi
echo "dbus=$DBUS_SESSION_BUS_ADDRESS"
setsid xfce4-terminal >/dev/null 2>&1 &
setsid thunar >/dev/null 2>&1 &
sleep 8
ps | grep -c "[x]fce4-terminal"
ps | grep -c "[t]hunar"