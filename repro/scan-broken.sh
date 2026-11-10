#!/system/bin/sh
# Ищет битые симлинки в usr/lib системы. Аргумент: имя системы.
d="$1"
cd "files/systems/$d/usr/lib" 2>/dev/null || { echo "нет каталога"; exit 1; }
n=0
for f in *; do
  if [ -L "$f" ] && [ ! -e "$f" ]; then echo "БИТЫЙ: $f -> $(readlink "$f")"; n=$((n+1)); fi
done
echo "битых: $n"
