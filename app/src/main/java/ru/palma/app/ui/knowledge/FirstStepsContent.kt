package ru.palma.app.ui.knowledge

val firstSteps: KnowledgeSection = KnowledgeSection(
    id = "start",
    title = "Первые шаги",
    lead = "Из чего состоит Linux, как входить и выходить, где лежат файлы и где искать справку.",
    pro = false,
    branch = Branch.START,
    groups = listOf(
        KnowledgeGroup(
            title = "Как устроен Linux",
            entries = listOf(
                KnowledgeEntry(
                    title = "Дистрибутив",
                    summary = "Готовая сборка: ядро, системные программы, менеджер пакетов и настройки.",
                    detail = "Debian, Ubuntu, Arch и Fedora отличаются менеджером пакетов и частотой обновлений, команды оболочки у них общие.",
                    code = "cat /etc/os-release",
                ),
                KnowledgeEntry(
                    title = "Ядро",
                    summary = "Программа, которая управляет памятью, процессами и устройствами.",
                    detail = "Внутри proot ядро общее с Android: proot подменяет файловую систему, но своего ядра не запускает.",
                    code = "uname -r",
                ),
                KnowledgeEntry(
                    title = "Оболочка",
                    summary = "Программа, которая читает команды из терминала и запускает их.",
                    detail = "Чаще всего это bash, встречаются zsh и fish. Скрипты пишутся под конкретную оболочку.",
                    code = "echo \$SHELL",
                ),
                KnowledgeEntry(
                    title = "Терминал",
                    summary = "Окно, в котором работает оболочка: принимает ввод и показывает вывод.",
                    detail = "Терминалов может быть открыто несколько, в каждом своя оболочка со своим текущим каталогом.",
                ),
                KnowledgeEntry(
                    title = "root",
                    summary = "Суперпользователь, которому разрешено всё, включая удаление системы.",
                    detail = "Приглашение # вместо \$ означает, что оболочка работает от root. Внутри proot root ненастоящий и не получает прав Android.",
                    code = "id -u",
                ),
                KnowledgeEntry(
                    title = "Домашний каталог",
                    summary = "Личный каталог пользователя, в нём хранятся файлы и настройки программ.",
                    detail = "Обозначается символом ~. Настройки лежат в скрытых файлах с точкой в начале имени.",
                    code = "echo ~",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Вход и выход",
            entries = listOf(
                KnowledgeEntry(
                    title = "login",
                    summary = "Запрашивает имя и пароль и открывает сеанс пользователя.",
                    detail = "Запускается системой на текстовой консоли, вручную вызывается редко.",
                ),
                KnowledgeEntry(
                    title = "exit",
                    summary = "Завершает текущую оболочку.",
                    detail = "Код выхода передаётся родительскому процессу: 0 означает успех.",
                    code = "exit",
                ),
                KnowledgeEntry(
                    title = "logout",
                    summary = "Завершает оболочку входа.",
                    detail = "В обычной, не входной оболочке выдаёт ошибку, там работает только exit.",
                    code = "logout",
                ),
                KnowledgeEntry(
                    title = "reboot",
                    summary = "Перезагружает систему.",
                    detail = "Требует прав root. В proot не действует: перезагрузить можно только сам телефон.",
                    code = "sudo reboot",
                ),
                KnowledgeEntry(
                    title = "poweroff",
                    summary = "Выключает систему.",
                    detail = "shutdown -h +10 выключает через 10 минут, shutdown -c отменяет. В proot не действует.",
                    code = "sudo poweroff",
                ),
                KnowledgeEntry(
                    title = "Ctrl+C",
                    summary = "Прерывает команду, которая сейчас выполняется.",
                    detail = "Отправляет процессу сигнал SIGINT. Программа может его перехватить и завершиться не сразу.",
                ),
                KnowledgeEntry(
                    title = "Ctrl+D",
                    summary = "Передаёт конец ввода, в пустой строке оболочки закрывает её.",
                    detail = "В cat, python и других программах, читающих ввод, означает «ввод окончен».",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Где что лежит",
            entries = listOf(
                KnowledgeEntry(
                    title = "/",
                    summary = "Корень файловой системы, от него начинаются все пути.",
                    detail = "Дисков C: и D: нет: всё подключается в каталоги внутри одного дерева.",
                    code = "ls /",
                ),
                KnowledgeEntry(
                    title = "/etc",
                    summary = "Настройки системы и служб в текстовых файлах.",
                    detail = "Правка файлов в /etc требует прав root. Перед правкой полезна копия файла.",
                    code = "ls /etc",
                ),
                KnowledgeEntry(
                    title = "/home",
                    summary = "Домашние каталоги пользователей, по одному на каждого.",
                    detail = "Домашний каталог root лежит отдельно, в /root.",
                    code = "ls /home",
                ),
                KnowledgeEntry(
                    title = "/var/log",
                    summary = "Журналы системы и программ.",
                    detail = "Старые журналы сжимаются в .gz и удаляются по расписанию logrotate.",
                    code = "ls -lh /var/log",
                ),
                KnowledgeEntry(
                    title = "/usr/bin",
                    summary = "Исполняемые файлы программ, установленных из пакетов.",
                    detail = "Программы, собранные вручную, принято класть в /usr/local/bin.",
                    code = "ls /usr/bin | wc -l",
                ),
                KnowledgeEntry(
                    title = "/tmp",
                    summary = "Временные файлы, которые не нужны после перезапуска.",
                    detail = "Каталог открыт на запись всем, но удалить чужой файл нельзя благодаря sticky bit.",
                    code = "ls -ld /tmp",
                ),
                KnowledgeEntry(
                    title = "/opt",
                    summary = "Сторонние программы, которые ставятся одним каталогом.",
                    detail = "Пакетный менеджер сюда не пишет, поэтому удалять такие программы приходится вручную.",
                    code = "ls /opt",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Справка",
            entries = listOf(
                KnowledgeEntry(
                    title = "man",
                    summary = "Открывает страницу руководства по команде.",
                    detail = "Внутри работает поиск через /, выход по q. Число перед именем выбирает раздел: 5 — форматы файлов.",
                    code = "man 5 crontab",
                ),
                KnowledgeEntry(
                    title = "man -k",
                    summary = "Ищет страницы руководства по слову в описании.",
                    detail = "Если ничего не находится, базу страниц обновляет sudo mandb.",
                    code = "man -k archive",
                ),
                KnowledgeEntry(
                    title = "--help",
                    summary = "Выводит краткий список флагов команды.",
                    detail = "Работает почти у всех программ и быстрее man, но без подробностей.",
                    code = "tar --help | less",
                ),
                KnowledgeEntry(
                    title = "apropos",
                    summary = "То же, что man -k: находит команды по ключевому слову.",
                    code = "apropos partition",
                ),
                KnowledgeEntry(
                    title = "info",
                    summary = "Открывает подробную документацию GNU с разделами и ссылками.",
                    detail = "Для coreutils, bash и tar info-страницы полнее, чем man.",
                    code = "info coreutils",
                ),
                KnowledgeEntry(
                    title = "tldr",
                    summary = "Показывает несколько частых примеров использования команды.",
                    detail = "Ставится отдельным пакетом tldr и при первом запуске скачивает базу примеров.",
                    code = "tldr tar",
                ),
            ),
        ),
    ),
)
