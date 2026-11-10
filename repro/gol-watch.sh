#!/bin/bash
# Сторож туннеля до Гола: ищет свежий адрес в ntfy, поднимает мастер-соединение,
# проверяет adb. Живёт до тех пор, пока не поднимет канал.
K=/root/.ssh/id_ed25519_gole
SOCK=/tmp/gol.sock
DOMAIN="run.pinggy-free.link"
OFFSET=0

addr_check() { # $1=host $2=port
  timeout 15 ssh -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
    -p "$2" -i "$K" root@"$1" 'hostname' 2>&1 | grep -q fedora
}

while true; do
  # живой ли уже сокет
  if timeout 15 ssh -S "$SOCK" -o BatchMode=yes -o ConnectTimeout=10 \
       root@shjwb-57-128-216-98.$DOMAIN 'true' >/dev/null 2>&1; then
    echo "$(date +%H:%M:%S) канал жив"; sleep 60; continue
  fi

  # адреса из ntfy
  ADDRS=$(curl -s "https://ntfy.sh/gole-k7qm4x2v/json?poll=1&since=15m" 2>/dev/null | \
    python3 -c "
import sys,json
out=[]
for l in sys.stdin:
    l=l.strip()
    if not l: continue
    try: d=json.loads(l)
    except: continue
    for tok in d.get('message','').split():
        if tok.startswith('tcp://'):
            out.append(tok[6:])
for a in reversed(out): print(a)
" 2>/dev/null)

  if [ -z "$ADDRS" ]; then echo "$(date +%H:%M:%S) адресов нет"; sleep 60; continue; fi

  FOUND=""
  for a in $ADDRS; do
    h=${a%%:*}; p=${a##*:}
    if addr_check "$h" "$p"; then FOUND="$h $p"; break; fi
  done

  if [ -z "$FOUND" ]; then echo "$(date +%H:%M:%S) адреса есть, но не отвечают"; sleep 60; continue; fi

  set -- $FOUND
  echo "$(date +%H:%M:%S) ПОДНЯЛ канал $1:$2"
  pkill -f 'ssh -N -M -S /tmp/gol.sock' 2>/dev/null
  rm -f "$SOCK"
  ssh -M -N -S "$SOCK" -o ControlPersist=900 -o ServerAliveInterval=30 \
      -o StrictHostKeyChecking=no -i "$K" -p "$2" root@"$1" >/dev/null 2>&1 &
  sleep 8
  ssh -S "$SOCK" -o BatchMode=yes root@"$1" "hostname; adb devices | tail -n +2" 2>&1 | grep -v Warning
  echo "$1:$2" > /tmp/gol-addr.txt
  sleep 30
done
