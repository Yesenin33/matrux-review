#!/bin/bash
# Настройка русского языка, нижней панели и ярлыков рабочего стола в rootfs образа.
# Запуск: apply-ru.sh <каталог-rootfs> <alpine|debian|arch>
set -e
ROOTFS="${1:?нужен каталог rootfs}"
FAMILY="${2:?нужно семейство: alpine|debian|arch}"
HERE="$(cd "$(dirname "$0")" && pwd)"
QEMU=/usr/bin/qemu-aarch64

[ -d "$ROOTFS" ] || { echo "нет каталога $ROOTFS"; exit 1; }

cp -f "$QEMU" "$ROOTFS/usr/bin/qemu-aarch64" 2>/dev/null || true
cp -f /etc/resolv.conf "$ROOTFS/etc/resolv.conf" 2>/dev/null || true
for m in proc dev sys; do
  mountpoint -q "$ROOTFS/$m" || {
    mount -t ${m}fs $m "$ROOTFS/$m" 2>/dev/null || mount --bind /$m "$ROOTFS/$m" 2>/dev/null || true
  }
done

sh_() { chroot "$ROOTFS" /usr/bin/qemu-aarch64 /bin/sh -c "$1"; }

echo "== локаль, раскладка, шрифты ($FAMILY)"
case "$FAMILY" in
  alpine)
    for p in musl-locales setxkbmap xkeyboard-config ttf-dejavu font-noto; do
      sh_ "apk add --no-cache $p >/dev/null 2>&1" || true
    done
    for p in libxfce4ui-lang libxfce4util-lang xfce4-panel-lang xfce4-session-lang xfce4-settings-lang xfwm4-lang thunar-lang xfce4-terminal-lang xfce4-appfinder-lang mousepad-lang exo-lang garcon-lang; do
      sh_ "apk add --no-cache $p >/dev/null 2>&1" || true
    done
    ;;
  debian|ubuntu)
    sh_ 'DEBIAN_FRONTEND=noninteractive apt-get install -y -qq locales x11-xkb-utils xkb-data fonts-dejavu-core 2>&1 | tail -2'
    sh_ 'sed -i "s/^# *ru_RU.UTF-8/ru_RU.UTF-8/" /etc/locale.gen; grep -q "^ru_RU.UTF-8" /etc/locale.gen || echo "ru_RU.UTF-8 UTF-8" >> /etc/locale.gen; locale-gen 2>&1 | tail -2'
    ;;
  arch)
    sh_ 'pacman -Sy --noconfirm --needed xorg-setxkbmap xkeyboard-config ttf-dejavu 2>&1 | tail -2'
    sh_ 'sed -i "s/^# *ru_RU.UTF-8/ru_RU.UTF-8/" /etc/locale.gen; grep -q "^ru_RU.UTF-8" /etc/locale.gen || echo "ru_RU.UTF-8 UTF-8" >> /etc/locale.gen; locale-gen 2>&1 | tail -2'
    ;;
  *) echo "неизвестное семейство $FAMILY"; exit 1 ;;
esac

echo "== экранная клавиатура"
case "$FAMILY" in
  alpine)
    sh_ 'apk add --no-cache onboard >/dev/null 2>&1 || apk add --no-cache xvkbd >/dev/null 2>&1 || true'
    ;;
  debian|ubuntu)
    sh_ 'DEBIAN_FRONTEND=noninteractive apt-get install -y -qq onboard >/dev/null 2>&1 || DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvkbd >/dev/null 2>&1 || true'
    ;;
  arch)
    sh_ 'pacman -S --noconfirm --needed onboard >/dev/null 2>&1 || pacman -S --noconfirm --needed xvkbd >/dev/null 2>&1 || true'
    ;;
esac
OSK=""
for b in onboard xvkbd; do
  if [ -x "$ROOTFS/usr/bin/$b" ]; then OSK="$b"; break; fi
done
echo "  экранная клавиатура: ${OSK:-НЕ установлена}"

echo "== часовой пояс и локаль по умолчанию"
sh_ 'ln -sf /usr/share/zoneinfo/Europe/Moscow /etc/localtime; echo Europe/Moscow > /etc/timezone'
printf 'LANG=ru_RU.UTF-8\nLC_MESSAGES=ru_RU.UTF-8\nLC_TIME=ru_RU.UTF-8\n' > "$ROOTFS/etc/environment"

echo "== панель и рабочий стол"
CONF="$ROOTFS/root/.config/xfce4/xfconf/xfce-perchannel-xml"
mkdir -p "$CONF" "$ROOTFS/root/Desktop" "$ROOTFS/usr/share/applications" "$ROOTFS/root/.config/autostart"
cp -f "$HERE/xfce4-panel.xml" "$CONF/xfce4-panel.xml"
cp -f "$HERE/xfce4-desktop.xml" "$CONF/xfce4-desktop.xml"
mkdir -p "$ROOTFS/etc/palma"
cp -f "$HERE/xfce4-panel.xml" "$ROOTFS/etc/palma/xfce4-panel.xml"
cp -f "$HERE/xfce4-desktop.xml" "$ROOTFS/etc/palma/xfce4-desktop.xml"
echo "  панель: сверху во всю ширину, шаблон в /etc/palma"

