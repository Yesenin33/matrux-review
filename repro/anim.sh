#!/bin/sh
# Анимация реальными окнами: 15 появлений/закрытий крупного окна (~15 с).
export DISPLAY=:0
i=0
n="${1:-15}"
while [ "$i" -lt "$n" ]; do
  xmessage -timeout 1 -geometry 600x400 "проверка $i" >/dev/null 2>&1
  i=$((i+1))
done
echo "окон открыто: $i"
