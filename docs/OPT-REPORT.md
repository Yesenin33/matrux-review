# Отчёт по правкам из AUDIT.md

Версия: `palma.versionName=0.1.35`, `palma.versionCode=36` (было 0.1.34 / 35).
Изменены только разрешённые файлы: `VncClient.kt`, `SessionController.kt`, `DesktopScreen.kt`, `MainActivity.kt`, `AppGraph.kt`, `image-build/palma-session`, плюс `gradle.properties` (версия) и этот отчёт.

Аудит писался по более старой версии кода: номера строк в нём не совпадают, часть правок к началу работы уже была в коде. Ниже везде указаны строки текущего кода.

## Десять главных правок, по порядку

| # | Пункт | Где | Что сделано |
|---|---|---|---|
| 1 | 4.2 | `palma-session:128`, `:180` | Уже было в коде: у x11vnc стоит `-localhost`, у Xvnc `-localhost=1`. На стенде x11vnc слушает только `127.0.0.1:5995`. |
| 2 | 4.1 | `palma-session:153-166`, `:189-207`, `:217-237` | Сделано, но не так, как в аудите (см. ниже). Xvfb стартует квадратом `MAXW×MAXH`, размер меняется через `xrandr --fb`, программы не закрываются. Проверено на стенде. |
| 3 | 1.3 + 3.1 | `VncClient.kt:20`, `SessionController.kt:468-485`, `DesktopScreen.kt:337-372`, `MainActivity.kt:168`, `:425` | Сделано. Кадр копируется в Bitmap на потоке pump; UI читает `StateFlow<Frame?>` только внутри `drawBehind`; `frame` и `frameBitmap()` убраны из `PalmaApp`; `DesktopActions` создаётся один раз через `remember(graph)` (`MainActivity.kt:383`). |
| 4 | 2.2 + 3.2 | `SessionController.kt:49-52`, `:235-271`, `DesktopScreen.kt:293-335`, `MainActivity.kt:399` | Сделано. Весь ввод идёт через одну очередь `Channel<Input>(256)`, подряд идущие движения с одними кнопками склеиваются. В режиме «Касание» нажатие уходит в гостя только после сдвига больше `TAP_SLOP`, иначе отправляется `tap()` в точке нажатия. |
| 5 | 2.3 | `SessionController.kt:141-155` | Сделано. Дебаунс 400 мс, одна отменяемая `geometryJob`. Убраны кулдаун 12 с, `reconnectDisplay()`, `frames.value = null` и `Log.i`. `GEOMETRY_WAIT_MS` поднят до 30 с: при запасной пересборке стола 20 с не хватает. |
| 6 | 1.1 | `VncClient.kt:44-57`, `:213-228` | **1.1 отклонена.** Причина и доказательство — ниже. |
| 6 | 1.2 | `VncClient.kt:37-57` | Уже было в коде: строка буфера переиспользуется (`rawRow`/`packRow`), запись идёт одним `put` на строку. Цикл перестановки байт оставлен, потому что `SetPixelFormat` не меняется (см. 1.1). |
| 7 | 2.1 | `SessionController.kt:47`, `:472` | Сделано: `data class Frame(bitmap, seq)`, номер берётся из `AtomicLong`. |
| 8 | 3.7 | `DesktopScreen.kt:399-414`, `:494-495` | Сделано, с отступлением от аудита. В поле всегда лежат 4 символа `U+200B`. Backspace считается по тому, сколько из них пропало. Пока у IME идёт композиция, ничего не отправляется, слово уходит при фиксации. `KeyboardType.Password` не включён, потому что на части клавиатур он отключает русскую раскладку. **Ждёт проверки на телефоне.** |
| 8 | 4.3 | `palma-session:168-173`, `:200`, `:291` | Сделано. `prepare_x` (setxkbmap + xrdb) вызывается после каждого `start_display`, в том числе после перезапуска упавшего Xvfb. `.Xresources` пишется один раз до цикла. |
| 9 | 1.4 | `SessionController.kt:475-485`, `:494` | Сделано частично. На сессию два Bitmap, при смене размера они перенастраиваются через `reconfigure`. Поворот 900×2000 ↔ 2000×900 укладывается в тот же объём, новых выделений нет. Direct-буфер `Framebuffer` по-прежнему создаётся заново на каждый DesktopSize (`VncClient.kt:169-174`), это осталось открытым. |
| 9 | 1.5 | `VncClient.kt:186-188` | `grow()` в коде уже не было. Raw-прямоугольник за границей дочитывается и отбрасывается через `skipFully`. |
| 10 | 2.5 | `AppGraph.kt:63`, `SessionController.kt:157-162`, `:388`, `:405`, `:525-526` | Сделано. `guestTmp = cacheDir/proot`. Логи: `trace.log`, `xvfb-0.log`, `xvfb-1.log`, `x11vnc.log`, `xfce.log`. `geometry-request` пишется через `.part` и `renameTo` (это заодно пункт 2.4). |
| 10 | 1.9 | `VncClient.kt:265`, `:271`, `:124` | Сделано. `soTimeout = 5000` на рукопожатие, после `requestUpdate(false)` сбрасывается в 0. |

