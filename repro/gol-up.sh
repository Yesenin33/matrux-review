#!/bin/bash
K=/root/.ssh/id_ed25519_gole
SOCK=/tmp/gol.sock
H=shjwb-57-128-216-98.run.pinggy-free.link
P=44273

echo "$(date +%H:%M:%S) долблю $H:$P"
OK=""
for i in $(seq 1 60); do
  if timeout 15 ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
       -p "$P" -i "$K" root@"$H" 'hostname' 2>/dev/null | grep -q fedora; then
    OK=1; break
  fi
  echo "$(date +%H:%M:%S) попытка $i — нет"
  sleep 10
done
[ -z "$OK" ] && { echo "канал не поднялся за 60 попыток"; exit 1; }

pkill -f 'ssh -N -M -S /tmp/gol.sock' 2>/dev/null; rm -f "$SOCK"
ssh -M -N -S "$SOCK" -o ControlPersist=1800 -o ServerAliveInterval=20 -o ServerAliveCountMax=3 \
    -o StrictHostKeyChecking=no -i "$K" -p "$P" root@"$H" >/dev/null 2>&1 &
sleep 8
echo "КАНАЛ ПОДНЯТ $H:$P"
ssh -S "$SOCK" -o BatchMode=yes root@"$H" 'hostname; uptime | sed "s/^/up:/"; adb devices | tail -n +2' 2>&1 | grep -v Warning
