#!/bin/sh
# Почему терминал извне сессии не стартует: hostname и сессионный dbus.
echo "hostname: [$(hostname 2>&1)]"
echo "uname -n: [$(uname -n 2>&1)]"
echo "/etc/hostname: [$(cat /etc/hostname 2>&1)]"
echo "HOSTNAME: [$HOSTNAME]"
echo "DBUS_SESSION_BUS_ADDRESS: [$DBUS_SESSION_BUS_ADDRESS]"
echo "-- dbus-launch true:"
dbus-launch true 2>&1 | head -3
echo "-- dbus-daemon: [$(command -v dbus-daemon)]"
echo "-- попытка терминала через свой bus:"
dbus-run-session -- xfce4-terminal --disable-server --geometry=90x20 -x sh -c 'echo V-BUS-OK; sleep 3' 2>&1 | head -3
echo "rc=$?"
