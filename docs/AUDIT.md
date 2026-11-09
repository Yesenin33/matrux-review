# Аудит: VNC-клиент, сессия, экран стола, palma-session

Размеры кадра считаются для типичной геометрии `900x2000` (телефон 1080×2400 после `MAX_WIDTH = 900` в `AppGraph.kt:153`): 900 × 2000 × 4 Б = 7,2 МБ на кадр.

Факт, от которого зависят выводы по супервизору: ни в одном rootfs (`arch-rootfs`, `debian-rootfs`, `debian13-rootfs`, `ubuntu-rootfs`) нет `Xvnc`. Во всех образах работает ветка `Xvfb + x11vnc`, ветка Xvnc в `palma-session:182-193` мёртвая.

---

## 1. `app/src/main/java/ru/palma/app/shell/VncClient.kt`

### 1.1 Красный и синий перепутаны — `VncClient.kt:176-179`, `:22-27`
**Что не так.** Декодер собирает int `0xFFRRGGBB` и кладёт его в `IntBuffer` с нативным (little-endian) порядком. В памяти получается `BB GG RR FF`. `Bitmap.copyPixelsFromBuffer` для `ARGB_8888` ждёт байты в порядке `R G B A`, то есть int `0xAABBGGRR` (это прямо указано в документации `Bitmap.Config.ARGB_8888`).
**Чем грозит.** Неверный кадр: оранжевое выходит синим, синие ссылки — коричневыми. На сером XFCE это почти не видно, на обоях и иконках видно сразу.
**Правка.** Попросить у сервера тот порядок байт, который нужен Bitmap, и убрать попиксельную перепаковку. В `setPixelFormat()` (`:204-206`) заменить
```
output.writeByte(16)
output.writeByte(8)
output.writeByte(0)
```
на
```
output.writeByte(0)
output.writeByte(8)
output.writeByte(16)
```
Тогда Raw приходит как `R G B X`, и строка копируется в буфер целиком (см. 1.2). При создании Bitmap вызвать `setHasAlpha(false)`, чтобы мусор в байте X не давал прозрачность.

### 1.2 Попиксельный `IntBuffer.put` и новый `ByteArray` на каждый прямоугольник — `VncClient.kt:170-181`
**Что не так.** На каждый Raw-прямоугольник выделяется `ByteArray(w * 4)`. Полный кадр — это 1,8 млн вызовов `IntBuffer.put(index, value)` на direct-буфере, и каждый проверяет границы.
**Чем грозит.** Лаги: после поворота и при прокрутке декодер тратит на один кадр десятки миллисекунд, и ввод отстаёт.
**Правка.** Одна переиспользуемая строка в `VncClient` и копирование строки целиком (вместе с правкой 1.1):
```kotlin
private var row = ByteArray(0)

private fun readRaw(x: Int, y: Int, w: Int, h: Int) {
    val fb = framebuffer
    val bytes = w * 4
    if (row.size < bytes) row = ByteArray(bytes)
    val target = fb.buffer.duplicate()
    for (line in 0 until h) {
        input.readFully(row, 0, bytes)
        target.position(((y + line) * fb.width + x) * 4)
        target.put(row, 0, bytes)
    }
}
```

### 1.3 Кадр копируется в Bitmap на главном потоке, пока поток pump пишет в тот же буфер — `VncClient.kt:22-27` + `MainActivity.kt:379`
**Что не так.** `frameBitmap()` вызывается прямо в композиции (`val bitmap = frame?.first?.frameBitmap()`), и копируются все 7,2 МБ. Это происходит при каждой рекомпозиции `PalmaApp`, а не только при новом кадре: смена клавиатуры, режима ввода, раскладки, `russian`, `stuck`. В это же время поток `pump` пишет в `buffer` без всякой синхронизации.
**Чем грозит.** Лаги на главном потоке (копирование 7,2 МБ на каждую рекомпозицию) и неверный кадр: рваная картинка, где верх от нового кадра, низ от старого.
**Правка.** Копировать в Bitmap на потоке pump сразу после `readMessage() == true`, а в UI отдавать готовый Bitmap. В `Framebuffer`:
```kotlin
private val lock = Any()
fun <T> locked(block: () -> T): T = synchronized(lock, block)
fun publish(target: Bitmap) = locked {
    val source = buffer.duplicate()
    source.rewind()
    target.copyPixelsFromBuffer(source)
}
```
Чтение прямоугольников в `readMessage()` для типа 0 обернуть в `framebuffer.locked { repeat(count) { readRect() } }`. В `SessionController.pump()` вызывать `publish()` и класть в StateFlow `Frame(bitmap, seq)` (см. 2.1). Строку `MainActivity.kt:379` удалить, `DesktopUi.frame` получает готовый Bitmap.

### 1.4 Bitmap и direct-буферы не освобождаются при смене размера и переподключении — `VncClient.kt:20`, `:91`, `:138-160`
**Что не так.** `resize()`, `grow()` и `handshake()` при переподключении создают новый `Framebuffer`: новый direct `ByteBuffer` на 7,2 МБ и, после первого `frameBitmap()`, новый Bitmap на 7,2 МБ. Старые ничем не освобождаются и ждут GC. Direct-буфер освобождается только Cleaner'ом после GC.
**Чем грозит.** Утечка до GC: каждый поворот оставляет 14,4 МБ мусора вне Java-кучи. Несколько поворотов подряд на телефоне с 3–4 ГБ памяти приводят к `OutOfMemoryError` в `createBitmap` или к убийству процесса LMK.
**Правка.** Держать один Bitmap на сессию в `SessionController`, выделять его под максимальную сторону и менять размер через `reconfigure`:
```kotlin
private var surface: Bitmap? = null

private fun surfaceFor(w: Int, h: Int): Bitmap {
    val side = maxOf(w, h)
    val current = surface
    if (current != null && current.allocationByteCount >= w * h * 4) {
        if (current.width != w || current.height != h) current.reconfigure(w, h, Bitmap.Config.ARGB_8888)
        return current
    }
    return Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).also {
        it.setHasAlpha(false)
        it.reconfigure(w, h, Bitmap.Config.ARGB_8888)
        surface = it
    }
}
```
Поле `image` и `frameBitmap()` из `Framebuffer` убрать. В `teardown()` добавить `surface = null`; `recycle()` не нужен, это единственная ссылка, и UI к этому моменту показывает «Ожидание».

