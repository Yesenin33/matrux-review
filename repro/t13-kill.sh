#!/bin/sh
# kill guest processes of the debian13 test session (never matches itself: cmdline is just this path)
SELF=$$
for p in /proc/[0-9]*; do
  pid=${p#/proc/}
  [ "$pid" = "$SELF" ] && continue
  [ -r "$p/cmdline" ] || continue
  c=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null) || continue
  case "$c" in
    /usr/bin/qemu-aarch64*Xvfb*|/usr/bin/qemu-aarch64*x11vnc*|/usr/bin/qemu-aarch64*xfce4-session*|/usr/bin/qemu-aarch64*xfwm4*|/usr/bin/qemu-aarch64*xfce4-panel*|/usr/bin/qemu-aarch64*xfdesktop*|/usr/bin/qemu-aarch64*dbus-daemon*|/usr/bin/qemu-aarch64*onboard*|/usr/bin/qemu-aarch64*xfconfd*|/usr/bin/qemu-aarch64*dbus-run-session*)
      kill -9 "$pid" 2>/dev/null ;;
  esac
done
sleep 1
echo "left: $(ps -eo args | grep -c '^/usr/bin/qemu-aarch64')"