app() { # app <файл> <Exec> <Имя> <Иконка> <Категории> <бинарники...>
  local file="$1" exec="$2" name="$3" icon="$4" cats="$5"; shift 5
  for b in "$@"; do [ -x "$ROOTFS/usr/bin/$b" ] || return 0; done
  cat > "$ROOTFS/usr/share/applications/$file.desktop" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=$name
Exec=$exec
Icon=$icon
Terminal=false
Categories=$cats
EOF
  cp -f "$ROOTFS/usr/share/applications/$file.desktop" "$ROOTFS/root/Desktop/$name.desktop"
  chmod 755 "$ROOTFS/root/Desktop/$name.desktop"
  echo "  ярлык: $name"
}

app palma-terminal "xfce4-terminal" "Терминал" "utilities-terminal" "System;TerminalEmulator;" xfce4-terminal
app palma-files "thunar" "Файлы" "system-file-manager" "System;FileManager;" thunar
app palma-browser "firefox" "Интернет" "firefox" "Network;WebBrowser;" firefox firefox-esr
app palma-editor "mousepad" "Текстовый редактор" "accessories-text-editor" "Utility;TextEditor;" mousepad
app palma-settings "xfce4-settings-manager" "Настройки" "preferences-system" "Settings;" xfce4-settings-manager
app palma-taskmanager "xfce4-taskmanager" "Диспетчер задач" "utilities-system-monitor" "System;" xfce4-taskmanager

if [ -n "$OSK" ]; then
  case "$OSK" in
    onboard) OSK_EXEC="onboard";;
    xvkbd) OSK_EXEC="xvkbd -geometry 720x240";;
  esac
  cat > "$ROOTFS/usr/share/applications/palma-keyboard.desktop" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=Клавиатура
Exec=$OSK_EXEC
Icon=input-keyboard
Terminal=false
Categories=Utility;Accessibility;
EOF
  cp -f "$ROOTFS/usr/share/applications/palma-keyboard.desktop" "$ROOTFS/root/Desktop/Клавиатура.desktop"
  chmod 755 "$ROOTFS/root/Desktop/Клавиатура.desktop"
  echo "  ярлык: Клавиатура"
else
  python3 - "$CONF/xfce4-panel.xml" <<'PY'
import sys, re
path = sys.argv[1]
text = open(path, encoding="utf-8").read()
text = re.sub(r'<property name="plugin-4" type="string" value="launcher">.*?</property>\s*', "", text, flags=re.S)
text = text.replace('        <value type="int" value="4"/>\n', "")
open(path, "w", encoding="utf-8").write(text)
print("  кнопка клавиатуры убрана из панели: экранной клавиатуры нет")
PY
fi

echo "== горячие клавиши"
if [ -n "$OSK" ]; then HOTKEY_KEYBOARD="    <property name=\"&lt;Super&gt;k\" type=\"string\" value=\"$OSK\"/>\n"; else HOTKEY_KEYBOARD=""; fi
{
  printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?>'
  printf '%s\n' '<channel name="xfce4-keyboard-shortcuts" version="1.0">'
  printf '%s\n' '  <property name="commands" type="empty">'
  printf '%s\n' '    <property name="custom" type="empty">'
  printf '%s\n' '      <property name="&lt;Super&gt;t" type="string" value="xfce4-terminal"/>'
  printf '%s\n' '      <property name="&lt;Super&gt;e" type="string" value="thunar"/>'
  printf '%s\n' '      <property name="&lt;Super&gt;s" type="string" value="xfce4-settings-manager"/>'
  printf '%s\n' '      <property name="&lt;Super&gt;q" type="string" value="xfce4-session-logout"/>'
  printf '%b' "$HOTKEY_KEYBOARD"
  printf '%s\n' '    </property>'
  printf '%s\n' '  </property>'
  printf '%s\n' '</channel>'
} > "$CONF/xfce4-keyboard-shortcuts.xml"
python3 -c "import xml.etree.ElementTree as ET,sys; ET.parse('$CONF/xfce4-keyboard-shortcuts.xml')" && echo "  горячие клавиши записаны"

echo "== раскладка клавиатуры"
cat > "$ROOTFS/root/.config/autostart/palma-keyboard.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Раскладка клавиатуры
Comment=Русская и английская раскладки, переключение Alt+Shift
Exec=setxkbmap -layout us,ru -option grp:alt_shift_toggle -option grp_led:scroll
Terminal=false
NoDisplay=true
EOF

for f in "$ROOTFS"/root/Desktop/*.desktop; do
  [ -f "$f" ] || continue
  chroot "$ROOTFS" /usr/bin/qemu-aarch64 /bin/sh -c "command -v gio >/dev/null 2>&1 && gio set '$f' metadata::trusted true" 2>/dev/null || true
done

echo "== итог"
echo "  ярлыков на столе: $(ls -1 "$ROOTFS"/root/Desktop/*.desktop 2>/dev/null | wc -l)"
echo "  переводов ru: $(ls -1 "$ROOTFS"/usr/share/locale/ru/LC_MESSAGES/*.mo 2>/dev/null | wc -l)"
echo "  setxkbmap: $([ -x "$ROOTFS/usr/bin/setxkbmap" ] && echo есть || echo НЕТ)"
echo "  раскладка ru: $([ -f "$ROOTFS/usr/share/X11/xkb/symbols/ru" ] && echo есть || echo НЕТ)"

rm -f "$ROOTFS/usr/bin/qemu-aarch64"
for m in proc dev sys; do umount "$ROOTFS/$m" 2>/dev/null || true; done
echo "готово"