### 1.5 `grow()` раздувает буфер за пределы экрана сервера — `VncClient.kt:152-160`, `:169`
**Что не так.** Если прямоугольник выходит за границу (бывает в гонке со сменой размера), буфер увеличивается до `max(w, old.width) × max(h, old.height)`, и попиксельно копируется старое содержимое. После этого `requestUpdate` просит у сервера область больше его экрана, а `SessionController.syncGeometry()` видит `actual != want`.
**Чем грозит.** Неверный кадр (чёрная полоса справа или снизу) и полный перезапуск сессии через `restartGeometry()` (см. 2.3). Поворот превращается в рестарт.
**Правка.** Удалить `grow()`. Прямоугольник за границей дочитывать и отбрасывать:
```kotlin
ENCODING_RAW -> {
    val fb = framebuffer
    if (x + w > fb.width || y + h > fb.height) skipFully(w * h * 4L) else readRaw(x, y, w, h)
}
```

### 1.6 `resize()` копирует старый кадр попиксельно — `VncClient.kt:141-147`
**Что не так.** После DesktopSize клиент всё равно запрашивает полный кадр (`sizeChanged → incremental=false`). Копирование старого содержимого (до 1,8 млн пар get/put) ничего не даёт: кадр сразу перезаписывается.
**Чем грозит.** Лишние 20–50 мс на потоке pump в момент поворота.
**Правка.** Оставить только `framebuffer = Framebuffer(w, h)` и `sizeChanged = true`, цикл копирования удалить.

### 1.7 `copyRect` выделяет `IntArray` на каждый вызов и копирует по одному int — `VncClient.kt:42-50`
**Что не так.** `IntArray(w)` на каждый CopyRect, плюс `w` вызовов `get` и `w` вызовов `put` на строку. При прокрутке в Thunar и в терминале CopyRect приходит десятками за кадр.
**Чем грозит.** Лишние аллокации и GC при прокрутке, рывки.
**Правка.** Массовые операции над `ByteBuffer`, которые корректно работают с перекрытием:
```kotlin
fun copyRect(srcX: Int, srcY: Int, x: Int, y: Int, w: Int, h: Int) {
    if (w <= 0 || h <= 0) return
    if (srcX + w > width || srcY + h > height || x + w > width || y + h > height) return
    val bytes = w * 4
    if (scratch.size < bytes) scratch = ByteArray(bytes)
    val down = y > srcY
    val view = buffer.duplicate()
    for (n in 0 until h) {
        val r = if (down) h - 1 - n else n
        view.position(((srcY + r) * width + srcX) * 4)
        view.get(scratch, 0, bytes)
        view.position(((y + r) * width + x) * 4)
        view.put(scratch, 0, bytes)
    }
}
private var scratch = ByteArray(0)
```
Проверка границ здесь нужна: сейчас CopyRect с источником за краем бросает `IndexOutOfBoundsException`, и pump уходит в переподключение.

### 1.8 `skipBytes` может пропустить меньше, чем нужно — `VncClient.kt:89`, `:110`, `:114-115`, `:187`
**Что не так.** `DataInputStream.skipBytes` молча возвращает меньшее число при EOF и с отрицательным аргументом. Длина ServerCutText (`:115`) — знаковый `readInt()`. Если там больше 2 ГБ или прямой мусор, `skipBytes` получает отрицательное число и ничего не пропускает.
**Чем грозит.** Рассинхрон протокола: следующие байты читаются как тип сообщения, клиент падает с `unknown_message_N` или рисует мусор.
**Правка.**
```kotlin
private fun skipFully(count: Long) {
    if (count < 0) throw VncException("bad_length")
    var left = count
    while (left > 0) {
        val skipped = input.skip(left)
        if (skipped <= 0) { input.readByte(); left-- } else left -= skipped
    }
}
```
Все `skipBytes(...)` заменить на `skipFully(...)`. Для `:115`: `skipFully(input.readInt().toLong() and 0xFFFFFFFFL)`. Длину имени в `:90` ограничить так же, как в `readReason()`: `if (length !in 0..4096) throw VncException("bad_name")`.

### 1.9 Нет таймаута на рукопожатие — `VncClient.kt:245-254`
**Что не так.** `soTimeout` не задан. Когда x11vnc принял TCP-соединение, но висит (идёт пересборка стола, proot тормозит), `input.readFully(version)` блокируется навсегда.
**Чем грозит.** Зависание: в `startDisplay()` дедлайн `DISPLAY_TIMEOUT_MS` проверяется только между попытками и до него не доходит. Экран «Запуск» висит без ошибки.
**Правка.** В `connect()` после `socket.connect(...)` добавить `socket.soTimeout = HANDSHAKE_TIMEOUT_MS` (5000). В конце `handshake()` нужен доступ к сокету: передать его в конструктор `VncClient(socket, input, output)` и после `requestUpdate(false)` вызвать `socket.soTimeout = 0`. Таймаут после рукопожатия оставлять нельзя: статичный экран — это нормальное молчание сервера, а `SocketTimeoutException` посреди сообщения ломает поток.

### 1.10 `framebuffer` читается из разных потоков без `@Volatile` — `VncClient.kt:55`
**Что не так.** Pump заменяет `framebuffer` в `resize()`. Главный поток читает `client.framebuffer.width` в `moveCursor()` и `click()` (`SessionController.kt:240`, `:255`), IO-потоки читают его в `pointer()` (`:125-126`). Без барьера памяти главный поток может видеть старый объект неопределённо долго.
**Чем грозит.** После поворота курсор упирается в старые границы, `pointer()` обрезает координаты по старому размеру, и клики уходят не туда.
**Правка.** `@Volatile lateinit var framebuffer` нельзя, поэтому:
```kotlin
@Volatile private var current: Framebuffer? = null
val framebuffer: Framebuffer get() = current ?: throw VncException("no_framebuffer")
```
и присваивать `current = ...` в `handshake()` и `resize()`.

