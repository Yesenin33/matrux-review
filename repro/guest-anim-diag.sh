#!/bin/sh
export DISPLAY=:0
echo "xsetroot: $(command -v xsetroot 2>/dev/null || echo НЕТ)"
echo "xwininfo: $(command -v xwininfo 2>/dev/null || echo НЕТ)"
echo "awk hex: $(awk 'BEGIN{srand();printf "%02x%02x%02x",rand()*255,rand()*255,rand()*255}' 2>&1)"
echo "дробный sleep: $(sleep 0.05 2>&1 && echo ок || echo нет)"
echo "DISPLAY=$DISPLAY"
xsetroot -solid "#ff0000" 2>&1; echo "xsetroot rc=$?"
xwininfo -root 2>&1 | sed -n '1,6p'
echo "процессы X: $(ps -A 2>/dev/null | grep -c Xvnc)"
