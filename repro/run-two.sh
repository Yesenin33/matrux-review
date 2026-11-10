#!/bin/bash
# Прогон Debian и Ubuntu на быстром пути (Xvnc): перед каждым запуском
# подменяем запись installed, чтобы автотест не пере-распаковывал систему.
H="root@$(cut -d: -f1 /tmp/gol-addr.txt 2>/dev/null || echo shjwb-57-128-216-98.run.pinggy-free.link)"
for d in debian-xfce ubuntu-xfce; do
  echo "######################## $d"
  bash /root/palma/repro/set-installed.sh "$d" 2>&1 | grep -E "цель|ключей|image_id|не "
  ssh -S /tmp/gol.sock -o BatchMode=yes $H "sh /tmp/diag-verify.sh $d" 2>&1 | grep -v Warning | \
    grep -E "RESULT|BOOT|Xvnc|framebuffer|vnc |desktop up|lost, restart|error while loading|processes|Xvnc$|xfce4-session|кадр:"
  echo
done