### 1.11 Encodings: Tight не поддерживается, ExtendedDesktopSize разбирается, но не объявлен — `VncClient.kt:211-219`, `:185-189`
**Что не так.** Клиент объявляет только CopyRect, Raw и DesktopSize. Ветка `ENCODING_EXT_DESKTOP_SIZE` недостижима: сервер шлёт -308 только тем, кто его объявил. Tight в клиенте нет совсем.
**Чем грозит.** По Tight ничего не грозит, на loopback он не нужен: каждый Tight-прямоугольник — это zlib-распаковка на телефоне плюс JPEG/zlib-сжатие в госте под ptrace, а это дороже, чем гнать Raw через localhost. Мёртвая ветка -308 вводит в заблуждение. Отсутствие Cursor pseudo-encoding заставляет x11vnc рисовать курсор в кадре: в режиме «Мышь» каждое движение порождает FramebufferUpdate.
**Правка.**
- Tight не добавлять. Если когда-нибудь понадобится сеть не через loopback — ZRLE без JPEG.
- `setEncodings()`: `writeShort(4)` и добавить `writeInt(ENCODING_CURSOR)` (`-239`, RichCursor). Разбор: `ENCODING_CURSOR -> { cursor = readCursor(w, h); hotX = x; hotY = y }`, где читается `w*h*4` байт пикселей и `((w + 7) / 8) * h` байт маски. Курсор рисовать поверх кадра в `Stage` (см. 3.1). Это убирает поток обновлений от движения мыши.
- Ветку `ENCODING_EXT_DESKTOP_SIZE` либо удалить, либо объявить -308 в `setEncodings()`. x11vnc его не шлёт, поэтому удалить.

### 1.12 `blank()` на каждом кадре — `VncClient.kt:30-38`, `SessionController.kt:383`, `:387`
**Что не так.** Около 106 000 вызовов `IntBuffer.get` на каждом кадре навсегда, а при смене размера ещё раз (`:387`). Флаг нужен только чтобы через `STUCK_MS` показать экран «Завис».
**Чем грозит.** Лишняя нагрузка на поток pump, растущая с частотой кадров.
**Правка.** В `pump()` проверять, пока стол не появился:
```kotlin
if (blankState.value || seen == 0L) blankState.value = client.framebuffer.blank()
```
и в логе на `:387` использовать уже вычисленное `blankState.value`.

---

## 2. `app/src/main/java/ru/palma/app/session/SessionController.kt`

### 2.1 Метка кадра по `currentTimeMillis` — StateFlow выбрасывает кадры — `SessionController.kt:382`, `:336`, `:417`
**Что не так.** `frames.value = client.framebuffer to clock()`. Объект `Framebuffer` тот же, и если два обновления пришли в одну миллисекунду, `Pair` равны. StateFlow сравнивает через `equals` и второе значение не публикует. В UI `remember(stamp)` тоже не пересоздаёт картинку.
**Чем грозит.** Неверный кадр: последнее обновление (например, конец ввода в поле или закрытие меню) не показывается, пока сервер не пришлёт следующее. На статичном экране его может не быть долго.
**Правка.**
```kotlin
data class Frame(val bitmap: Bitmap, val seq: Long)
private val seq = java.util.concurrent.atomic.AtomicLong()
private val frames = MutableStateFlow<Frame?>(null)
```
В `pump()`: `client.framebuffer.publish(surface); frames.value = Frame(surface, seq.incrementAndGet())`. `DesktopUi.frameStamp` заменить на `seq`.

### 2.2 Каждое событие касания — отдельная корутина, порядок не гарантирован — `SessionController.kt:170-184`, `:229-236`
**Что не так.** `pointer()` на каждое `ACTION_MOVE` делает `scope.launch(Dispatchers.IO)`. Корутины расходятся по пулу IO и выполняются в произвольном порядке. Отпускание кнопки (`buttons = 0`) может уйти раньше последнего движения с нажатой кнопкой. `tap()` и `pointer()` идут мимо очереди `enqueue`, поэтому клик с панели мыши и движение курсора перемешиваются. В `enqueue` `withTimeoutOrNull(2000) { previous?.join() }` после двух секунд пускает следующий блок параллельно с зависшим.
**Чем грозит.** Неверный ввод: залипшая кнопка (окно «прилипает» к пальцу), рывки курсора назад, лишние выделения текста. Плюс 60–120 корутин и `flush()` в секунду при перетаскивании.
**Правка.** Одна очередь ввода на сессию и слияние движений:
```kotlin
private sealed interface Input {
    data class Move(val x: Int, val y: Int, val buttons: Int) : Input
    class Script(val block: suspend (VncClient) -> Unit) : Input
}
private val inputs = kotlinx.coroutines.channels.Channel<Input>(capacity = 256)

private fun startInput() = scope.launch(Dispatchers.IO) {
    for (first in inputs) {
        var next: Input = first
        while (next is Input.Move) {
            val more = inputs.tryReceive().getOrNull()
            if (more !is Input.Move || more.buttons != next.buttons) {
                send(next)
                next = more ?: break
                continue
            }
            next = more
        }
        if (next is Input.Script) send(next)
    }
}

private suspend fun send(input: Input) {
    val client = vnc ?: return
    runCatching {
        when (input) {
            is Input.Move -> client.pointer(input.x, input.y, input.buttons)
            is Input.Script -> input.block(client)
        }
    }
}
```
`pointer()` → `inputs.trySend(Input.Move(x, y, buttons))`, `tap()`, `key()`, `combo()`, `click()`, `scroll()` → `inputs.trySend(Input.Script { ... })`. Поле `inputJob` и `withTimeoutOrNull(QUEUE_WAIT_MS)` удалить. Корутину очереди запускать в `start()` и отменять в `stop()`.

### 2.3 `syncGeometry()`: кулдаун теряет поворот, фоновая корутина не отменяется, двойное переподключение — `SessionController.kt:122-147`
**Что не так.**
1. `:131-132`: второй поворот в течение 12 с просто игнорируется и потом не повторяется. Повернул и сразу вернул — гость остаётся в неверной геометрии до следующего поворота.
2. `:131` берёт `System.currentTimeMillis()` вместо внедрённого `clock()`, и тесты не могут это проверить.
3. `:136` `frames.value = null` гасит картинку сразу, но pump ещё жив и в ближайшие миллисекунды кладёт в `frames` старый кадр. На экране «Ожидание» и старый кадр сменяют друг друга.
4. `:137-143` корутина не сохраняется и не отменяется ни в `stop()`, ни при следующем `syncGeometry()`. Через 9 с она закрывает сокет, хотя гость к этому моменту уже убил x11vnc при пересборке, pump уже переподключился, и закрывается **новое** соединение. Ещё через 20 с старая корутина сравнивает кадр со своим устаревшим `want` и может вызвать `restartGeometry()` уже после второго поворота.
5. `:129` `mismatch` при любом расхождении размера (например, после `grow()` из 1.5) ведёт к полному `stop()/start()`.
6. `:127` `Log.i` на каждый вызов с конкатенацией и полным именем `android.util.Log`, хотя `Log` уже импортирован.

