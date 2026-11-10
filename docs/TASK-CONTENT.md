# Задача: контент обучающих разделов Matrux

Ты пишешь **только тексты и данные** на Kotlin для Android-приложения Matrux (`ru.palma.app`).
Пакет: `ru.palma.app.ui.knowledge`. Язык всего текста — **русский**.

## Ровно пять файлов, больше ничего не трогать

Создать (новые файлы, существующие не редактировать):

```
app/src/main/java/ru/palma/app/ui/knowledge/LanguagesContent.kt
app/src/main/java/ru/palma/app/ui/knowledge/LinuxContent.kt
app/src/main/java/ru/palma/app/ui/knowledge/TermsContent.kt
app/src/main/java/ru/palma/app/ui/knowledge/HardwareContent.kt
app/src/main/java/ru/palma/app/ui/knowledge/GitContent.kt
```

Каждый файл содержит **одну** верхнеуровневую `val` с точным именем:

| Файл | Объявление |
|---|---|
| LanguagesContent.kt | `val languages: KnowledgeSection = KnowledgeSection(...)` |
| LinuxContent.kt | `val linux: KnowledgeSection = KnowledgeSection(...)` |
| TermsContent.kt | `val terms: KnowledgeSection = KnowledgeSection(...)` |
| HardwareContent.kt | `val hardware: KnowledgeSection = KnowledgeSection(...)` |
| GitContent.kt | `val git: KnowledgeSection = KnowledgeSection(...)` |

**Никаких** `package`-различий: во всех файлах первая строка `package ru.palma.app.ui.knowledge`.
Импорты не нужны — модель лежит в том же пакете. Никаких комментариев в коде.

## Модель (уже существует, менять её нельзя)

```kotlin
package ru.palma.app.ui.knowledge

data class KnowledgeEntry(
    val title: String,
    val summary: String,
    val detail: String? = null,
    val code: String? = null,
)

data class KnowledgeGroup(
    val title: String,
    val entries: List<KnowledgeEntry>,
)

data class KnowledgeSection(
    val id: String,
    val title: String,
    val lead: String,
    val pro: Boolean,
    val groups: List<KnowledgeGroup>,
)
```

Правила полей:
- `title` — сама команда для Linux/Git (`ls -la`, `git commit`) или термин (`Ядро`) или понятие (`Оперативная память`) или конструкция языка (`list — список`).
- `summary` — **одна короткая фраза**, что это делает. Без «данная команда позволяет». Пример: «Показывает файлы в каталоге, включая скрытые».
- `detail` — 1–2 фразы пользы: ключевые флаги, подводный камень, аналогия. Может быть `null`.
- `code` — один готовый к запуску пример **без** обратных кавычек и без переносов строк внутри примера Linux/Git. Для языков — короткий осмысленный фрагмент, здесь переносы строк допустимы через `\n`. Нет естественного примера — `null`.
- Никаких эмодзи. Никаких «изучайте», «важно отметить», «в современном мире». Пиши как хорошая шпаргалка: сухо, точно, по делу. Тон — как в документации Apple: спокойный, уверенный, без восклицаний.

## Пять разделов: id, title, lead, pro

1. `languages` — «Базовые знания языков программирования», `pro = true`,
   `lead` = «Синтаксис и инструменты трёх языков, на которых пишут серверы, CLI и веб.»
   Группы: **Python**, **Go**, **Node.js**. В каждой 12–16 записей: где запускается код, типы, переменные,
   условия, циклы, функции, коллекции, модули/импорты, менеджер пакетов, запуск/сборка, работа с файлами,
   обработка ошибок, асинхронность, типичная точка входа, самые полезные команды терминала языка
   (`python3 -m venv`, `go run`, `npm init -y` и т. п.). Примеры кода — настоящие и рабочие.

2. `linux` — «Команды Linux», `pro = false`,
   `lead` = «Всё, что нужно в терминале: от навигации до сети и пакетов.»
   Группы строго в этом порядке: **Основные**, **Файлы и каталоги**, **Текст и поиск**, **Права доступа**,
   **Процессы**, **Диски и тома**, **Сеть**, **Пакеты и обновления**, **Пользователи и система**, **Архивы и передача файлов**, **Терминал и продуктивность**.
   В «Основные» — 12 самых нужных команд (`pwd`, `ls`, `cd`, `cp`, `mv`, `rm`, `mkdir`, `cat`, `grep`, `chmod`, `man`, `sudo`).
   Дальше — по 6–10 реальных команд в группе, у каждой настоящие флаги. Примеры команд — рабочие, из типичной Ubuntu/Debian.