### Почему 1.1 отклонена

Аудит утверждает, что R и B перепутаны. В текущем коде это не так. С `SetPixelFormat` (сдвиги R=16, G=8, B=0, little-endian) x11vnc шлёт байты `B G R X`. `Framebuffer.writeRow` (`VncClient.kt:44-57`) переставляет их в `R G B FF`, а именно такой порядок ждёт `Bitmap.copyPixelsFromBuffer` для `ARGB_8888`.

Проверено командой на настоящем x11vnc и на байткоде `VncClient` из сборки. Корень Xvfb залит `#FF8000`, в буфере `Framebuffer` лежит:

```
first 2000x2000 pixel(10,10) in memory R G B A = FF 80 00 FF
resized 2000x900 same socket, 1555 ms after first frame, pixel(10,10) = FF 80 00 FF
```

`FF 80 00 FF` — это R=FF, G=80, B=00, A=FF, то есть оранжевый. Это совпадает со скриншотами с телефона. Если сделать так, как предлагает аудит (сдвиги 0/8/16 и копирование строки без перестановки), четвёртый байт станет `00` вместо `FF`. Тогда кадр будет непрозрачным только при условии, что HWUI честно учитывает `setHasAlpha(false)`, а это можно проверить только на телефоне. Поэтому `SetPixelFormat` и копирование строки не тронуты.

### Как на деле сделана 4.1

Если делать как в аудите, поворот не работает. На стенде выяснилось:

1. На Xvfb 21.1 `xrandr --fb` меняет размер, только если к дисплею уже подключён клиент, который слушает события RandR. x11vnc с `-xrandr` как раз такой клиент. Без него ответ `BadValue (RRSetScreenSize)`, размер не меняется.
2. Код выхода `xrandr` врёт: при первом уменьшении он возвращает `1`, хотя размер поменялся.
3. Старая строка `xrandr --fb` после цикла запуска не делала ничего: она выполнялась до того, как с дисплеем что-то связывалось.

Поэтому:

- `resize_fb` (`:157-166`) вызывает `xrandr --fb` до 15 раз с паузой 0,2 с и проверяет фактический размер через `xrandr -q` (`fb_size`, `:153-155`).
- `start_display` (`:189-207`) поднимает Xvfb квадратом `MAXW×MAXH`, затем запускает x11vnc и только потом уменьшает экран до `GW×GH`.
- Если RandR не сработал, ставится `NORANDR=1`, Xvfb перезапускается в точном размере, а дальнейшие повороты идут старой пересборкой. Поведение тогда такое же, как до правок.
- В `apply_geometry` (`:217-237`) сначала пробуется `resize_fb`. Если не вышло или новый размер больше квадрата, стол пересобирается и сразу поднимается `start_desktop`.