**Чем грозит.** Поворот занимает 9–30 с чёрного экрана, иногда заканчивается полным рестартом сессии, иногда — неверной геометрией. Возможен «самозапуск» после ручного закрытия, если корутина дожила до `restartGeometry()`.

**Правка.** Дебаунс вместо кулдауна, одна отменяемая задача, без ручного закрытия сокета (переподключение сделает pump, когда гость перезапустит x11vnc; после правки 4.1 переподключения вообще не будет — придёт DesktopSize):
```kotlin
private var geometryJob: Job? = null

fun syncGeometry() {
    val state = mutable.value
    if (state !is SessionState.Live) return
    geometryJob?.cancel()
    geometryJob = scope.launch {
        delay(GEOMETRY_DEBOUNCE_MS)
        val want = geometry()
        if (want == startedGeometry) return@launch
        if (!requestGeometry(want)) return@launch restartGeometry()
        startedGeometry = want
        val reached = withTimeoutOrNull(GEOMETRY_WAIT_MS) {
            frames.first { it != null && "${it.bitmap.width}x${it.bitmap.height}" == want }
        }
        if (reached == null) restartGeometry()
    }
}
```
`GEOMETRY_DEBOUNCE_MS = 400L`. Удалить `lastGeometryAt`, `GEOMETRY_COOLDOWN_MS`, `GEOMETRY_SETTLE_MS`, `reconnectDisplay()`, строку `frames.value = null` из `syncGeometry()` и строку `Log.i`. В `stop()` добавить `geometryJob?.cancel()`. Состояние `Launching` из условия убрать: во время запуска геометрию и так берёт `startEngine()` через `geometry()`.

### 2.4 `geometry-request` пишется неатомарно — `SessionController.kt:153-158`
**Что не так.** `File.writeText` открывает файл с усечением и пишет. Цикл супервизора раз в 2 с читает его через `tr` и может застать пустой или частичный файл.
**Чем грозит.** Сейчас частичные значения почти всегда отсекаются проверкой `>= 320` в `palma-session:391`, но для пары вида `1440x3200` частичное `1440x320` проходит, и стол пересобирается в неверном размере.
**Правка.**
```kotlin
private fun requestGeometry(size: String): Boolean = runCatching {
    val dir = guestTmp.apply { mkdirs() }
    val part = File(dir, "geometry-request.part")
    part.writeText(size)
    part.renameTo(File(dir, "geometry-request"))
}.getOrDefault(false)
```
где `guestTmp` — см. 2.5.

### 2.5 Диагностика читает не ту папку и не те файлы — `SessionController.kt:351`, `:368`, `:465-466`
**Что не так.** Гостевой `/tmp` — это `cacheDir/proot` (`AppGraph.kt:60` → `Proot.kt:27`). `SessionController.tmpDir` — это `cacheDir/x11` (`AppGraph.kt:62`). Логи ищутся в `cacheDir/x11/*.log`, где их никогда нет. `geometry-request` попадает в нужную папку только через хрупкое `tmpDir.parentFile + "proot/"` (`:154`). Имена тоже неверные: `xvfb.log` скрипт не пишет (пишет `xvfb-N.log`), `palma-session.log` скрипт обнуляет (`palma-session:4`) и больше в него не пишет, весь вывод уходит в `trace.log`.
**Чем грозит.** Экран «Завис» и `Failure.Display` всегда показывают пустой отчёт, поэтому причины зависаний у пользователей не видны.
**Правка.** В конструктор добавить `private val guestTmp: File` и в `AppGraph` передавать тот же `File(app.cacheDir, "proot")`, что и в `ProotEngine`. Списки заменить:
```kotlin
val DISPLAY_LOGS = listOf("trace.log", "xvfb-0.log", "xvfb-1.log", "x11vnc.log")
val DIAGNOSTIC_LOGS = listOf("trace.log", "xvfb-0.log", "x11vnc.log", "xfce.log")
```
и читать `File(guestTmp, name)`. `File(tmpDir.parentFile, "proot/geometry-request")` → `File(guestTmp, "geometry-request")`.

### 2.6 Вывод движка: два читателя одного потока и хранится начало вместо хвоста — `SessionController.kt:307-321`
**Что не так.** Фоновая корутина читает `started.inputStream` построчно, а при падении `:318` пытается прочитать тот же поток ещё раз через `readText()`, но он уже вычитан. `engineOutput` перестаёт пополняться после 1200 символов, а диагностика берёт `takeLast(500)` от этого начала.
**Чем грозит.** В `Failure.Engine` чаще всего пустой текст, в долгой сессии видно только начало лога.
**Правка.** `:318-319` заменить на
```kotlin
delay(OUTPUT_DRAIN_MS)
val output = synchronized(engineOutput) { engineOutput.toString().trim().takeLast(MAX_ENGINE_OUTPUT) }
```
(`OUTPUT_DRAIN_MS = 200L`). В `:311` хранить хвост:
```kotlin
engineOutput.append(line).append('\n')
if (engineOutput.length > MAX_ENGINE_OUTPUT * 2) engineOutput.delete(0, engineOutput.length - MAX_ENGINE_OUTPUT)
```

### 2.7 Поля сессии без синхронизации между Main, Default и IO — `SessionController.kt:64-71`, `:76`
**Что не так.** `scope` работает на `Dispatchers.Default` (`AppGraph.kt:38`). `start()` и `stop()` вызываются с Main, `teardown()` — с Default (через `LaunchState.Failed`) и с IO (`pump`). `vnc`, `socket`, `process`, `cursorX/Y`, `restarting`, `startedGeometry` — обычные `var`.
**Чем грозит.** `pointer()` на главном потоке видит закрытый `vnc` и пишет в мёртвый сокет. `restarting` из `stop()` не виден корутине `restartGeometry()`, и сессия самовоскресает после закрытия.
**Правка.** `@Volatile` на `vnc`, `socket`, `process`, `restarting`, `startedGeometry`. Курсорные поля читаются и пишутся только с главного потока, их оставить, но `teardown()` вызывать через `withContext(Dispatchers.Main.immediate)` из `pump()` и из `collect`.

