package ru.palma.app.ui.knowledge

val services: KnowledgeSection = KnowledgeSection(
    id = "services",
    title = "Службы и процессы",
    lead = "Запуск и остановка служб, работа с процессами, расписания и ограничения ресурсов.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "systemctl",
            entries = listOf(
                KnowledgeEntry(
                    title = "systemctl status",
                    summary = "Показывает состояние службы, её PID и последние строки журнала.",
                    detail = "systemd в proot не запускается, поэтому systemctl там отвечает ошибкой. Команды раздела относятся к серверу.",
                    code = "systemctl status nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl start",
                    summary = "Запускает службу сейчас.",
                    detail = "После перезагрузки служба не поднимется, если не включена через enable.",
                    code = "sudo systemctl start nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl stop",
                    summary = "Останавливает службу.",
                    code = "sudo systemctl stop nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl restart",
                    summary = "Останавливает и заново запускает службу.",
                    code = "sudo systemctl restart nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl reload",
                    summary = "Перечитывает настройки службы без остановки.",
                    detail = "Работает, только если служба это поддерживает. reload-or-restart выбирает сам.",
                    code = "sudo systemctl reload nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl enable",
                    summary = "Включает автозапуск службы при загрузке.",
                    detail = "--now заодно запускает её сразу.",
                    code = "sudo systemctl enable --now nginx",
                ),
                KnowledgeEntry(
                    title = "systemctl disable",
                    summary = "Отключает автозапуск службы.",
                    detail = "Работающую службу не останавливает. mask запрещает запуск совсем, даже вручную.",
                    code = "sudo systemctl disable --now cups",
                ),
                KnowledgeEntry(
                    title = "systemctl list-units --failed",
                    summary = "Перечисляет службы, которые упали.",
                    code = "systemctl list-units --failed",
                ),
                KnowledgeEntry(
                    title = "systemctl is-active",
                    summary = "Выводит active или inactive и возвращает код для скриптов.",
                    code = "systemctl is-active --quiet nginx",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Процессы",
            entries = listOf(
                KnowledgeEntry(
                    title = "ps aux",
                    summary = "Выводит все процессы с владельцем, PID, долей CPU и памяти.",
                    detail = "--sort=-%cpu сортирует по нагрузке на процессор.",
                    code = "ps aux --sort=-%cpu | head",
                ),
                KnowledgeEntry(
                    title = "top",
                    summary = "Показывает процессы в реальном времени, самые тяжёлые сверху.",
                    detail = "M сортирует по памяти, P — по процессору, k завершает процесс, q — выход.",
                    code = "top",
                ),
                KnowledgeEntry(
                    title = "htop",
                    summary = "Интерактивный top с цветом, деревом процессов и прокруткой.",
                    detail = "F5 показывает дерево, F9 отправляет сигнал. Ставится отдельным пакетом.",
                    code = "htop -u anna",
                ),
                KnowledgeEntry(
                    title = "pgrep",
                    summary = "Находит PID процессов по имени.",
                    detail = "-a выводит полную командную строку, -f ищет по ней, а не только по имени.",
                    code = "pgrep -af python",
                ),
                KnowledgeEntry(
                    title = "kill -15",
                    summary = "Просит процесс завершиться сигналом SIGTERM.",
                    detail = "Процесс успевает сохранить данные и закрыть файлы. kill без флага отправляет именно этот сигнал.",
                    code = "kill -15 4821",
                ),
                KnowledgeEntry(
                    title = "kill -9",
                    summary = "Немедленно убивает процесс сигналом SIGKILL.",
                    detail = "Перехватить его нельзя: несохранённые данные теряются, временные файлы остаются. Применяется, когда -15 не помог.",
                    code = "kill -9 4821",
                ),
                KnowledgeEntry(
                    title = "pkill -f",
                    summary = "Завершает процессы, в командной строке которых есть шаблон.",
                    detail = "Шаблон может совпасть с чужими процессами. Список совпадений заранее показывает pgrep -af.",
                    code = "pkill -f \"node server.js\"",
                ),
                KnowledgeEntry(
                    title = "nice",
                    summary = "Запускает команду с пониженным приоритетом.",
                    detail = "Шкала от -20 до 19, больше — уступчивее. Отрицательные значения требуют root.",
                    code = "nice -n 10 tar -czf home.tar.gz ~",
                ),
                KnowledgeEntry(
                    title = "renice",
                    summary = "Меняет приоритет уже работающего процесса.",
                    code = "renice -n 15 -p 4821",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Расписание",
            entries = listOf(
                KnowledgeEntry(
                    title = "systemctl list-timers",
                    summary = "Показывает таймеры systemd, время прошлого и следующего запуска.",
                    code = "systemctl list-timers --all",
                ),
                KnowledgeEntry(
                    title = "crontab -e",
                    summary = "Открывает расписание текущего пользователя в редакторе.",
                    detail = "Строка: минута, час, день месяца, месяц, день недели, команда. В proot cron запускается вручную: sudo cron.",
                    code = "crontab -e",
                ),
                KnowledgeEntry(
                    title = "crontab -l",
                    summary = "Выводит расписание текущего пользователя.",
                    detail = "crontab -r удаляет расписание целиком без подтверждения.",
                    code = "crontab -l",
                ),
                KnowledgeEntry(
                    title = "Строка cron",
                    summary = "Пять полей времени и команда.",
                    detail = "*/15 — каждые 15 единиц, 1-5 — диапазон, 1,3 — список. Окружение cron минимальное, пути нужны полные.",
                    code = "30 3 * * 1-5 /home/anna/bin/backup.sh",
                ),
                KnowledgeEntry(
                    title = "@reboot",
                    summary = "Запускает команду один раз после старта cron.",
                    detail = "Есть и другие сокращения: @hourly, @daily, @weekly.",
                    code = "@reboot /home/anna/bin/start-bot.sh",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Своя служба",
            entries = listOf(
                KnowledgeEntry(
                    title = "[Unit]",
                    summary = "Описание службы и порядок запуска относительно других.",
                    detail = "Файл кладётся в /etc/systemd/system/имя.service. After= задаёт, после чего стартовать.",
                    code = "After=network-online.target",
                ),
                KnowledgeEntry(
                    title = "[Service]",
                    summary = "Команда запуска, пользователь и поведение при падении.",
                    detail = "ExecStart требует полный путь. User= запускает не от root.",
                    code = "ExecStart=/usr/bin/python3 /opt/bot/bot.py",
                ),
                KnowledgeEntry(
                    title = "Restart=on-failure",
                    summary = "Перезапускает службу, если она завершилась с ошибкой.",
                    detail = "RestartSec= задаёт паузу перед перезапуском, по умолчанию 100 мс.",
                    code = "RestartSec=5",
                ),
                KnowledgeEntry(
                    title = "[Install]",
                    summary = "Указывает, к какой цели привязать службу при enable.",
                    detail = "multi-user.target — обычная загрузка сервера без графики.",
                    code = "WantedBy=multi-user.target",
                ),
                KnowledgeEntry(
                    title = "systemctl daemon-reload",
                    summary = "Перечитывает юнит-файлы после создания или правки.",
                    detail = "Без него systemd продолжает использовать старую версию файла.",
                    code = "sudo systemctl daemon-reload",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Ресурсы",
            entries = listOf(
                KnowledgeEntry(
                    title = "ulimit -a",
                    summary = "Показывает лимиты текущей оболочки: файлы, процессы, стек.",
                    code = "ulimit -a",
                ),
                KnowledgeEntry(
                    title = "ulimit -n",
                    summary = "Показывает или меняет лимит открытых файлов.",
                    detail = "Действует только на текущую оболочку и её потомков. Поднять выше жёсткого лимита может только root.",
                    code = "ulimit -n 4096",
                ),
                KnowledgeEntry(
                    title = "systemd-run --scope",
                    summary = "Запускает команду с ограничением памяти или процессора.",
                    detail = "При превышении MemoryMax процесс убивается. CPUQuota=50% ограничивает процессор половиной ядра.",
                    code = "systemd-run --scope -p MemoryMax=1G make -j4",
                ),
                KnowledgeEntry(
                    title = "timeout",
                    summary = "Завершает команду, если она работает дольше заданного времени.",
                    detail = "Код выхода 124 означает, что время вышло. -s KILL отправляет SIGKILL вместо SIGTERM.",
                    code = "timeout 30s curl -s https://example.com",
                ),
            ),
        ),
    ),
)