Версии на стенде: Xvfb 21.1.22, x11vnc 0.9.17. В образах: Xvfb 21.1.7 (debian), 21.1.16 (debian13), 21.1.12 (ubuntu), 21.1.25 (arch); x11vnc 0.9.16–0.9.17. `xrandr` есть во всех четырёх rootfs. Каталоги rootfs только читались.

Цена: Xvfb для `2000x2000` занимает 2000 × 2000 × 4 Б = 16,0 МБ вместо 7,2 МБ.

На стенде также видно, что первый клиент может получить один кадр 2000×2000 до того, как x11vnc обработает событие RandR. Следом сразу приходит DesktopSize, и клиент его применяет.

## Остальные пункты аудита

| Пункт | Где | Статус |
|---|---|---|
| 1.6 | `VncClient.kt:169-174` | Уже было: `resize()` ничего не копирует. |
| 1.7 | `VncClient.kt:59-73` | Уже было: строка-буфер переиспользуется, копирование массовое, границы проверяются. |
| 1.8 | `VncClient.kt:201-214`, `:116-118`, `:139`, `:144-145` | Сделано: `skipFully(Long)`; длина ServerCutText читается беззнаково; длина имени ограничена 0..4096 (`bad_name`). |
| 1.10 | `VncClient.kt:81-83` | Сделано: `@Volatile current` и геттер `framebuffer`. `pointer()` и `requestUpdate()` читают его один раз в локальную переменную. |
| 1.11 | `VncClient.kt:190-197` | Мёртвая ветка `-308` удалена. Псевдокодировка Cursor (-239) не сделана, это открытый пункт. |
| 1.12 | `SessionController.kt:422` | Сделано: `blank()` считается на первом кадре соединения и пока кадр пустой. |
| 2.4 | `SessionController.kt:157-162` | Сделано: запись через `.part` и `renameTo`. |
| 2.6 | `SessionController.kt:343-358` | Сделано: хранится хвост вывода, при падении движка ждём 200 мс и берём `takeLast`. Второго чтения потока больше нет. |
| 2.7 | `SessionController.kt:83-90` | `@Volatile` поставлен на `process`, `socket`, `vnc`, `startedGeometry`, `restarting` и на курсорные поля. Перенос `teardown()` на Main не делался: вместо него курсорные поля тоже `@Volatile`. |
| 2.8 | `SessionController.kt:137` | `killLeftovers()` вынесен на `Dispatchers.IO`. Добавить `Xvnc` и `dbus-daemon` в `NAMES` нельзя: `GuestProcesses.kt` не входит в разрешённые файлы. Открыто. |
| 2.9 | `SessionController.kt:434` | Сделано: при обрыве `blankState = true`, через `STUCK_MS` появится экран «Завис» с отчётом. |
| 2.10 | — | Не делалось: `-xkb` и печать кириллицы без переключения группы нужно проверять на образе в телефоне. |
| 3.3 | `DesktopScreen.kt:251-291` | Сделано: жест мыши завершается, только когда подняты все пальцы. Общий класс `MouseGesture` используется и в `Stage`, и в `Trackpad`. |
| 3.4 | `DesktopScreen.kt:252` | Сделано: `drag` начинается с `0f`. |
| 3.5 | `DesktopScreen.kt:271-277` | Сделано: дробный остаток движения курсора накапливается. |
| 3.6 | `DesktopScreen.kt:232`, `:339` | Сделано: `rememberUpdatedState(actions)`. Ключ `pointerInput` у `Stage` теперь только `input`, так что поворот жест не перезапускает. |
| 3.8 | — | Не делалось: нужна таблица keyCode → keysym в `Keys` и проверка с Bluetooth-клавиатурой. Открыто. |
| 4.4 | `palma-session:116-124` | **Не применено.** С Android 10 SELinux закрывает приложениям `/proc/net/tcp`, а в proot `/proc` взят с хоста. Если файл не читается, новый `wait_port` всегда вернёт 1, и супервизор выйдет с «no listener». Ждёт проверки на телефоне. |
| 4.5 | `palma-session:140-151` | Сделано: сначала `kill -TERM` для `dbus-run-session` и пауза 0,5 с, в список добавлены `dbus-daemon\|dbus-run-session`. |
| 4.6 | `palma-session:339`, `:371`, `:391-396` | Сделано: `XRESTARTS` (лимит 4) и `DRESTARTS` (лимит 40), оба обнуляются после 300 с стабильной работы стола. |
| 4.7 | `palma-session:400` | Сделано: файл удаляется сразу после чтения. |
| 4.8 | `palma-session:416-421` | Сделано: цикл ждёт до 2 с шагами по 0,2 с и выходит сразу, как только появляется `geometry-request`. |
| 4.9 | `palma-session:88-94` | **Не применено.** Проверка по `/proc/$p/comm` зависит от того, что показывает `comm` у процесса под proot (может оказаться имя загрузчика). Тогда остатки прошлой сессии перестанут убиваться. Ждёт проверки на телефоне. |
| 4.10 | `palma-session:128` | Не применено: `-noshm` оставлен, замер FPS — только на телефоне. Опции `-xd` в текущей строке нет. |
| 4.11 | `palma-session:1-3`, `:37-39` | Сделано: `say` пишет одну строку, удалены `LOG=`, `try_vnc`, `tune_panel` и пустые строки. Ветка Xvnc оставлена: она недостижима, но безопасна (`-localhost=1`). |