### 2.8 `stop()` сканирует `/proc` на главном потоке; `Xvnc` и `dbus-daemon` не в списке — `SessionController.kt:118`, `GuestProcesses.kt:33-36`, `:48-64`
**Что не так.** `killLeftovers()` читает `cmdline` всех процессов на Main. Список имён не содержит `Xvnc` и `dbus-daemon`.
**Чем грозит.** Подвисание UI на 50–300 мс при нажатии «Закрыть» и при каждом `restartGeometry()`. Процессы `dbus-daemon` копятся (по одному на каждую пересборку стола, см. 4.5).
**Правка.** `GuestProcesses.killLeftovers()` → `scope.launch(Dispatchers.IO) { GuestProcesses.killLeftovers() }`. В `NAMES` добавить `"Xvnc"` и `"dbus-daemon"`.

### 2.9 При обрыве в pump старый кадр висит до 60 с без индикации — `SessionController.kt:392-397`, `:407-426`
**Что не так.** На исключении сокет закрывается, `vnc = null`, но `frames` остаётся со старым кадром, и `reattach()` до 60 с пытается переподключиться. Касания уходят в `vnc ?: return`, то есть пропадают.
**Чем грозит.** Выглядит как зависание: картинка есть, на касания не реагирует.
**Правка.** В `catch` перед `reattach()` выставить `blankState.value = true`. Тогда через `STUCK_MS` появится экран «Завис» с отчётом. Либо добавить в `SessionState.Live` поле `reconnecting: Boolean` и показывать в `ControlBar` строку «Переподключение».

### 2.10 Раскладка переключается вслепую через Alt+Shift — `SessionController.kt:194-210`, `:220-227`
**Что не так.** `russianState` — догадка клиента о текущей группе XKB. После любой пересборки X (поворот, падение Xvfb) `setxkbmap` не применяется заново (см. 4.3), и Alt+Shift ничего не делает. Если пользователь переключит раскладку в самом госте, состояния тоже разъедутся.
**Чем грозит.** Неверный ввод: кириллица печатается латиницей и наоборот, до ручного переключения.
**Правка.** Минимальная — 4.3. Надёжная — запускать x11vnc с `-xkb` (он ищет keycode и группу для keysym по всем группам), слать кириллические keysym без `switchGroup()`, а `toggleLayout()` оставить только как кнопку для пользователя. Проверить на образе: напечатать «съешь ещё» при включённой группе `us`.

---

## 3. `app/src/main/java/ru/palma/app/ui/screens/DesktopScreen.kt`

### 3.1 Каждый кадр рекомпонует весь `PalmaApp` и весь `DesktopScreen` — `MainActivity.kt:166`, `:379-397`, `DesktopScreen.kt:63-77`, `:109`, `:319`
**Что не так.** `frame` собирается на верхнем уровне `PalmaApp`, поэтому каждый кадр рекомпонует `AnimatedContent`, создаёт новые `DesktopUi` и `DesktopActions` (около 18 новых лямбд) и копирует Bitmap (1.3). `DesktopUi` содержит `Bitmap`, это нестабильный тип, и `DesktopScreen` не пропускается. `ControlBar`, `CursorPad` и `KeyboardSink` рекомпонуются на каждый кадр. `remember(stamp) { frame.asImageBitmap() }` при одинаковом `stamp` (2.1) оставляет старую обёртку над уже изменённым Bitmap.
**Чем грозит.** Лаги: 30–60 рекомпозиций дерева в секунду при прокрутке в госте, просадки кадров в анимации панели, задержка касаний.
**Правка.** Кадр читать только в фазе рисования внутри `Stage`:
1. Из `PalmaApp` убрать `val frame by ...` и строку `:379`. В `DesktopUi` вместо `frame: Bitmap?` и `frameStamp` передавать `frame: StateFlow<Frame?>` (ссылка не меняется) и `hasFrame: Boolean` (меняется только при появлении или пропаже кадра; собирать через `graph.session.frame.map { it != null }.distinctUntilChanged()`).
2. `Stage`:
```kotlin
@Composable
private fun Stage(frames: StateFlow<Frame?>, input: InputMode, actions: DesktopActions) {
    val frame = frames.collectAsState()
    var box by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .then(gesture(input, { box }, { frame.value?.bitmap }, actions))
            .drawBehind {
                val current = frame.value ?: return@drawBehind
                val bitmap = current.bitmap
                val scale = min(size.width / bitmap.width, size.height / bitmap.height)
                val left = (size.width - bitmap.width * scale) / 2
                val top = (size.height - bitmap.height * scale) / 2
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(
                        bitmap, null,
                        android.graphics.RectF(left, top, left + bitmap.width * scale, top + bitmap.height * scale),
                        null,
                    )
                }
            },
    )
}
```
`frame.value` читается внутри `drawBehind`, поэтому новый кадр вызывает только перерисовку этого узла, без рекомпозиции. `asImageBitmap()` не нужен.
3. `DesktopActions` в `MainActivity` обернуть в `remember(graph) { DesktopActions(...) }`, изменяемые значения (`report`, `route`) передавать через `rememberUpdatedState`.

### 3.2 Касание и движение мыши уходят по одной корутине на событие — `DesktopScreen.kt:286-289`, `:300-303`
**Что не так.** В режиме «Касание» каждое событие указателя → `onPointer` → `scope.launch` (2.2). Порога смещения нет: любое дрожание пальца при тапе отправляется как перетаскивание с нажатой кнопкой.
**Чем грозит.** Тап по иконке превращается в микроперетаскивание: иконка сдвигается, выделяется текст, окно дёргается. Плюс проблема порядка из 2.2.
**Правка.** Вместе с 2.2: нажатие отправлять, только когда палец ушёл дальше `TAP_SLOP` или прошло `LONG_PRESS_MS`; если палец отпущен раньше — отправить `tap(x, y)` в точке нажатия.
```kotlin
if (input == InputMode.Touch) {
    val point = guestPoint(change.position, box(), bitmap) ?: continue
    when {
        change.pressed && !change.previousPressed -> { downAt = change.position; downPoint = point; dragging = false }
        change.pressed && !dragging && (change.position - downAt).getDistance() > TAP_SLOP -> {
            dragging = true
            actions.onPointer(downPoint.first, downPoint.second, 1)
            actions.onPointer(point.first, point.second, 1)
        }
        change.pressed && dragging -> actions.onPointer(point.first, point.second, 1)
        !change.pressed && change.previousPressed ->
            if (dragging) actions.onPointer(point.first, point.second, 0) else actions.onTap(downPoint.first, downPoint.second)
    }
}
```
В `DesktopActions` добавить `onTap`, в `MainActivity` подключить к `graph.session.tap`.

