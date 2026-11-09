#!/bin/sh
export DISPLAY=:0
ROOT=$(xwininfo -root 2>/dev/null)
RW=$(printf '%s\n' "$ROOT" | awk '/Width:/{print $2; exit}')
RH=$(printf '%s\n' "$ROOT" | awk '/Height:/{print $2; exit}')
[ -z "$RW" ] && { echo "SCREEN unknown"; exit 1; }
echo "SCREEN $RW $RH"
xwininfo -root -tree 2>/dev/null | grep -E '^ +0x' | while read -r line; do
  geo=$(printf '%s\n' "$line" | awk '{for(i=2;i<=NF;i++) if ($i ~ /^[0-9]+x[0-9]+\+[-0-9]+\+[-0-9]+$/) g=$i; print g}')
  [ -z "$geo" ] && continue
  name=$(printf '%s\n' "$line" | sed -n 's/^ *0x[0-9a-f]* "\([^"]*\)".*/\1/p')
  w=${geo%%x*}
  rest=${geo#*x}
  h=${rest%%+*}
  x=${rest#*+}
  x=${x%%+*}
  y=${rest##*+}
  [ "$w" -lt 120 ] 2>/dev/null && continue
  [ "$h" -lt 120 ] 2>/dev/null && continue
  right=$((x + w))
  bottom=$((y + h))
  verdict=OK
  [ "$right" -gt "$RW" ] && verdict=WIDE
  [ "$bottom" -gt "$RH" ] && verdict=TALL
  [ "$x" -lt 0 ] && verdict=LEFT
  [ "$y" -lt 0 ] && verdict=TOP
  echo "WIN $verdict $w x $h at $x $y [$name]"
done