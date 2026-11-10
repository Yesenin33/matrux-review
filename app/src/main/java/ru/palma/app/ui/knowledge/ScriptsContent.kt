package ru.palma.app.ui.knowledge

val scripts: KnowledgeSection = KnowledgeSection(
    id = "scripts",
    title = "Скрипты",
    lead = "Скрипты на bash: переменные, условия, циклы, функции и готовые задачи для cron.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "Основа",
            entries = listOf(
                KnowledgeEntry(
                    title = "#!/usr/bin/env bash",
                    summary = "Первая строка скрипта, указывает, какой оболочкой его выполнять.",
                    detail = "env находит bash по PATH, поэтому строка работает и там, где bash лежит не в /bin.",
                    code = "#!/usr/bin/env bash",
                ),
                KnowledgeEntry(
                    title = "chmod +x",
                    summary = "Разрешает запускать скрипт как программу.",
                    code = "chmod +x backup.sh",
                ),
                KnowledgeEntry(
                    title = "./script.sh",
                    summary = "Запускает скрипт из текущего каталога.",
                    detail = "Без ./ оболочка ищет команду только в каталогах из PATH. bash script.sh запускает без права x.",
                    code = "./backup.sh",
                ),
                KnowledgeEntry(
                    title = "set -euo pipefail",
                    summary = "Останавливает скрипт на первой ошибке вместо продолжения.",
                    detail = "-e — выход при ошибке команды, -u — при пустой переменной, pipefail — при сбое внутри конвейера.",
                    code = "set -euo pipefail",
                ),
                KnowledgeEntry(
                    title = "#",
                    summary = "Всё после решётки до конца строки оболочка пропускает.",
                    detail = "Исключение — первая строка #!, которую читает система при запуске.",
                ),
                KnowledgeEntry(
                    title = "bash -x",
                    summary = "Выводит каждую команду скрипта перед выполнением.",
                    detail = "set -x внутри скрипта включает то же для отдельного участка, set +x выключает.",
                    code = "bash -x ./backup.sh",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Переменные и аргументы",
            entries = listOf(
                KnowledgeEntry(
                    title = "name=value",
                    summary = "Создаёт переменную.",
                    detail = "Пробелы вокруг = не допускаются. Значение читается через \$name, в кавычках — \"\$name\".",
                    code = "dir=\"\$HOME/site\"",
                ),
                KnowledgeEntry(
                    title = "\$1",
                    summary = "Первый аргумент, переданный скрипту или функции.",
                    detail = "\$2, \$3 — следующие, \$0 — имя самого скрипта.",
                    code = "echo \"Каталог: \$1\"",
                ),
                KnowledgeEntry(
                    title = "\$@",
                    summary = "Все аргументы списком.",
                    detail = "\"\$@\" в кавычках сохраняет аргументы с пробелами целыми.",
                    code = "for arg in \"\$@\"; do echo \"\$arg\"; done",
                ),
                KnowledgeEntry(
                    title = "\$#",
                    summary = "Количество переданных аргументов.",
                    code = "[[ \$# -eq 0 ]] && echo \"нужен путь\" && exit 1",
                ),
                KnowledgeEntry(
                    title = "\$?",
                    summary = "Код выхода последней команды: 0 — успех, остальное — ошибка.",
                    code = "echo \$?",
                ),
                KnowledgeEntry(
                    title = "\$(команда)",
                    summary = "Подставляет вывод команды в строку.",
                    detail = "date +%F выдаёт дату вида 2026-11-09, удобно для имён файлов.",
                    code = "today=\"\$(date +%F)\"",
                ),
                KnowledgeEntry(
                    title = "\${var:-default}",
                    summary = "Подставляет значение по умолчанию, если переменная пуста.",
                    code = "port=\"\${1:-8080}\"",
                ),
                KnowledgeEntry(
                    title = "read -p",
                    summary = "Спрашивает значение у пользователя и кладёт его в переменную.",
                    detail = "-r не трактует обратный слэш особо, -s скрывает ввод для паролей.",
                    code = "read -rp \"Имя сервера: \" host",
                ),
                KnowledgeEntry(
                    title = "export",
                    summary = "Делает переменную видимой для запускаемых программ.",
                    detail = "Без export переменная существует только в текущей оболочке.",
                    code = "export EDITOR=nano",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Ветвления и циклы",
            entries = listOf(
                KnowledgeEntry(
                    title = "if [[ ]]",
                    summary = "Выполняет блок, если условие истинно.",
                    detail = "Пробелы внутри скобок обязательны. Числа сравниваются через -eq, -lt, -gt, строки — через == и !=.",
                    code = "if [[ \$# -lt 1 ]]; then exit 1; fi",
                ),
                KnowledgeEntry(
                    title = "[[ -f ]]",
                    summary = "Проверяет, существует ли файл.",
                    detail = "-d — каталог, -e — что угодно, -s — непустой файл, -x — исполняемый.",
                    code = "[[ -f ~/.env ]] && source ~/.env",
                ),
                KnowledgeEntry(
                    title = "case",
                    summary = "Выбирает ветку по значению переменной.",
                    detail = "Каждая ветка заканчивается ;;, ветка *) срабатывает, если ничего не подошло.",
                    code = "case \"\$1\" in start) run ;; *) usage ;; esac",
                ),
                KnowledgeEntry(
                    title = "for f in *.log",
                    summary = "Повторяет блок для каждого файла по маске.",
                    detail = "Если файлов нет, в переменную попадёт сама строка *.log. shopt -s nullglob это отключает.",
                    code = "for f in *.log; do gzip \"\$f\"; done",
                ),
                KnowledgeEntry(
                    title = "while read -r line",
                    summary = "Читает файл построчно.",
                    detail = "IFS= сохраняет пробелы в начале и конце строки.",
                    code = "while IFS= read -r line; do echo \"\$line\"; done < hosts.txt",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Функции",
            entries = listOf(
                KnowledgeEntry(
                    title = "name() { }",
                    summary = "Объявляет функцию, которая вызывается по имени как команда.",
                    detail = "Функция объявляется выше места вызова. Слово function перед именем необязательно.",
                    code = "log() { echo \"\$(date +%T) \$*\"; }",
                ),
                KnowledgeEntry(
                    title = "Аргументы функции",
                    summary = "Внутри функции \$1, \$2 и \$@ относятся к её аргументам.",
                    code = "log \"копия готова\"",
                ),
                KnowledgeEntry(
                    title = "return",
                    summary = "Завершает функцию с кодом от 0 до 255.",
                    detail = "Возвращает только код, не значение. Результат передаётся через echo и \$(имя).",
                    code = "is_up() { curl -fsS \"\$1\" > /dev/null; }",
                ),
                KnowledgeEntry(
                    title = "local",
                    summary = "Объявляет переменную, видимую только внутри функции.",
                    detail = "Без local переменная функции перезаписывает одноимённую в остальном скрипте.",
                    code = "local dest=\"\$HOME/backup\"",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Практика",
            entries = listOf(
                KnowledgeEntry(
                    title = "Резервная копия с датой",
                    summary = "Упаковывает каталог в архив, в имени которого стоит сегодняшняя дата.",
                    code = "tar -czf ~/backup/site-\$(date +%F).tar.gz ~/site",
                ),
                KnowledgeEntry(
                    title = "Ротация старых копий",
                    summary = "Удаляет архивы старше семи дней.",
                    detail = "Перед -delete полезно запустить ту же команду без него и проверить список.",
                    code = "find ~/backup -name \"*.tar.gz\" -mtime +7 -delete",
                ),
                KnowledgeEntry(
                    title = "Проверка доступности",
                    summary = "Пишет в лог, если сайт не отвечает или отвечает ошибкой.",
                    detail = "-f считает ошибкой ответы 4xx и 5xx, -sS молчит, но выводит причину сбоя.",
                    code = "curl -fsS https://example.com > /dev/null || log down",
                ),
                KnowledgeEntry(
                    title = "Запуск каждый час",
                    summary = "Строка crontab, которая запускает скрипт в начале каждого часа.",
                    detail = "Вывод уходит в файл, иначе cron пытается отправить его почтой.",
                    code = "0 * * * * \$HOME/bin/check.sh >> \$HOME/check.log 2>&1",
                ),
                KnowledgeEntry(
                    title = "trap",
                    summary = "Выполняет команду при выходе из скрипта, в том числе по ошибке.",
                    detail = "Удобно для удаления временных файлов. EXIT срабатывает при любом завершении.",
                    code = "trap 'rm -f \"\$tmp\"' EXIT",
                ),
            ),
        ),
    ),
)