### 3.3 Двухпальцевый тап даёт ПКМ и сразу ЛКМ — `DesktopScreen.kt:282-283`, `:307-312`, `:236-237`, `:255-260`
**Что не так.** Обрабатывается только `event.changes.firstOrNull()`. Когда поднимается первый палец, срабатывает `onClick(2)` и сбрасывается `two = false`, хотя второй палец ещё на экране. Его последующее отпускание даёт `onClick(1)`.
**Чем грозит.** Неверный ввод: контекстное меню открывается и тут же закрывается левым кликом или выбирается первый пункт.
**Правка.** Завершать жест, только когда подняты все пальцы:
```kotlin
val anyPressed = event.changes.any { it.pressed }
val change = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
...
if (!anyPressed && event.changes.any { it.previousPressed }) {
    if (drag < TAP_SLOP) actions.onClick(if (two) 2 else 1)
    drag = 0f; scroll = 0f; two = false
}
```
Одинаково в `Stage` и в `Trackpad`.

### 3.4 Первый тап в режиме «Мышь» после каждого перезапуска жеста проглатывается — `DesktopScreen.kt:276`
**Что не так.** `var drag = TAP_SLOP`. `pointerInput` перезапускается при смене `input`, `frame.width` и `frame.height`, то есть при каждом повороте. Первый тап сравнивает `TAP_SLOP < TAP_SLOP` → `false`, и клика нет.
**Чем грозит.** После поворота и после переключения на мышь первый клик не срабатывает.
**Правка.** `var drag = TAP_SLOP` → `var drag = 0f`.

### 3.5 Медленное движение курсора теряется на округлении — `DesktopScreen.kt:248-251`, `:300-303`
**Что не так.** `(delta.x * PAD_SPEED).roundToInt()` на каждое событие. При медленном ведении `delta` меньше 0,28 px, и результат 0.
**Чем грозит.** Курсор «липнет» при точном наведении на мелкие элементы (крестик окна, разделители).
**Правка.**
```kotlin
var restX = 0f
var restY = 0f
...
restX += delta.x * PAD_SPEED
restY += delta.y * PAD_SPEED
val dx = restX.toInt()
val dy = restY.toInt()
restX -= dx
restY -= dy
if (dx != 0 || dy != 0) actions.onMoveCursor(dx, dy)
```

### 3.6 Устаревшие лямбды в `pointerInput` — `DesktopScreen.kt:229`, `:274`
**Что не так.** `Trackpad` — `pointerInput(Unit)`, `Stage` — ключи без `actions`. Корутина жеста навсегда держит `actions` из первой композиции. Сейчас лямбды вызывают одни и те же методы `graph.session`, и это работает случайно. Любая лямбда, захватывающая изменяемое состояние экрана, сломается молча.
**Правка.** `val current by rememberUpdatedState(actions)` и внутри жеста вызывать `current.onMoveCursor(...)`.

### 3.7 Клавиатура: Backspace не работает, IME дублирует буквы — `DesktopScreen.kt:345`, `:356-365`
**Что не так.** После каждого ввода поле сбрасывается в `""`. На пустом поле Backspace от IME (`deleteSurroundingText`) ничего не меняет, `onValueChange` не вызывается, ветка `added < 0` недостижима. Gboard и Яндекс Клавиатура с подсказками держат композицию: на «пр» IME присылает `"пр"` целиком, а поле считает, что было пусто, и отправляет обе буквы ещё раз.
**Чем грозит.** Неверный ввод: удалять символы с экранной клавиатуры нельзя, слова печатаются с повторами («ппрпри»).
**Правка.** Держать в поле постоянный запас и отключить подсказки:
```kotlin
private const val SENTINEL = "​​​​"
var field by remember { mutableStateOf(TextFieldValue(SENTINEL, TextRange(SENTINEL.length))) }
BasicTextField(
    value = field,
    onValueChange = { value ->
        val text = value.text
        when {
            text.length < SENTINEL.length -> repeat(SENTINEL.length - text.length) { onKey(Keys.BACKSPACE) }
            value.composition == null -> text.removePrefix(SENTINEL).forEach(onLetter)
            else -> { field = value; return@BasicTextField }
        }
        field = TextFieldValue(SENTINEL, TextRange(SENTINEL.length))
    },
    keyboardOptions = KeyboardOptions(
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.None,
    ),
    ...
)
```
`KeyboardType.Password` выключает подсказки и композицию у Gboard и Яндекс Клавиатуры. Если раскладка «пароль» неудобна, надёжный вариант — `AndroidView` с наследником `View`, где `onCreateInputConnection` возвращает `BaseInputConnection` и переопределены `commitText` и `deleteSurroundingText`. Так делают все VNC-клиенты.

### 3.8 Физическая клавиатура не передаётся — `DesktopScreen.kt:342-375`
**Что не так.** Нет `onPreviewKeyEvent`. Стрелки, Esc, Ctrl, Alt, F1–F12 с Bluetooth-клавиатуры и из DeX до гостя не доходят. Буквы приходят только пока открыто поле `KeyboardSink`.
**Чем грозит.** На планшетах с клавиатурой рабочий стол бесполезен.
**Правка.** На корневой `Box` в `DesktopScreen` добавить `.focusable().onPreviewKeyEvent { event -> actions.onRawKey(event.nativeKeyEvent); true }`. В `SessionController` сделать `rawKey(event: android.view.KeyEvent)`, который переводит `keyCode` в keysym (таблица в `Keys`), а для печатных — `event.unicodeChar`, и шлёт `key(keysym, event.action == ACTION_DOWN)` через очередь из 2.2.

---

