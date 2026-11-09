#!/system/bin/sh
for p in /proc/[0-9]*; do
  c=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null)
  case "$c" in
    *Xvfb*|*x11vnc*|*xfce4*|*xfwm4*|*xfdesktop*|*dbus-daemon*|*palma-session*)
      pid="${p#/proc/}"
      [ "$pid" = "1" ] && continue
      kill -9 "$pid" 2>/dev/null
      echo "убит $pid $c"
      ;;
  esac
done
rm -f /data/data/ru.palma.app/cache/proot/.X*-lock /data/data/ru.palma.app/cache/proot/.X11-unix/X* /data/data/ru.palma.app/cache/proot/.l2s.*
echo "чистка завершена"
