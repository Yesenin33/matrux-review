package ru.palma.app.ui.knowledge

val logs: KnowledgeSection = KnowledgeSection(
    id = "logs",
    title = "Логи и поиск",
    lead = "Чтение журналов, поиск по тексту и разбор вывода конвейерами.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "journalctl",
            entries = listOf(
                KnowledgeEntry(
                    title = "journalctl -u",
                    summary = "Показывает журнал одной службы.",
                    detail = "Журнал systemd ведётся только там, где systemd запущен, в proot его нет.",
                    code = "journalctl -u nginx",
                ),
                KnowledgeEntry(
                    title = "journalctl -f",
                    summary = "Выводит новые записи по мере появления.",
                    code = "journalctl -fu nginx",
                ),
                KnowledgeEntry(
                    title = "journalctl -n",
                    summary = "Показывает последние N записей.",
                    detail = "--no-pager выводит сразу в терминал, удобно для копирования.",
                    code = "journalctl -u ssh -n 100 --no-pager",
                ),
                KnowledgeEntry(
                    title = "journalctl --since",
                    summary = "Ограничивает журнал временем.",
                    detail = "Понимает даты, \"1 hour ago\", today и yesterday. --until задаёт конец интервала.",
                    code = "journalctl --since \"1 hour ago\"",
                ),
                KnowledgeEntry(
                    title = "journalctl -p err",
                    summary = "Оставляет только записи с приоритетом err и серьёзнее.",
                    detail = "Уровни по убыванию: emerg, alert, crit, err, warning, notice, info, debug.",
                    code = "journalctl -p err -b",
                ),
                KnowledgeEntry(
                    title = "journalctl -b",
                    summary = "Показывает записи с текущей загрузки.",
                    detail = "-b -1 — с предыдущей, удобно после внезапной перезагрузки.",
                    code = "journalctl -b -1 -n 50",
                ),
                KnowledgeEntry(
                    title = "journalctl --disk-usage",
                    summary = "Показывает, сколько места занимает журнал.",
                    code = "journalctl --disk-usage",
                ),
                KnowledgeEntry(
                    title = "journalctl --vacuum-size",
                    summary = "Удаляет старые записи, пока журнал не станет меньше заданного размера.",
                    detail = "--vacuum-time=2weeks удаляет записи старше двух недель.",
                    code = "sudo journalctl --vacuum-size=500M",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Файлы логов",
            entries = listOf(
                KnowledgeEntry(
                    title = "/var/log/syslog",
                    summary = "Общий журнал системы в Debian и Ubuntu.",
                    detail = "В Fedora и Arch вместо него journalctl, файла может не быть.",
                    code = "sudo tail -n 100 /var/log/syslog",
                ),
                KnowledgeEntry(
                    title = "/var/log/auth.log",
                    summary = "Входы, sudo и попытки подбора пароля ssh.",
                    code = "sudo grep \"Failed password\" /var/log/auth.log",
                ),
                KnowledgeEntry(
                    title = "/var/log/dpkg.log",
                    summary = "История установки, обновления и удаления пакетов.",
                    code = "grep \" install \" /var/log/dpkg.log",
                ),
                KnowledgeEntry(
                    title = "dmesg",
                    summary = "Выводит сообщения ядра: устройства, драйверы, нехватку памяти.",
                    detail = "-T показывает обычное время, -w ждёт новых сообщений.",
                    code = "sudo dmesg -T | tail -n 30",
                ),
                KnowledgeEntry(
                    title = "tail -f",
                    summary = "Выводит новые строки файла в реальном времени.",
                    detail = "-F продолжает следить после ротации, когда файл пересоздаётся.",
                    code = "tail -F /var/log/nginx/error.log",
                ),
                KnowledgeEntry(
                    title = "zgrep",
                    summary = "Ищет в сжатых старых логах .gz без распаковки.",
                    detail = "zcat выводит такой файл целиком, zless открывает постранично.",
                    code = "zgrep -h \"error\" /var/log/syslog.*.gz",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "grep",
            entries = listOf(
                KnowledgeEntry(
                    title = "grep -rn",
                    summary = "Ищет текст во всех файлах каталога и выводит номера строк.",
                    code = "grep -rn \"TODO\" ~/projects/site",
                ),
                KnowledgeEntry(
                    title = "grep -i",
                    summary = "Ищет без учёта регистра.",
                    code = "grep -i \"timeout\" /var/log/syslog",
                ),
                KnowledgeEntry(
                    title = "grep -v",
                    summary = "Выводит строки, в которых шаблона нет.",
                    detail = "Удобно отсекать шум: комментарии, пустые строки, частые сообщения.",
                    code = "grep -v '^#' /etc/ssh/sshd_config",
                ),
                KnowledgeEntry(
                    title = "grep -C",
                    summary = "Добавляет строки контекста вокруг совпадения.",
                    detail = "-A N — строки после, -B N — до, -C N — с обеих сторон.",
                    code = "grep -C 3 \"panic\" app.log",
                ),
                KnowledgeEntry(
                    title = "grep --include",
                    summary = "Ограничивает рекурсивный поиск файлами по маске.",
                    detail = "--exclude-dir=node_modules пропускает каталог целиком.",
                    code = "grep -rn --include=\"*.kt\" \"Compose\" .",
                ),
                KnowledgeEntry(
                    title = "grep -c",
                    summary = "Выводит число совпавших строк вместо самих строк.",
                    code = "grep -c \"ERROR\" app.log",
                ),
                KnowledgeEntry(
                    title = "grep -E",
                    summary = "Включает расширенные регулярные выражения с | + ? и скобками.",
                    detail = "-o выводит только совпавшую часть строки, а не строку целиком.",
                    code = "grep -E \"error|fail\" app.log",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Разбор текста",
            entries = listOf(
                KnowledgeEntry(
                    title = "cut -d",
                    summary = "Вырезает поля из строк по разделителю.",
                    detail = "-f выбирает поля: -f1,3 — первое и третье, -f2- — со второго до конца.",
                    code = "cut -d: -f1,7 /etc/passwd",
                ),
                KnowledgeEntry(
                    title = "sort | uniq -c",
                    summary = "Считает, сколько раз встречается каждая строка.",
                    detail = "uniq схлопывает только соседние повторы, поэтому перед ним нужен sort.",
                    code = "sort access.log | uniq -c | sort -rn",
                ),
                KnowledgeEntry(
                    title = "awk",
                    summary = "Выводит нужные колонки и считает по ним.",
                    detail = "\$1, \$2 — поля через пробел, -F задаёт другой разделитель, \$NF — последнее поле.",
                    code = "awk '{print \$1}' /var/log/nginx/access.log",
                ),
                KnowledgeEntry(
                    title = "sed -i",
                    summary = "Заменяет текст прямо в файле.",
                    detail = "g в конце меняет все вхождения в строке. -i.bak сохраняет копию исходного файла.",
                    code = "sed -i.bak 's/8080/9090/g' config.ini",
                ),
                KnowledgeEntry(
                    title = "tr",
                    summary = "Заменяет или удаляет символы в потоке.",
                    detail = "-d удаляет символы, -s схлопывает повторы.",
                    code = "tr 'a-z' 'A-Z' < names.txt",
                ),
                KnowledgeEntry(
                    title = "xargs",
                    summary = "Передаёт строки из ввода как аргументы другой команде.",
                    detail = "С find -print0 и xargs -0 корректно обрабатываются имена с пробелами.",
                    code = "find . -name \"*.tmp\" -print0 | xargs -0 rm",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Конвейеры",
            entries = listOf(
                KnowledgeEntry(
                    title = "Частые адреса в логе",
                    summary = "Десять IP-адресов, с которых пришло больше всего запросов.",
                    code = "cut -d' ' -f1 access.log | sort | uniq -c | sort -rn | head",
                ),
                KnowledgeEntry(
                    title = "Подбор пароля ssh",
                    summary = "Адреса, с которых чаще всего не удавался вход по ssh.",
                    detail = "-P включает регулярные выражения Perl, \\K отбрасывает из вывода всё до адреса.",
                    code = "grep -oP 'Failed.*from \\K\\S+' auth.log | sort | uniq -c",
                ),
                KnowledgeEntry(
                    title = "Тяжёлые каталоги",
                    summary = "Десять самых больших подкаталогов, крупные внизу.",
                    detail = "sort -h понимает суффиксы К, М и Г в выводе du.",
                    code = "du -xh --max-depth=1 / 2>/dev/null | sort -h | tail",
                ),
                KnowledgeEntry(
                    title = "Коды ответа сервера",
                    summary = "Сколько ответов nginx пришлось на каждый HTTP-код: 200, 404, 500.",
                    detail = "В стандартном формате access.log код ответа стоит девятым полем.",
                    code = "awk '{print \$9}' access.log | sort | uniq -c | sort -rn",
                ),
            ),
        ),
    ),
)
