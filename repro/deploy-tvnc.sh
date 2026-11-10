#!/bin/bash
set -u
H="root@shjwb-57-128-216-98.run.pinggy-free.link"
SSH="ssh -S /tmp/gol.sock -o BatchMode=yes $H"
SYS="$1"
TAR="$2"
$SSH "adb push $TAR /data/local/tmp/tvnc.tar >/dev/null 2>&1
  adb shell \"run-as ru.palma.app sh -c 'cd files/systems/$SYS && tar -xf /data/local/tmp/tvnc.tar'\"
  echo '--- результат ---'
  adb shell \"run-as ru.palma.app ls -la files/systems/$SYS/usr/bin/Xvnc\" 2>&1 | tr -d '\r'
  adb shell \"run-as ru.palma.app sh -c 'ls -la files/systems/$SYS/usr/lib/xorg/modules/extensions/libvnc.so files/systems/$SYS/etc/alternatives/Xvnc 2>&1'\" 2>&1 | tr -d '\r'
" 2>&1 | grep -v Warning