## Проверки

**Сборка.** `ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug --offline`:
```
BUILD SUCCESSFUL in 6s
package: name='ru.palma.app' versionCode='36' versionName='0.1.35'
```

**Комментарии.** `grep` по изменённым файлам и по добавленным строкам diff ничего не нашёл (rc=1 во всех трёх проверках):
```
== kotlin: // or /* in changed files
rc=1
== shell: # outside shebang
rc=1
== added lines with comment markers
rc=1
```

**Синтаксис супервизора.** `sh -n`, `dash -n` и `busybox sh -n` проходят.

**Юнит-тесты.** `./gradlew testDebugUnitTest` не компилируется. Это было и до правок, ошибки те же самые:
```
e: .../shell/LaunchSequenceTest.kt:57:68 No value passed for parameter 'port'
e: .../shell/VncClientTest.kt:82:9 Unresolved reference ... set
```
Оба файла вне разрешённого списка, поэтому не правились. Кроме того, `VncClientTest` устарел по смыслу: `handshake38AndRawUpdate` ждёт старую упаковку `0xFFRRGGBB` в `IntBuffer`, а `outOfBoundsRectIsRejected` ждёт исключение, хотя код (ещё до правок) такой прямоугольник пропускает. Вместо юнит-тестов работу проверяли два стенда ниже.

**Стенд 1: настоящие функции `palma-session` + x11vnc + `VncClient` из сборки.** Запуск в отдельных PID- и mount-неймспейсах:
```
matrux 19:08:17: framebuffer 900x2000 в экране 2000x2000
after start_display: fb=900x2000 MAX=2000x2000 NORANDR=no
x11vnc listen: 127.0.0.1:5995
matrux 19:08:19: geometry 2000x900 через xrandr
apply_geometry took 19 ms, fb=2000x900
first 2000x2000 pixel(10,10) in memory R G B A = FF 80 00 FF
resized 2000x900 same socket, 1555 ms after first frame, pixel(10,10) = FF 80 00 FF
X client after rotate: alive
Xvfb pid same: yes
```