3. `terms` — «Основные термины», `pro = false`,
   `lead` = «Слова, без которых не читается ни одна инструкция по системе.»
   Группы: **Система**, **Файлы и хранилище**, **Права и безопасность**, **Сеть**, **Запуск и службы**.
   Всего 35–45 терминов, касающихся операционной системы в целом: ядро, дистрибутив, оболочка, терминал, консоль,
   демон, служба, systemd, пакет, репозиторий, зависимость, монтирование, точка монтирования, файловая система,
   раздел, inode, суперпользователь, sudo, права, владелец, группа, разрешение, порт, сокет, IP, маска подсети,
   шлюз, DNS, SSH, PID, сигнал, переменная окружения, PATH, stdout/stderr, конвейер, стандартный поток,
   символическая ссылка, жёсткая ссылка, ядро vs userspace, образ, контейнер, виртуальная машина, буфер, кэш, дрова/драйвер, живой USB.
   `code` — короткий пример, где термин встречается в команде (например `echo $PATH`), либо `null`.

4. `hardware` — «Устройство компьютера», `pro = true`,
   `lead` = «Из чего собран компьютер и как части разговаривают между собой.»
   Группы: **Процессор и память**, **Хранение**, **Материнская плата и шины**, **Видео и вывод**, **Питание и корпус**, **Подключения**.
   Всего 30–40 записей: процессор, ядро CPU, поток, гиперпоточность, частота, кэш L1/L2/L3, архитектуры ARM/x86,
   TDP, оперативная память, двухканальный режим, DDR, ПЗУ, BIOS/UEFI, SSD, HDD, NVMe, SATA, M.2, RAID, TRIM,
   сектор, материнская плата, чипсет, PCIe, USB, Thunderbolt, NVMe-линия, GPU, видеопамять, DPU, HDMI/DisplayPort,
   блок питания, ватт, 80 Plus, охлаждение, теплотрубка, термопаста, корпус, продув, разъём, Ethernet, Wi-Fi.
   Для понятий, которые проверяются командой, давай `code` (`lscpu`, `free -h`, `df -h`, `lsblk`, `lsusb`, `lspci`).

5. `git` — «Основные команды git», `pro = true`,
   `lead` = «История, ветки и совместная работа — от первого коммита до rebase.»
   Группы: **Начало работы**, **Ежедневные**, **Ветки**, **Совместная работа**, **История и откат**, **Служебное**.
   30–36 команд с реальными флагами: `git init`, `git clone`, `git status`, `git add`, `git commit -m`,
   `git log --oneline --graph`, `git diff`, `git show`, `git branch`, `git switch -c`, `git checkout`, `git merge`,
   `git rebase -i`, `git remote -v`, `git push -u`, `git pull --rebase`, `git fetch`, `git tag`, `git stash`, `git restore`,
   `git reset --soft/--hard`, `git revert`, `git cherry-pick`, `git blame`, `git config --global user.name`,
   `git remote add`, `git push origin --delete`, `git clean -fd`, `git reflog`, `git switch`, `git log --author`,
   `git rebase --continue`, `git commit --amend`, `.gitignore`, `git bisect`.

## Объём и качество

- Суммарно **не меньше 240 записей** по всем пяти разделам.
- Все команды настоящие и проверяемые: если сомневаешься в флагах — проверь на этой машине
  (`man`, `--help`, `lsblk --help`) либо не включай.
- Никаких заглушек, повторяющихся описаний и текстов-наполнителей. Каждая `summary` уникальна.
- Одинаковый стиль ударения: «Удаляет» / «Показывает» / «Создаёт», а не «можно удалить».
- Длина `summary` — до 90 символов. `detail` — до 200 символов.
- Кавычки внутри строк экранируй корректно; иначе используй „ёлочки“ или одиночные апострофы.

## Проверка перед отчётом

Обязательно прогнать и показать вывод:

```
grep -c 'KnowledgeEntry(' app/src/main/java/ru/palma/app/ui/knowledge/*.kt
grep -rn 'TODO\|FIXME\|//' app/src/main/java/ru/palma/app/ui/knowledge/
```

Вторая команда должна вернуть **пусто** — ни одного комментария. Если компилятор Kotlin доступен
(`./gradlew compileDebugKotlin`), прогони его; если Gradle съест больше 5 минут — не жди, просто убедись,
что файлы синтаксически ровные.

В отчёте: сколько записей в каждом файле, какие команды проверял лично и как именно.
