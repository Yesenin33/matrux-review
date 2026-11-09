#!/bin/bash
# Сторож: ждёт, пока заказчик поднимет туннель до Голы, затем сам ставит свежий APK
# и прогоняет системы по очереди, снимая скриншот и лог гостя.
cd /root/palma || exit 1
TUNNEL_KEY=/root/.ssh/id_ed25519_gole
NTFY="https://ntfy.sh/gole-k7qm4x2v/json?poll=1&since=3h"

latest_address() {
  curl -s --max-time 20 "$NTFY" | python3 -c "
import sys,json,re
rows=[]
for line in sys.stdin:
    line=line.strip()
    if not line: continue
    try: o=json.loads(line)
    except: continue
    m=(o.get('message') or '')
    a=re.search(r'([\w\.\-]+\.run\.pinggy-free\.link):(\d+)', m)
    if a: rows.append((o.get('time',0), a.group(1), a.group(2)))
rows.sort()
print(('%s %s' % (rows[-1][1], rows[-1][2])) if rows else '')
"
}

connect() {
  local addr host port
  addr=$(latest_address)
  [ -z "$addr" ] && return 1
  host=${addr% *}
  port=${addr#* }
  pgrep -f "ssh .*-M .*gol.sock" | while read -r pid; do kill -9 "$pid" 2>/dev/null; done
  rm -f /tmp/gol.sock
  ssh -f -N -M -S /tmp/gol.sock -o BatchMode=yes -o ConnectTimeout=12 -o ServerAliveInterval=20 \
    -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i "$TUNNEL_KEY" \
    -p "$port" root@"$host" >/dev/null 2>&1
  sleep 5
  timeout 40 ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=10 root@127.0.0.1 'hostname' 2>/dev/null | grep -q . && echo "$host:$port"
}

deadline=$(( $(date +%s) + 2700 ))
echo "жду туннель до $(date -u -d "+45 minutes" +%H:%M) UTC"
found=""
while [ "$(date +%s)" -lt "$deadline" ]; do
  found=$(connect)
  if [ -n "$found" ]; then echo "туннель открыт: $found ($(date -u +%H:%M) UTC)"; break; fi
  sleep 45
done
[ -z "$found" ] && { echo "туннель так и не открылся"; exit 1; }

S="ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=15 -o ServerAliveInterval=20 root@127.0.0.1"
scp -o BatchMode=yes -o ControlPath=/tmp/gol.sock dist/matrux-0.1.20.apk root@127.0.0.1:/tmp/m.apk 2>&1 | tail -1
$S 'adb install -r -d /tmp/m.apk 2>&1 | tail -1'
$S 'adb shell dumpsys package ru.palma.app | grep versionName | head -1'

for img in debian-xfce ubuntu-xfce arch-xfce dev-xfce; do
  echo "=== $img ($(date -u +%H:%M) UTC)"
  $S 'adb logcat -c' >/dev/null 2>&1
  $S 'adb shell am force-stop ru.palma.app' >/dev/null 2>&1
  sleep 3
  $S "adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $img" >/dev/null 2>&1
  res=""
  for i in $(seq 1 90); do
    sleep 20
    line=$($S 'adb logcat -d -s palma-autotest:I | tail -1' 2>/dev/null)
    case "$line" in
      *Live*|*Lost*) res="$line"; break;;
    esac
  done
  echo "итог: ${res:-состояние не появилось за 30 минут}"
  $S 'adb logcat -d -s palma-autotest:I | tail -3'
  $S 'adb shell run-as ru.palma.app sh -c "tail -6 cache/proot/palma-session.log"' 2>/dev/null
  $S 'adb exec-out screencap -p' > "repro/phone-$img-fix.png" 2>/dev/null
  echo "скриншот repro/phone-$img-fix.png: $(stat -c%s "repro/phone-$img-fix.png" 2>/dev/null) байт"
done
echo "=== проверка завершена"
