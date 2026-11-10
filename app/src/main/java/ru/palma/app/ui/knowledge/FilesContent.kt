package ru.palma.app.ui.knowledge

val files: KnowledgeSection = KnowledgeSection(
    id = "files",
    title = "Файлы и каталоги",
    lead = "Перемещение по каталогам, создание и удаление, просмотр, поиск и архивы.",
    pro = false,
    branch = Branch.START,
    groups = listOf(
        KnowledgeGroup(
            title = "Навигация",
            entries = listOf(
                KnowledgeEntry(
                    title = "pwd",
                    summary = "Показывает полный путь текущего каталога.",
                    code = "pwd",
                ),
                KnowledgeEntry(
                    title = "cd",
                    summary = "Переходит в указанный каталог.",
                    detail = "cd без аргументов ведёт в домашний каталог, cd .. поднимает на уровень выше.",
                    code = "cd /var/log",
                ),
                KnowledgeEntry(
                    title = "cd -",
                    summary = "Возвращает в предыдущий каталог.",
                    detail = "Повторный вызов переключает обратно, удобно для работы в двух каталогах.",
                    code = "cd -",
                ),
                KnowledgeEntry(
                    title = "ls -la",
                    summary = "Показывает все файлы, включая скрытые, с правами, владельцем и датой.",
                    detail = "Скрытыми считаются файлы, имя которых начинается с точки.",
                    code = "ls -la ~",
                ),
                KnowledgeEntry(
                    title = "ls -lh",
                    summary = "Показывает размеры файлов в К, М и Г вместо байтов.",
                    detail = "-t сортирует по времени изменения, -S — по размеру, -r переворачивает порядок.",
                    code = "ls -lhS ~/Downloads",
                ),
                KnowledgeEntry(
                    title = "tree",
                    summary = "Рисует дерево каталогов с вложенными файлами.",
                    detail = "-L 2 ограничивает глубину, -d показывает только каталоги. Ставится отдельным пакетом.",
                    code = "tree -L 2 ~/projects",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Создать и удалить",
            entries = listOf(
                KnowledgeEntry(
                    title = "mkdir -p",
                    summary = "Создаёт каталог вместе со всеми недостающими родительскими.",
                    detail = "Без -p команда падает, если родителя нет, и ругается на уже существующий каталог.",
                    code = "mkdir -p ~/projects/site/assets",
                ),
                KnowledgeEntry(
                    title = "touch",
                    summary = "Создаёт пустой файл или обновляет время изменения существующего.",
                    code = "touch notes.txt",
                ),
                KnowledgeEntry(
                    title = "rm",
                    summary = "Удаляет файлы без корзины.",
                    detail = "Восстановить удалённое штатными средствами нельзя. -i спрашивает подтверждение на каждый файл.",
                    code = "rm -i old-*.log",
                ),
                KnowledgeEntry(
                    title = "rm -rf",
                    summary = "Удаляет каталог со всем содержимым без вопросов.",
                    detail = "Опечатка в пути или пустая переменная в rm -rf \$DIR/ стирает не тот каталог. Путь стоит проверить через ls.",
                    code = "rm -rf ~/projects/site/node_modules",
                ),
                KnowledgeEntry(
                    title = "rmdir",
                    summary = "Удаляет только пустой каталог.",
                    detail = "Отказ на непустом каталоге защищает от случайного удаления файлов.",
                    code = "rmdir ~/empty",
                ),
                KnowledgeEntry(
                    title = "cp -r",
                    summary = "Копирует каталог со всем содержимым.",
                    detail = "-a вместо -r сохраняет права, владельца и даты. Слэш в конце источника влияет на результат только у rsync.",
                    code = "cp -r ~/site ~/site-backup",
                ),
                KnowledgeEntry(
                    title = "mv",
                    summary = "Перемещает или переименовывает файл и каталог.",
                    detail = "Существующая цель перезаписывается без вопроса. -n не трогает существующие файлы.",
                    code = "mv draft.md posts/2026-11-09.md",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Смотреть содержимое",
            entries = listOf(
                KnowledgeEntry(
                    title = "cat",
                    summary = "Выводит содержимое файла целиком.",
                    detail = "-n нумерует строки. Для больших файлов удобнее less.",
                    code = "cat -n /etc/hostname",
                ),
                KnowledgeEntry(
                    title = "less",
                    summary = "Показывает файл постранично с прокруткой и поиском.",
                    detail = "/ ищет вперёд, n — следующее совпадение, G — конец файла, q — выход.",
                    code = "less /var/log/dpkg.log",
                ),
                KnowledgeEntry(
                    title = "head",
                    summary = "Показывает первые строки файла, по умолчанию 10.",
                    code = "head -n 20 /etc/passwd",
                ),
                KnowledgeEntry(
                    title = "tail",
                    summary = "Показывает последние строки файла.",
                    detail = "-f продолжает выводить новые строки по мере записи, -F переживает ротацию файла.",
                    code = "tail -n 50 /var/log/syslog",
                ),
                KnowledgeEntry(
                    title = "wc -l",
                    summary = "Считает строки в файле.",
                    detail = "-w считает слова, -c — байты. Без флагов выводит всё сразу.",
                    code = "wc -l /etc/passwd",
                ),
                KnowledgeEntry(
                    title = "file",
                    summary = "Определяет тип файла по содержимому, а не по расширению.",
                    code = "file /usr/bin/ls",
                ),
                KnowledgeEntry(
                    title = "stat",
                    summary = "Показывает размер, права, владельца и три метки времени файла.",
                    detail = "Access — последнее чтение, Modify — изменение содержимого, Change — изменение прав или имени.",
                    code = "stat ~/.bashrc",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Поиск",
            entries = listOf(
                KnowledgeEntry(
                    title = "find -name",
                    summary = "Ищет файлы по имени в каталоге и всех вложенных.",
                    detail = "Шаблон берётся в кавычки, иначе оболочка раскроет его заранее. -iname не различает регистр.",
                    code = "find ~ -name \"*.pdf\"",
                ),
                KnowledgeEntry(
                    title = "find -mtime",
                    summary = "Ищет файлы по дате изменения в сутках.",
                    detail = "-mtime -1 — изменённые за последние сутки, +30 — старше 30 дней. -mmin считает в минутах.",
                    code = "find /var/log -mtime -1",
                ),
                KnowledgeEntry(
                    title = "find -type",
                    summary = "Ограничивает поиск файлами или каталогами.",
                    detail = "f — обычные файлы, d — каталоги, l — символические ссылки.",
                    code = "find ~/projects -type d -name .git",
                ),
                KnowledgeEntry(
                    title = "locate",
                    summary = "Мгновенно ищет файл по имени в заранее собранной базе.",
                    detail = "Новые файлы появляются в базе после sudo updatedb. Ставится пакетом plocate.",
                    code = "locate sshd_config",
                ),
                KnowledgeEntry(
                    title = "which",
                    summary = "Показывает, какой исполняемый файл запустится по имени команды.",
                    code = "which python3",
                ),
                KnowledgeEntry(
                    title = "whereis",
                    summary = "Находит исполняемый файл, исходники и страницу man команды.",
                    code = "whereis tar",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Архивы и ссылки",
            entries = listOf(
                KnowledgeEntry(
                    title = "tar -czf",
                    summary = "Упаковывает каталог в сжатый архив .tar.gz.",
                    detail = "c — создать, z — сжать gzip, f — имя архива. -J вместо -z сжимает xz сильнее, но медленнее.",
                    code = "tar -czf site.tar.gz ~/site",
                ),
                KnowledgeEntry(
                    title = "tar -xzf",
                    summary = "Распаковывает архив .tar.gz.",
                    detail = "-C указывает каталог для распаковки, по умолчанию — текущий.",
                    code = "tar -xzf site.tar.gz -C ~/restore",
                ),
                KnowledgeEntry(
                    title = "tar -tzf",
                    summary = "Показывает список файлов в архиве без распаковки.",
                    code = "tar -tzf site.tar.gz | head",
                ),
                KnowledgeEntry(
                    title = "zip -r",
                    summary = "Упаковывает каталог в .zip, который открывается в любой системе.",
                    code = "zip -r photos.zip ~/photos",
                ),
                KnowledgeEntry(
                    title = "unzip",
                    summary = "Распаковывает архив .zip.",
                    detail = "-l показывает содержимое, -d задаёт каталог назначения.",
                    code = "unzip photos.zip -d ~/photos",
                ),
                KnowledgeEntry(
                    title = "ln -s",
                    summary = "Создаёт символическую ссылку на файл или каталог.",
                    detail = "Сначала указывается цель, потом имя ссылки. Если цель удалить, ссылка останется битой.",
                    code = "ln -s /var/log/nginx ~/nginx-logs",
                ),
            ),
        ),
    ),
)
