#!/bin/bash
# Починка библиотек гостя по sha256 (сравнение с эталонным rootfs).
# Аргументы: <image-id> <rootfs-эталон> [подкаталог=usr/lib/aarch64-linux-gnu]
set -u
ID="${1:?image-id}"; RF="${2:?rootfs}"; LIB="${3:-usr/lib/aarch64-linux-gnu}"
H="root@$(cut -d: -f1 /tmp/gol-addr.txt 2>/dev/null || echo shjwb-57-128-216-98.run.pinggy-free.link)"
TMP=$(mktemp -d)

echo "== sha на телефоне =="
ssh -S /tmp/gol.sock -o BatchMode=yes $H "adb shell \"run-as ru.palma.app sh -c 'cd files/systems/$ID/$LIB && sha256sum *'\" | tr -d '\\r'" 2>/dev/null > "$TMP/g.sha"
wc -l < "$TMP/g.sha"
echo "== sha эталона =="
( cd "$RF/$LIB" && sha256sum * ) > "$TMP/h.sha" 2>/dev/null
wc -l < "$TMP/h.sha"

python3 - "$TMP" "$RF" "$LIB" <<'PY' > "$TMP/fix.list"
import sys, os
tmp, rf, lib = sys.argv[1], sys.argv[2], sys.argv[3]
def parse(p):
    d={}
    for line in open(p, errors='replace'):
        f=line.split()
        if len(f)>=2: d[f[1].lstrip('*')]=f[0]
    return d
g=parse(tmp+'/g.sha'); h=parse(tmp+'/h.sha')
names=set()
for n,sh in h.items():
    if os.path.isdir(os.path.join(rf,lib,n)): continue
    if g.get(n)!=sh: names.add(n)
# битые/чужие симлинки: цель обязана существовать в госте и совпадать с эталоном
for n in os.listdir(os.path.join(rf,lib)):
    p=os.path.join(rf,lib,n)
    if os.path.islink(p):
        tgt=os.readlink(p)
        if tgt.split('/')[-1] in h and (tgt.split('/')[-1] not in g or tgt.split('/')[-1] in names):
            names.add(tgt.split('/')[-1])
for n in sorted(names): print(n)
PY
sort -u "$TMP/fix.list" > "$TMP/fix.uniq"
echo "== к починке: $(wc -l < "$TMP/fix.uniq") файлов =="
head -20 "$TMP/fix.uniq"
[ -s "$TMP/fix.uniq" ] || { echo "нечего чинить"; rm -rf "$TMP"; exit 0; }

PATHS=""; while read -r n; do PATHS="$PATHS $LIB/$n"; done < "$TMP/fix.uniq"
tar -cf /tmp/libfix2.tar -C "$RF" $PATHS 2>/dev/null
ls -la /tmp/libfix2.tar
base64 -w0 /tmp/libfix2.tar | ssh -S /tmp/gol.sock -o BatchMode=yes $H "
base64 -d > /tmp/libfix2.tar
adb push /tmp/libfix2.tar /data/local/tmp/libfix2.tar >/dev/null 2>&1
adb shell \"run-as ru.palma.app sh -c 'cd files/systems/$ID && tar -xf /data/local/tmp/libfix2.tar && find $LIB -maxdepth 1 -type f -exec chmod 755 {} +'\"
echo '-- сверка после:'
adb shell \"run-as ru.palma.app sh -c 'cd files/systems/$ID/$LIB && sha256sum *'\" | tr -d '\r' > /tmp/after.sha" 2>&1 | grep -v Warning

python3 - "$TMP" <<'PY'
import sys
tmp=sys.argv[1]
def parse(p):
    d={}
    for line in open(p, errors='replace'):
        f=line.split()
        if len(f)>=2: d[f[1].lstrip('*')]=f[0]
    return d
a=parse('/tmp/after.sha'); h=parse(tmp+'/h.sha')
bad=[n for n,sh in h.items() if a.get(n)!=sh]
print('осталось расхождений:', len(bad))
for n in bad[:15]: print('  ', n)
PY
rm -rf "$TMP"
