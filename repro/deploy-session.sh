#!/bin/bash
set -u
H="root@shjwb-57-128-216-98.run.pinggy-free.link"
SSH="ssh -S /tmp/gol.sock -o BatchMode=yes $H"
LOCAL_SHA=$(sha256sum /root/palma/image-build/palma-session | awk '{print $1}')
echo "эталон супервизора: $(echo "$LOCAL_SHA" | cut -c1-16)"

base64 -w0 /root/palma/image-build/palma-session | $SSH 'base64 -d > /tmp/palma-session.new' 2>/dev/null
$SSH 'adb push /tmp/palma-session.new /data/local/tmp/palma-session.new >/dev/null 2>&1' 2>/dev/null

for d in alpine-xfce debian-xfce ubuntu-xfce arch-xfce; do
  $SSH "adb shell \"run-as ru.palma.app cp -f /data/local/tmp/palma-session.new files/systems/$d/usr/local/bin/palma-session\"" >/dev/null 2>&1
  GOT=$($SSH "adb shell \"run-as ru.palma.app sha256sum files/systems/$d/usr/local/bin/palma-session\"" 2>/dev/null | tr -d '\r' | awk '{print $1}')
  if [ "$GOT" = "$LOCAL_SHA" ]; then echo "$d  OK  $(echo $GOT | cut -c1-16)"; else echo "$d  ПРОВАЛ  $(echo ${GOT:-пусто} | cut -c1-16)"; fi
done
