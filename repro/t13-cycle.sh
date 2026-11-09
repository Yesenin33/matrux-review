#!/bin/sh
# usage: t13-cycle.sh <position-string> <out.png>
set -e
POS="$1"
OUT="$2"
R=/root/palma/image-build/debian13-rootfs
XML=$R/root/.config/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml

for p in /proc/[0-9]*; do
  [ -r "$p/cmdline" ] || continue
  c=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null)
  case "$c" in
    *qemu-aarch64*Xvfb*|*qemu-aarch64*x11vnc*|*qemu-aarch64*xfce4-session*|*qemu-aarch64*xfwm4*|*qemu-aarch64*xfce4-panel*|*qemu-aarch64*xfdesktop*|*qemu-aarch64*dbus-daemon*|*qemu-aarch64*onboard*)
      kill -9 "${p#/proc/}" 2>/dev/null || true ;;
  esac
done
sleep 2
rm -f "$R"/tmp/.X*-lock "$R"/tmp/palma-session.log
rm -rf "$R"/tmp/.X11-unix
mkdir -p "$R"/tmp/.X11-unix
chmod 1777 "$R"/tmp/.X11-unix

sed -i "s|<property name=\"position\" type=\"string\" value=\"[^\"]*\"/>|<property name=\"position\" type=\"string\" value=\"$POS\"/>|" "$XML"
grep -m1 'name="position"' "$XML"

env -u TMPDIR chroot "$R" /usr/bin/qemu-aarch64 /bin/sh -c 'export PALMA_GEOMETRY=720x1600 PALMA_VNC_PORT=5902; exec /usr/local/bin/palma-session vnc' > /root/palma/repro/t13-run.log 2>&1 &
i=0
while [ $i -lt 90 ]; do
  grep -q 'desktop up' "$R/tmp/palma-session.log" 2>/dev/null && break
  i=$((i+1)); sleep 2
done
sleep 6
python3 /root/palma/repro/vnc_shot.py "$OUT" 5902 | tail -3