## 4. `image-build/palma-session`

### 4.1 Поворот убивает весь рабочий стол вместе с программами пользователя — `palma-session:202-218`, `:382-399`
**Что не так.** `apply_geometry` → `rebuild_desktop` → `kill_desktop` убивает `-9` Xvfb, x11vnc и xfce, затем поднимает новый Xvfb. Все открытые программы теряют X-соединение и закрываются. Скрипт уже считает `MAXW`/`MAXH` (`:32-37`), но использует их только в режиме x11. Вызов `xrandr --fb` на `:306` ставит тот же размер, с которым запущен Xvfb, и ничего не делает.
**Чем грозит.** Потеря несохранённой работы при каждом повороте, 10–30 с чёрного экрана (Xvfb + xfce + ожидание приложения), и именно отсюда растут все гонки из 2.3.
**Правка.** Запускать Xvfb квадратным на максимальную сторону и менять размер через RandR. x11vnc с `-xrandr` (уже есть на `:139`) сам пришлёт клиенту DesktopSize, а клиент его обрабатывает (`VncClient.kt:184`).
`:194` →
```sh
Xvfb "$DISP" -screen 0 "${MAXW}x${MAXH}x24" -nolisten tcp -ac >"/tmp/xvfb-${DISP#:}.log" 2>&1 &
```
`:306` оставить как есть: теперь он реально уменьшает экран до `GWxGH`.
`apply_geometry` →
```sh
apply_geometry() {
  GEOMETRY="$1"
  GW=${GEOMETRY%x*}
  GH=${GEOMETRY#*x}
  if xrandr -d "$DISP" --fb "$GEOMETRY" >/dev/null 2>&1; then
    say "geometry $GEOMETRY через xrandr"
    return 0
  fi
  RESTARTS=0
  UP=""
  say "geometry $GEOMETRY, xrandr не сработал, пересобираю рабочий стол"
  rebuild_desktop
}
```
Память Xvfb для `2000x2000` — 16 МБ вместо 7,2 МБ; это цена сохранения программ. Перед выкаткой проверить на образе, что Xvfb из Debian/Ubuntu/Arch принимает `xrandr --fb` до начального размера (в xorg-server 21 принимает), и что x11vnc отдаёт новый размер без переподключения. Для максимальной стороны больше начальной (смена устройства) остаётся старая ветка с пересборкой.

### 4.2 x11vnc слушает все интерфейсы без пароля — `palma-session:139`, `:185`
**Что не так.** `x11vnc ... -nopw -rfbport "$VPORT"` без `-localhost` открывает порт на всех интерфейсах телефона, включая Wi-Fi. Ветка Xvnc явно делает то же: `-SecurityTypes None -localhost=0`.
**Чем грозит.** Любой в той же сети подключается к рабочему столу от root без пароля и видит и вводит всё, включая токены в браузере гостя. Это не производительность, но это самая серьёзная находка аудита.
**Правка.** `:139`: добавить `-localhost` (x11vnc слушает только `127.0.0.1`). `:185`: `-localhost=0` → `-localhost=1`. Клиент подключается к `127.0.0.1` (`SessionController.kt:443`), поэтому для приложения ничего не меняется.

### 4.3 После пересборки X не восстанавливаются раскладки и Xresources — `palma-session:311-318`, `:202-207`, `:328-336`
**Что не так.** `setxkbmap -layout us,ru -option grp:alt_shift_toggle` и `xrdb -merge` выполняются один раз до главного цикла. После `apply_geometry` и после перезапуска Xvfb (`:331-335`) новый X-сервер стартует с раскладкой `us` без переключателя.
**Чем грозит.** Неверный ввод: после первого поворота кириллица печатается латиницей, Alt+Shift из `SessionController.switchGroup()` ничего не делает. Шрифты переходят на DPI по умолчанию.
**Правка.** Вынести в функцию и вызывать после каждого `start_display`:
```sh
prepare_x() {
  xrandr -d "$DISP" --fb "${GW}x${GH}" >/dev/null 2>&1
  command -v setxkbmap >/dev/null 2>&1 && \
    DISPLAY="$DISP" setxkbmap -layout us,ru -option grp:alt_shift_toggle -option grp_led:scroll >/dev/null 2>&1
  command -v xrdb >/dev/null 2>&1 && DISPLAY="$DISP" xrdb -merge /root/.Xresources
}
```
Вызовы: в конце `start_display()`, вместо блоков `:306` и `:311-318`. Файл `/root/.Xresources` записывать один раз до цикла.

### 4.4 `wait_port` не ждёт порт — `palma-session:127-135`
**Что не так.** Функция возвращает 0, как только процесс x11vnc жив, то есть сразу после `&`. Порт к этому моменту ещё не слушает. Лог `vnc listening on ...` (`:307`) пишется до того, как это правда. Из-за этого приложение долбит `connect` каждые 500 мс (`SessionController.kt:327-343`), а при пересборке — до 60 с в `reattach()`.
**Чем грозит.** Лишние попытки подключения и вводящий в заблуждение лог при разборе зависаний.
**Правка.**
```sh
wait_port() {
  hex=$(printf ':%04X ' "$VPORT")
  i=0
  while [ "$i" -lt 50 ]; do
    live "$VPID" || return 1
    grep -q "$hex.* 0A " /proc/net/tcp 2>/dev/null && return 0
    i=$((i + 1))
    sleep 0.2
  done
  return 1
}
```
`/proc` смонтирован из хоста (`Proot.kt:25`), поэтому `/proc/net/tcp` показывает сокеты приложения.

### 4.5 `dbus-daemon` копится на каждой пересборке — `palma-session:163-165`, `:168-178`
**Что не так.** `kill_desktop` убивает `-9` процесс `dbus-run-session` (`DESKPID`). Его дочерний `dbus-daemon` остаётся жив: SIGKILL не даёт `dbus-run-session` прибрать за собой. Список имён в `:174` его не содержит.
**Чем грозит.** Утечка: плюс один `dbus-daemon` (около 3 МБ RSS) на каждый поворот и каждый перезапуск стола из цикла.
**Правка.** В `case` на `:174` добавить `dbus-daemon|dbus-run-session`. Перед этим посылать `kill -TERM "$DESKPID"` и ждать 0,5 с, чтобы xfce успел сохранить сессию, и только затем `-9`.

