#!/bin/bash
# Меняет ТОЛЬКО значение ключа installed в shared_prefs/palma_state.xml,
# чтобы автотест пошёл веткой «уже стоит» и не пере-распаковывал систему.
set -u
ID="${1:?укажи image-id}"
H="root@$(cut -d: -f1 /tmp/gol-addr.txt 2>/dev/null || echo shjwb-57-128-216-98.run.pinggy-free.link)"
TMP=$(mktemp -d)

ROW=$(python3 - "$ID" <<'PY'
import sqlite3, sys
c = sqlite3.connect("/root/palma/backend/data/palma.sqlite3")
r = c.execute("select id,name,distro,desktop,size_bytes,sha256 from images where id=?", (sys.argv[1],)).fetchone()
print("|".join(str(x) for x in r) if r else "")
PY
)
[ -z "$ROW" ] && { echo "нет системы $ID в каталоге"; exit 1; }
echo "цель: $(echo "$ROW" | cut -d'|' -f1) $(echo "$ROW" | cut -d'|' -f6 | cut -c1-16)"

ssh -S /tmp/gol.sock -o BatchMode=yes $H 'adb shell am force-stop ru.palma.app' >/dev/null 2>&1
ssh -S /tmp/gol.sock -o BatchMode=yes $H 'adb shell "run-as ru.palma.app cat shared_prefs/palma_state.xml"' 2>/dev/null | tr -d '\r' > "$TMP/cur.xml"
[ -s "$TMP/cur.xml" ] || { echo "не прочитал palma_state.xml"; exit 1; }

python3 - "$TMP/cur.xml" "$TMP/new.xml" "$ROW" <<'PY'
import sys, json, html, re
src, dst, row = sys.argv[1], sys.argv[2], sys.argv[3]
xml = open(src, encoding="utf-8").read()
i, n, d, de, s, sh = row.split("|")
inst = json.dumps({"image_id": i, "name": n, "distro": d, "desktop": de,
                   "size_bytes": int(s), "sha256": sh, "last_launch_ms": 0}, ensure_ascii=False)
inc = html.escape(inst, quote=False)
if '<string name="installed">' in xml:
    out = re.sub(r'<string name="installed">.*?</string>',
                 '<string name="installed">%s</string>' % inc, xml, flags=re.S)
else:
    out = xml.replace("</map>", '    <string name="installed">%s</string>\n</map>' % inc)
open(dst, "w", encoding="utf-8").write(out)
PY

echo "ключей было $(grep -c '<string name=' "$TMP/cur.xml"), стало $(grep -c '<string name=' "$TMP/new.xml")"
base64 -w0 "$TMP/new.xml" | ssh -S /tmp/gol.sock -o BatchMode=yes $H '
base64 -d > /tmp/palma_state.xml
adb push /tmp/palma_state.xml /data/local/tmp/palma_state.xml >/dev/null 2>&1
adb shell "run-as ru.palma.app cp -f /data/local/tmp/palma_state.xml shared_prefs/palma_state.xml"
echo "-- на телефоне:"
adb shell "run-as ru.palma.app cat shared_prefs/palma_state.xml" | tr -d "\r" | grep -oE "image_id[^,]*|signing_key" | head -3
' 2>&1 | grep -v Warning
rm -rf "$TMP"