**Стенд 2: запасной путь, `xrandr` подменён заглушкой с отказом:**
```
matrux 19:08:24: xrandr не меняет размер Xvfb, запуск в 900x2000
matrux 19:08:25: framebuffer 900x2000 в экране 900x2000
NORANDR=1
  dimensions:    900x2000 pixels (229x508 millimeters)
matrux 19:08:26: geometry 2000x900, xrandr не сработал, пересобираю рабочий стол
matrux 19:08:27: framebuffer 2000x900 в экране 2000x900
  dimensions:    2000x900 pixels (508x229 millimeters)
```

**Происшествие на хосте.** Первый прогон стенда 2 шёл только в mount-неймспейсе, без своего PID-неймспейса. `kill_desktop` убивает процессы по имени через `/proc` и в таком режиме видит процессы хоста. В 19:06:52 системный `dbus.service` на хосте получил SIGKILL, и systemd сразу его перезапустил (`active`). Мой `kill_desktop` отработал раньше, в 19:05:22, а в 19:06:4x `dbus-daemon` 928 ещё был жив. Поэтому связь с моим прогоном не доказана, но и не исключена. Все повторные прогоны и команды ниже идут через `unshare -pfm --mount-proc`. На телефоне такой проблемы нет: приложение может убивать только процессы своего uid.

**Память.** Рабочий набор кадра при 900×2000: direct-буфер 7,2 МБ + два Bitmap по 7,2 МБ = 21,6 МБ (было 14,4 МБ и ещё +14,4 МБ мусора на каждый поворот). Второй Bitmap нужен, чтобы pump не писал в Bitmap, который в этот момент рисуется.

## Ждёт проверки на телефоне

Пакет `ru.palma.app`, гостевой `/tmp` = `/data/data/ru.palma.app/cache/proot`.

1. **4.1, поворот живьём.** Открыть терминал в госте, повернуть телефон туда и обратно. Терминал должен остаться открытым, а в логе должна быть строка «через xrandr»:
   ```
   adb shell run-as ru.palma.app grep -E "через xrandr|xrandr не|пересобираю" cache/proot/trace.log
   adb logcat -s palma-vnc
   ```
   Если там «xrandr не меняет размер», значит под proot RandR не работает, и повороты идут старой пересборкой.
2. **1.3 + 3.1, плавность.** Прокрутка в Thunar или в терминале, до и после правок:
   ```
   adb shell dumpsys gfxinfo ru.palma.app reset
   adb shell dumpsys gfxinfo ru.palma.app | grep -E "Total frames|Janky frames|90th"
   ```
3. **2.2 + 3.2, ввод касанием.** Тап по иконке не должен её сдвигать, перетаскивание окна не должно залипать. Долгое нажатие без сдвига сейчас превращается в тап: порог `LONG_PRESS_MS` из аудита не сделан.
4. **3.7, клавиатура.** На Gboard и Яндекс Клавиатуре с подсказками напечатать «привет», стереть два символа Backspace'ом, проверить, нет ли дублей. Проверить, что русская раскладка переключается.
5. **4.3, раскладка после поворота.** После поворота напечатать «съешь ещё»:
   ```
   adb shell run-as ru.palma.app grep framebuffer cache/proot/trace.log
   ```
6. **4.4, доступ к `/proc/net/tcp`:**
   ```
   adb shell run-as ru.palma.app cat /proc/net/tcp | head -3
   ```
   Если выдаёт таблицу, `wait_port` из аудита можно применить.
7. **4.9, `comm` процессов под proot:**
   ```
   adb shell run-as ru.palma.app sh -c 'for p in $(cat cache/proot/palma-pids); do echo "$p $(cat /proc/$p/comm)"; done'
   ```
   Если видны `Xvfb`, `x11vnc`, `xfwm4`, фильтр из аудита можно применить.
8. **4.10, `-noshm`.** Убрать ключ в копии скрипта и проверить:
   ```
   adb shell run-as ru.palma.app grep -iE "shm|XShm" cache/proot/x11vnc.log
   ```
   Затем замерить FPS прокрутки, как в пункте 2.

