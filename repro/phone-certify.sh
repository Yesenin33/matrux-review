#!/bin/bash
# Прогон всех систем на телефоне с восстановлением туннеля: ставит свежий APK,
# затем по очереди запускает каждую систему, ждёт Live/Lost, снимает скриншот и лог гостя.
cd /root/palma || exit 1
KEY=/root/.ssh/id_ed25519_gole

latest_address() {
  curl -s --max-time 20 "https://ntfy.sh/gole-k7qm4x2v/json?poll=1&since=3h" | python3 -c "
import sys,json,re
rows=[]
for line in sys.stdin:
    line=line.strip()
    if not line: continue
    try: o=json.loads(line)
    except: continue
    a=re.search(r'([\w\.\-]+\.run\.pinggy-free\.link):(\d+)', (o.get('message') or ''))
    if a: rows.append((o.get('time',0), a.group(1), a.group(2)))
rows.sort(); print(('%s %s' % (rows[-1][1], rows[-1][2])) if rows else '')
"
}

ensure_tunnel() {
  if timeout 25 ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=8 root@127.0.0.1 'true' 2>/dev/null; then return 0; fi
  local addr host port
  addr=$(latest_address)
  [ -z "$addr" ] && return 1
  host=${addr% *}
  port=${addr#* }
  rm -f /tmp/gol.sock
  ssh -f -N -M -S /tmp/gol.sock -o BatchMode=yes -o ConnectTimeout=15 -o ServerAliveInterval=20 \
    -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i "$KEY" -p "$port" root@"$host" >/dev/null 2>&1
  sleep 6
  timeout 30 ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=10 root@127.0.0.1 'true' 2>/dev/null
}

S="ssh -o BatchMode=yes -o ControlPath=/tmp/gol.sock -o ConnectTimeout=15 -o ServerAliveInterval=20 root@127.0.0.1"

ensure_tunnel || { echo "нет туннеля"; exit 1; }
scp -o BatchMode=yes -o ControlPath=/tmp/gol.sock dist/matrux-0.1.20.apk root@127.0.0.1:/tmp/m.apk 2>&1 | tail -1
$S 'adb install -r -d /tmp/m.apk 2>&1 | tail -1'
$S 'adb shell dumpsys package ru.palma.app | grep versionName | head -1'

for img in debian-xfce ubuntu-xfce arch-xfce dev-xfce; do
  echo "=== $img ($(date -u +%H:%M) UTC)"
  ensure_tunnel || { echo "  туннель пропал, дальше не иду"; break; }
  $S 'adb logcat -c' >/dev/null 2>&1
  $S 'adb shell am force-stop ru.palma.app' >/dev/null 2>&1
  sleep 3
  $S "adb shell am start -n ru.palma.app/.MainActivity --ez palma_autotest true --es palma_image $img" >/dev/null 2>&1
  res=""
  for i in $(seq 1 60); do
    sleep 20
    ensure_tunnel >/dev/null 2>&1
    line=$($S 'adb logcat -d -s palma-autotest:I | tail -1' 2>/dev/null)
    case "$line" in *Live*|*Lost*) res="$line"; break;; esac
  done
  echo "  итог: ${res:-состояние не появилось за 20 минут}"
  $S 'adb logcat -d -s palma-autotest:I | tail -3'
  $S 'adb shell run-as ru.palma.app sh -c "tail -8 cache/proot/palma-session.log"' 2>/dev/null
  $S 'adb exec-out screencap -p' > "repro/phone-$img-v20.png" 2>/dev/null
  echo "  скриншот repro/phone-$img-v20.png: $(stat -c%s "repro/phone-$img-v20.png" 2>/dev/null) байт"
done
echo "=== прогон завершён"
