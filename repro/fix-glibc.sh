#!/bin/bash
# Починка glibc-гостя: заменить расходящиеся библиотеки/симлинки эталонными из rootfs.
# Аргументы: <image-id> <rootfs-эталон>
set -u
ID="${1:?image-id}"; RF="${2:?rootfs}"
H="root@$(cut -d: -f1 /tmp/gol-addr.txt 2>/dev/null || echo shjwb-57-128-216-98.run.pinggy-free.link)"
LIB=usr/lib/aarch64-linux-gnu
TMP=$(mktemp -d)

ssh -S /tmp/gol.sock -o BatchMode=yes $H "adb shell \"run-as ru.palma.app ls -l files/systems/$ID/$LIB/\" | tr -d '\\r'" 2>/dev/null > "$TMP/guest.txt"
ls -l "$RF/$LIB/" > "$TMP/host.txt"

python3 - "$TMP" "$LIB" "$RF" <<'PY' > "$TMP/fix.list"
import sys, os
tmp, lib, rf = sys.argv[1], sys.argv[2], sys.argv[3]
def parse(p):
    d={}
    for line in open(p, errors='replace'):
        line=line.rstrip('\n')
        if ' -> ' in line:
            l,t=line.split(' -> ',1); d[l.split()[-1]]=('link',t.strip())
        else:
            f=line.split()
            if len(f)<8: continue
            d[f[-1]]=('file',f[4])
    return d
g=parse(tmp+'/guest.txt'); h=parse(tmp+'/host.txt')
names=set()
for n,(k,v) in h.items():
    if os.path.isdir(os.path.join(rf,lib,n)): continue
    if n not in g or g[n][0]!=k or g[n][1]!=v: names.add(n)
for n in sorted(names):
    k,v=h[n]
    print(n)
    if k=='link':
        tgt=v.split('/')[-1]
        if tgt in h: print(tgt)
PY
sort -u "$TMP/fix.list" > "$TMP/fix.uniq"
echo "к починке: $(wc -l < "$TMP/fix.uniq") записей"

PATHS=""; while read -r n; do PATHS="$PATHS $LIB/$n"; done < "$TMP/fix.uniq"
tar -cf /tmp/libfix.tar -C "$RF" $PATHS 2>/dev/null
ls -la /tmp/libfix.tar
base64 -w0 /tmp/libfix.tar | ssh -S /tmp/gol.sock -o BatchMode=yes $H "
base64 -d > /tmp/libfix.tar
adb push /tmp/libfix.tar /data/local/tmp/libfix.tar >/dev/null 2>&1
adb shell \"run-as ru.palma.app sh -c 'cd files/systems/$ID && tar -xf /data/local/tmp/libfix.tar && find $LIB -type f -exec chmod 755 {} + '\"
echo '-- проверка ключевых:'
adb shell \"run-as ru.palma.app ls -l files/systems/$ID/$LIB/libz.so.1 files/systems/$ID/$LIB/libcap.so.2 files/systems/$ID/$LIB/libgcc_s.so.1\" | tr -d '\r'" 2>&1 | grep -v Warning
rm -rf "$TMP"