## Что осталось незакрытым

- Юнит-тесты не компилируются (`LaunchSequenceTest.kt:57`, `VncClientTest.kt:82`), а `VncClientTest` ждёт устаревший порядок байт. Файлы вне разрешённого списка.
- 1.4: direct-буфер `Framebuffer` пересоздаётся на каждый DesktopSize.
- 1.11: нет псевдокодировки Cursor, x11vnc по-прежнему рисует курсор в кадре.
- 2.8: `Xvnc` и `dbus-daemon` не добавлены в `GuestProcesses.NAMES` (файл не разрешён).
- 2.10, 3.8: раскладка через `-xkb` и физическая клавиатура.
- 3.2: долгое нажатие (`LONG_PRESS_MS`) без сдвига.
- 4.4, 4.9, 4.10: ждут данных с телефона (см. выше).

## Команды для повторной проверки

```sh
cd /root/palma
ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug --offline 2>&1 | grep BUILD
$(ls /opt/android-sdk/build-tools/*/aapt | tail -1) dump badging app/build/outputs/apk/debug/app-debug.apk | head -1

F="app/src/main/java/ru/palma/app/shell/VncClient.kt app/src/main/java/ru/palma/app/session/SessionController.kt app/src/main/java/ru/palma/app/ui/screens/DesktopScreen.kt app/src/main/java/ru/palma/app/MainActivity.kt app/src/main/java/ru/palma/app/AppGraph.kt"
grep -nE '(^|[^:])//|/\*' $F; echo "kotlin rc=$?"
grep -nE '^\s*#|\s#\s' image-build/palma-session | grep -v '^1:#!'; echo "shell rc=$?"
sh -n image-build/palma-session && dash -n image-build/palma-session && busybox sh -n image-build/palma-session && echo SYNTAX_OK

ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest --offline 2>&1 | grep -E "^e:|BUILD"
```

Стенды (нужны `Xvfb`, `x11vnc`, `xrandr`, `xsetroot`, `xprop`, `xdpyinfo`, `javac`, root для `unshare`):

