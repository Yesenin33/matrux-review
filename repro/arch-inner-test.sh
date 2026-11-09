id
echo "HOME=$HOME TMPDIR=$TMPDIR XDG_CACHE_HOME=$XDG_CACHE_HOME"
P=/usr/share/icons/Adwaita/16x16/places/folder-documents.png
echo "-- иконка PNG: $(ls -la $P 2>&1)"
gdk-pixbuf-pixdata "$P" /tmp/p1 >/dev/null 2>/tmp/e1 && echo PNG_OK || { echo PNG_FAIL; head -3 /tmp/e1; }
S=$(ls /usr/share/icons/Adwaita/scalable/places/*.svg 2>/dev/null | head -1)
echo "-- иконка SVG: $S"
gdk-pixbuf-pixdata "$S" /tmp/p2 >/dev/null 2>/tmp/e2 && echo SVG_OK || { echo SVG_FAIL; head -3 /tmp/e2; }
echo "-- каталог загрузчиков:"
ls /usr/lib/gdk-pixbuf-2.0/2.10.0/ 2>&1 | head -3
echo "-- mime.cache: $(ls -la /usr/share/mime/mime.cache 2>&1)"
echo "-- кэш темы Adwaita: $(ls -la /usr/share/icons/Adwaita/icon-theme.cache 2>&1)"
echo "-- кэш в /tmp/.cache:"
ls -la /tmp/.cache 2>&1 | head -6
echo "-- тест GTK-темы через gtk-launch-иконку:"
python3 - <<'PY' 2>&1 | tail -6 || echo "python3/gi нет"
import gi
gi.require_version('Gtk','3.0')
from gi.repository import Gtk
t=Gtk.IconTheme.get_default()
print('тема:', t.get_example_icon_name(), t.get_theme_name())
pi,_,_=t.lookup_icon('folder-documents',16,0)
print('найдена:', pi.get_filename() if pi else 'НЕТ')
PY
