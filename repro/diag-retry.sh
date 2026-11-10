#!/bin/bash
K=/root/.ssh/id_ed25519_gole
SOCK=/tmp/gol.sock
H=shjwb-57-128-216-98.run.pinggy-free.link
P=44273
SYS="${1:-arch-xfce}"

alive() { timeout 15 ssh -S "$SOCK" -o BatchMode=yes -o ConnectTimeout=10 root@"$H" 'true' >/dev/null 2>&1; }
rawalive() { timeout 15 ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p "$P" -i "$K" root@"$H" 'hostname' 2>/dev/null | grep -q fedora; }

up() {
  pkill -f 'ssh -N -M -S /tmp/gol.sock' 2>/dev/null; rm -f "$SOCK"
  ssh -M -N -S "$SOCK" -o ControlPersist=1800 -o ServerAliveInterval=20 -o ServerAliveCountMax=3 \
      -o StrictHostKeyChecking=no -i "$K" -p "$P" root@"$H" >/dev/null 2>&1 &
  sleep 8
}

for attempt in 1 2 3 4 5 6; do
  echo "$(date +%H:%M:%S) попытка $attempt"
  alive || { rawalive && up; }
  if alive; then
    OUT=$(ssh -S "$SOCK" -o BatchMode=yes root@"$H" "sh /tmp/diag-verify.sh $SYS" 2>&1 | grep -v Warning)
    if echo "$OUT" | grep -q 'RESULT'; then
      echo "$OUT"
      echo "ГОТОВО"
      exit 0
    fi
    echo "$(date +%H:%M:%S) прогон сорвался: $(echo "$OUT" | head -2)"
  fi
  sleep 20
done
echo "НЕ УДАЛОСЬ за 6 попыток"
exit 1