```sh
mkdir -p /var/tmp/vncproof/shim && cd /var/tmp/vncproof
cat > Proof.java <<'EOF'
import ru.palma.app.shell.Framebuffer;
import ru.palma.app.shell.VncClient;
import java.nio.ByteBuffer;

public class Proof {
    static String px(Framebuffer fb, int x, int y) {
        ByteBuffer b = fb.getBuffer();
        int o = (y * fb.getWidth() + x) * 4;
        return String.format("%02X %02X %02X %02X", b.get(o) & 255, b.get(o + 1) & 255, b.get(o + 2) & 255, b.get(o + 3) & 255);
    }
    static Framebuffer settle(VncClient v, String size, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (v.readMessage()) {
                Framebuffer fb = v.getFramebuffer();
                if ((size == null || size.equals(fb.getWidth() + "x" + fb.getHeight())) && (fb.getBuffer().get((10 * fb.getWidth() + 10) * 4 + 3) & 255) == 255) return fb;
            }
        }
        return null;
    }
    public static void main(String[] a) throws Exception {
        kotlin.Pair<java.net.Socket, VncClient> c = VncClient.Companion.connect("127.0.0.1", Integer.parseInt(a[0]), 2000);
        VncClient v = c.getSecond();
        v.handshake();
        Framebuffer fb = settle(v, null, 5000);
        System.out.println("first " + fb.getWidth() + "x" + fb.getHeight() + " pixel(10,10) in memory R G B A = " + px(fb, 10, 10));
        System.out.flush();
        long t1 = System.currentTimeMillis();
        fb = settle(v, a[2], 15000);
        if (fb == null) { System.out.println("NO RESIZE"); return; }
        System.out.println("resized " + fb.getWidth() + "x" + fb.getHeight() + " same socket, " + (System.currentTimeMillis() - t1) + " ms after first frame, pixel(10,10) = " + px(fb, 10, 10));
        c.getFirst().close();
    }
}
EOF
STD=$(find /root/.gradle/caches/modules-2 -name "kotlin-stdlib-1.9.2*.jar" ! -name "*sources*" | head -1)
javac -cp /root/palma/app/build/tmp/kotlin-classes/debug:$STD -d . Proof.java

cat > session-stand.sh <<'EOF'
#!/bin/sh
SCRIPT=/root/palma/image-build/palma-session
STD=$(find /root/.gradle/caches/modules-2 -name "kotlin-stdlib-1.9.2*.jar" ! -name "*sources*" | head -1)
CLS=/root/palma/app/build/tmp/kotlin-classes/debug
FUNCS=/var/tmp/vncproof/funcs.sh
sed -n '1,/^clean_leftovers$/p' "$SCRIPT" | sed '$d' | sed 's|^exec >>/tmp/trace.log 2>&1$||' > "$FUNCS"
export PALMA_GEOMETRY=900x2000 PALMA_VNC_PORT=5995
mkdir -p /tmp/root-home; export HOME=/tmp/root-home
printf 'Xft.dpi: 96\n' > /tmp/Xresources; sed -i 's|/root/.Xresources|/tmp/Xresources|' "$FUNCS"; . "$FUNCS"
DISP=:5
start_display
echo "after start_display: fb=$(fb_size) MAX=${MAXW}x${MAXH} NORANDR=${NORANDR:-no}"
echo "x11vnc listen: $(ss -ltn '( sport = :5995 )' | tail -n +2 | awk '{print $4}' | tr '\n' ' ')"
DISPLAY=$DISP xsetroot -solid "#FF8000"
DISPLAY=$DISP xprop -root -spy >/dev/null 2>&1 & CLIENT=$!
java -cp /var/tmp/vncproof:$CLS:$STD Proof 5995 $DISP 2000x900 > /tmp/proof.out 2>&1 & JP=$!
sleep 2
T0=$(date +%s%N)
apply_geometry 2000x900
echo "apply_geometry took $(( ($(date +%s%N) - T0) / 1000000 )) ms, fb=$(fb_size)"
wait $JP
cat /tmp/proof.out
echo "X client after rotate: $(kill -0 $CLIENT 2>/dev/null && echo alive || echo dead)"
echo "Xvfb pid same: $(kill -0 $XPID 2>/dev/null && echo yes || echo no)"
kill $CLIENT $VPID $XPID 2>/dev/null
EOF

printf '#!/bin/sh\necho "current 2000 x 2000, maximum 2000 x 2000"\nexit 1\n' > shim/xrandr && chmod +x shim/xrandr
cat > fallback-stand.sh <<'EOF'
#!/bin/sh
FUNCS=/var/tmp/vncproof/funcs.sh
sed -n '1,/^clean_leftovers$/p' /root/palma/image-build/palma-session | sed '$d' | sed 's|^exec >>/tmp/trace.log 2>&1$||' > "$FUNCS"
export PALMA_GEOMETRY=900x2000 PALMA_VNC_PORT=5996 HOME=/tmp
. "$FUNCS" >/dev/null
export PATH=/var/tmp/vncproof/shim:$PATH
DISP=:6
start_display
echo "NORANDR=${NORANDR:-no}"
PATH=${PATH#*:} xdpyinfo -display :6 | grep dimensions
apply_geometry 2000x900
PATH=${PATH#*:} xdpyinfo -display :6 | grep dimensions
kill $VPID $XPID $DESKPID 2>/dev/null
EOF

for t in session-stand.sh fallback-stand.sh; do
  echo "=== $t"
  unshare -pfm --mount-proc sh -c "mount -t tmpfs none /tmp && sh /var/tmp/vncproof/$t" 2>&1 | grep -v "^$\|BOOT"
done
```