### 4.6 Общий счётчик `RESTARTS` для X и для рабочего стола — `palma-session:329-338`, `:362-370`
**Что не так.** Один `RESTARTS` и для падений Xvfb (лимит 4), и для падений xfce (лимит 40). Счётчик не сбрасывается, когда стол долго стабилен.
**Чем грозит.** Зависание или выход: после четырёх перезапусков xfce за сессию первое же падение Xvfb завершает супервизор (`exit 1`), и приложение показывает «Сессия потеряна».
**Правка.** Два счётчика: `XRESTARTS` (лимит 4) и `DRESTARTS` (лимит 40), оба обнулять, когда `UP=1` держится дольше 300 с:
```sh
if [ -n "$UP" ]; then STABLE=$((STABLE + 2)); [ "$STABLE" -ge 300 ] && { XRESTARTS=0; DRESTARTS=0; }; else STABLE=0; fi
```
Вставить перед проверкой `geometry-request`.

### 4.7 `geometry-request` не удаляется при неподходящем значении — `palma-session:382-399`
**Что не так.** Файл удаляется только при успешном применении. Если в нём текущая геометрия или мусор, он перечитывается каждые 2 с вечно, а частичная запись (2.4) может примениться на следующем проходе.
**Правка.** Сразу после `REQ=$(...)` добавить `rm -f /tmp/geometry-request`, внутреннюю `rm -f` на `:392` убрать.

### 4.8 Реакция на поворот — до 2 с только на опрос — `palma-session:401`
**Что не так.** Главный цикл спит 2 с. Запрос геометрии ждёт следующей итерации.
**Чем грозит.** До 2 с задержки поворота поверх всего остального.
**Правка.** Спать короче, только пока ждём файл:
```sh
n=0
while [ "$n" -lt 10 ] && [ ! -f /tmp/geometry-request ]; do sleep 0.2; n=$((n + 1)); done
```
вместо `sleep 2`. Счётчики времени (`WAITED`, `STABLE` из 4.6) продолжают работать в шагах по 2 с.

### 4.9 `clean_leftovers` убивает чужие PID из прошлой сессии — `palma-session:99-105`
**Что не так.** `/tmp/palma-pids` лежит в `cacheDir/proot` и переживает перезапуск приложения. На старте все записанные PID получают `kill -9` без проверки, что это всё ещё наш процесс. После перезапуска телефона или долгой паузы PID переиспользуются, а все процессы приложения работают под одним uid.
**Чем грозит.** Редкий, но тяжёлый случай: `kill -9` по процессу самого приложения или его сервиса, то есть падение без отчёта.
**Правка.**
```sh
for p in $(cat "$PIDS_FILE" 2>/dev/null); do
  case "$(cat "/proc/$p/comm" 2>/dev/null)" in
    Xvfb|Xvnc|x11vnc|xfce4-*|xfwm4|xfdesktop|xfsettingsd|xfconfd|dbus-*) kill -9 "$p" 2>/dev/null ;;
  esac
done
```

### 4.10 x11vnc без разделяемой памяти — `palma-session:139`
**Что не так.** `-noshm` заставляет x11vnc снимать экран через `XGetImage` по сокету X. proot запускается с `--sysvipc` (`Proot.kt:43`), то есть SysV shm эмулируется.
**Чем грозит.** Лаги: каждое обновление проходит через сокет X и через ptrace proot на каждом `read`/`write`.
**Правка.** Замерить FPS прокрутки в `xfce4-terminal` с `-noshm` и без него. Если без него x11vnc стартует (в `x11vnc.log` нет `XShmAttach failed`), убрать `-noshm`. Заодно проверить в `x11vnc.log`, что `-xd` распознан. У x11vnc нет опции с таким именем, есть `-xdamage` и `-noxdamage`. Если в логе `unrecognized option`, заменить на `-xdamage`.

### 4.11 Двойная запись логов и мёртвый код — `palma-session:3-6`, `:39-43`, `:144-159`, `:79-81`, `:182-193`
**Что не так.** После `exec >>/tmp/trace.log` функция `say` пишет каждую строку в `trace.log` дважды. `palma-session.log` обнуляется и больше не пишется. `try_vnc` и `tune_panel` не используются. Ветка Xvnc недостижима ни в одном образе.
**Правка.** В `say` оставить только `echo "$line"`. Удалить `LOG=` и `: > "$LOG"`, `try_vnc`, `tune_panel` и его вызов на `:354`. Ветку Xvnc либо удалить, либо ставить `tigervnc-standalone-server` в образы и держать её с правкой 4.2. Пустые строки `:52-55`, `:60` удалить.

---

## Десять главных правок по приоритету

1. **4.2** — `-localhost` у x11vnc и `-localhost=1` у Xvnc. Сейчас рабочий стол от root открыт без пароля всей Wi-Fi-сети.
2. **4.1** — Xvfb на `MAXW×MAXH` и поворот через `xrandr --fb` без убийства стола. Убирает потерю программ при повороте, 10–30 с чёрного экрана и большую часть гонок из 2.3.
3. **1.3 + 3.1** — копирование в Bitmap на потоке pump под блокировкой, чтение кадра только в `drawBehind`, убрать `frame` из `PalmaApp`. Главный источник лагов и рваных кадров.
4. **2.2 + 3.2** — одна очередь ввода с слиянием движений вместо корутины на событие; тап по порогу смещения. Убирает залипание кнопки и случайные перетаскивания.
5. **2.3** — дебаунс и одна отменяемая `geometryJob`, без ручного закрытия сокета и без `frames.value = null`.
6. **1.1 + 1.2** — порядок каналов `R G B` в `SetPixelFormat` и копирование строк целиком. Чинит цвета и ускоряет декодирование.
7. **2.1** — номер кадра `AtomicLong` вместо `currentTimeMillis`. Последний кадр перестаёт теряться.
8. **3.7 + 4.3** — Backspace и дубли букв от IME; восстановление `setxkbmap` после каждого старта X.
9. **1.4 + 1.5** — один Bitmap на сессию через `reconfigure`, удалить `grow()`. Нет 14,4 МБ мусора на поворот и ложных рестартов из-за `actual != want`.
10. **2.5 + 1.9** — диагностика из `cacheDir/proot` с верными именами логов; таймаут рукопожатия 5 с. Зависания перестают быть немыми.
