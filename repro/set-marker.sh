#!/bin/bash
# Проставляет в .palma-ready системы sha из текущего каталога,
# чтобы приложение считало её актуальной и запускало (без перекачки).
set -u
ID="${1:?укажи image-id}"
H="root@$(cut -d: -f1 /tmp/gol-addr.txt 2>/dev/null || echo shjwb-57-128-216-98.run.pinggy-free.link)"
SHA=$(curl -s --max-time 8 localhost:8000/api/manifest | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(next((i['sha256'] for i in d.get('images',[]) if i['id']=='$ID'), ''))")
[ -z "$SHA" ] && { echo "нет $ID в каталоге"; exit 1; }
echo "каталожный sha: $(echo "$SHA" | cut -c1-16)"

ssh -S /tmp/gol.sock -o BatchMode=yes $H "adb shell \"run-as ru.palma.app sh -c 'printf %s $SHA > files/systems/$ID/.palma-ready'\"" >/dev/null 2>&1
GOT=$(ssh -S /tmp/gol.sock -o BatchMode=yes $H "adb shell \"run-as ru.palma.app cat files/systems/$ID/.palma-ready\"" 2>/dev/null | tr -d '\r')
if [ "$GOT" = "$SHA" ]; then echo "маркер проставлен: $(echo "$GOT" | cut -c1-16)"; else echo "ПРОВАЛ: получил ${GOT:0:16}"; exit 1; fi
