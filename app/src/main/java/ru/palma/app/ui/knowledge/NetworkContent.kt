package ru.palma.app.ui.knowledge

val network: KnowledgeSection = KnowledgeSection(
    id = "network",
    title = "Сеть и ssh",
    lead = "Адреса, проверка связи, открытые порты, удалённый вход и файрвол.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "Свои адреса",
            entries = listOf(
                KnowledgeEntry(
                    title = "ip a",
                    summary = "Показывает сетевые интерфейсы и их IP-адреса.",
                    detail = "inet — IPv4, inet6 — IPv6. -br выводит по строке на интерфейс.",
                    code = "ip -br a",
                ),
                KnowledgeEntry(
                    title = "ip r",
                    summary = "Показывает таблицу маршрутов и шлюз по умолчанию.",
                    detail = "Строка default via указывает, куда уходит трафик в интернет.",
                    code = "ip r",
                ),
                KnowledgeEntry(
                    title = "ip link",
                    summary = "Показывает состояние интерфейсов: включён, MAC-адрес, MTU.",
                    code = "ip -br link",
                ),
                KnowledgeEntry(
                    title = "hostname -I",
                    summary = "Выводит все IP-адреса машины одной строкой.",
                    code = "hostname -I",
                ),
                KnowledgeEntry(
                    title = "/etc/hosts",
                    summary = "Сопоставляет имена и адреса до обращения к DNS.",
                    detail = "Строка 127.0.0.1 site.local направляет имя на свою машину.",
                    code = "cat /etc/hosts",
                ),
                KnowledgeEntry(
                    title = "/etc/resolv.conf",
                    summary = "Указывает DNS-серверы, к которым система отправляет запросы.",
                    detail = "Часто создаётся автоматически и перезаписывается, ручные правки теряются.",
                    code = "cat /etc/resolv.conf",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Проверка",
            entries = listOf(
                KnowledgeEntry(
                    title = "ping -c 4",
                    summary = "Отправляет четыре пакета и показывает задержку и потери.",
                    detail = "Без -c ping идёт бесконечно. Многие серверы не отвечают на ping, хотя работают.",
                    code = "ping -c 4 1.1.1.1",
                ),
                KnowledgeEntry(
                    title = "curl -I",
                    summary = "Запрашивает только заголовки ответа сервера.",
                    detail = "-L идёт по перенаправлениям, -v показывает весь обмен, включая TLS.",
                    code = "curl -IL https://example.com",
                ),
                KnowledgeEntry(
                    title = "wget",
                    summary = "Скачивает файл по ссылке.",
                    detail = "-c докачивает прерванную загрузку, -O задаёт имя файла.",
                    code = "wget -c https://example.com/file.iso",
                ),
                KnowledgeEntry(
                    title = "dig +short",
                    summary = "Выводит IP-адреса, в которые разрешается имя.",
                    detail = "@1.1.1.1 спрашивает конкретный DNS-сервер, MX и TXT запрашивают другие записи.",
                    code = "dig +short example.com @1.1.1.1",
                ),
                KnowledgeEntry(
                    title = "nslookup",
                    summary = "Разрешает имя в адрес и показывает, какой DNS-сервер ответил.",
                    code = "nslookup example.com",
                ),
                KnowledgeEntry(
                    title = "traceroute",
                    summary = "Показывает маршрут до узла по промежуточным узлам.",
                    detail = "Звёздочки значат, что узел не отвечает на запросы трассировки, а не обрыв.",
                    code = "traceroute example.com",
                ),
                KnowledgeEntry(
                    title = "mtr",
                    summary = "Совмещает ping и traceroute, обновляя потери на каждом узле.",
                    detail = "-r выводит отчёт после заданного числа пакетов.",
                    code = "mtr -rc 20 example.com",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Порты",
            entries = listOf(
                KnowledgeEntry(
                    title = "ss -tulpn",
                    summary = "Показывает, какие программы слушают какие порты.",
                    detail = "t — TCP, u — UDP, l — только слушающие, p — процесс, n — числа вместо имён. Процессы видны с sudo.",
                    code = "sudo ss -tulpn",
                ),
                KnowledgeEntry(
                    title = "ss -tan",
                    summary = "Показывает все TCP-соединения с их состоянием.",
                    code = "ss -tan state established",
                ),
                KnowledgeEntry(
                    title = "lsof -i",
                    summary = "Показывает, какой процесс держит указанный порт.",
                    code = "sudo lsof -i :80",
                ),
                KnowledgeEntry(
                    title = "nc -zv",
                    summary = "Проверяет, открыт ли порт на удалённом узле.",
                    detail = "-z не передаёт данные, -v выводит результат. -w 3 ограничивает ожидание тремя секундами.",
                    code = "nc -zv -w 3 example.com 22",
                ),
                KnowledgeEntry(
                    title = "nc -l",
                    summary = "Открывает порт и выводит всё, что на него приходит.",
                    detail = "В традиционной версии netcat порт задаётся через -p: nc -l -p 9000.",
                    code = "nc -l 9000",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "ssh",
            entries = listOf(
                KnowledgeEntry(
                    title = "ssh user@host",
                    summary = "Открывает оболочку на удалённой машине.",
                    detail = "Команда после адреса выполняется на сервере без входа в оболочку.",
                    code = "ssh anna@203.0.113.10",
                ),
                KnowledgeEntry(
                    title = "ssh -p",
                    summary = "Подключается к ssh на нестандартном порту.",
                    code = "ssh -p 2222 anna@203.0.113.10",
                ),
                KnowledgeEntry(
                    title = "ssh-keygen -t ed25519",
                    summary = "Создаёт пару ключей для входа без пароля.",
                    detail = "Закрытый ключ ~/.ssh/id_ed25519 остаётся на устройстве, на сервер копируется только .pub.",
                    code = "ssh-keygen -t ed25519 -C \"phone\"",
                ),
                KnowledgeEntry(
                    title = "ssh-copy-id",
                    summary = "Дописывает открытый ключ в authorized_keys на сервере.",
                    code = "ssh-copy-id anna@203.0.113.10",
                ),
                KnowledgeEntry(
                    title = "~/.ssh/config",
                    summary = "Хранит короткие имена серверов с адресом, портом и ключом.",
                    detail = "После блока Host vps с HostName, User и Port вход сводится к ssh vps.",
                    code = "ssh vps",
                ),
                KnowledgeEntry(
                    title = "scp",
                    summary = "Копирует файлы по ssh.",
                    detail = "-r копирует каталоги, -P задаёт порт заглавной буквой, в отличие от ssh.",
                    code = "scp -P 2222 site.tar.gz vps:/tmp/",
                ),
                KnowledgeEntry(
                    title = "rsync -avz",
                    summary = "Синхронизирует каталоги, передавая только изменённые файлы.",
                    detail = "Слэш в конце источника копирует содержимое, без слэша — сам каталог. --delete удаляет лишнее в цели.",
                    code = "rsync -avz ~/site/ vps:/var/www/site/",
                ),
                KnowledgeEntry(
                    title = "ssh -L",
                    summary = "Пробрасывает локальный порт на порт удалённой машины.",
                    detail = "После команды localhost:8080 на устройстве ведёт на порт 80 сервера.",
                    code = "ssh -L 8080:localhost:80 vps",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Файрвол",
            entries = listOf(
                KnowledgeEntry(
                    title = "ufw allow",
                    summary = "Открывает порт или службу в ufw.",
                    detail = "Правила файрвола требуют доступа к ядру, поэтому в proot не применяются. Раздел относится к серверу.",
                    code = "sudo ufw allow 22/tcp",
                ),
                KnowledgeEntry(
                    title = "ufw enable",
                    summary = "Включает файрвол и его автозапуск.",
                    detail = "Без правила для ssh удалённое соединение оборвётся сразу после включения.",
                    code = "sudo ufw enable",
                ),
                KnowledgeEntry(
                    title = "ufw status",
                    summary = "Показывает, включён ли ufw и какие правила действуют.",
                    detail = "numbered нумерует правила для удаления через ufw delete N.",
                    code = "sudo ufw status numbered",
                ),
                KnowledgeEntry(
                    title = "firewall-cmd --add-port",
                    summary = "Открывает порт в firewalld на Fedora и RHEL.",
                    detail = "Без --permanent правило пропадает после перезапуска, --reload применяет постоянные правила.",
                    code = "sudo firewall-cmd --permanent --add-port=8080/tcp",
                ),
                KnowledgeEntry(
                    title = "iptables -L",
                    summary = "Выводит правила iptables по цепочкам.",
                    detail = "-n не разрешает имена, -v показывает счётчики пакетов. В новых системах под ним работает nftables.",
                    code = "sudo iptables -L -n -v",
                ),
            ),
        ),
    ),
)
