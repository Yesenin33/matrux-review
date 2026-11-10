package ru.palma.app.ui.knowledge

val users: KnowledgeSection = KnowledgeSection(
    id = "users",
    title = "Права и пользователи",
    lead = "Кто владеет файлом, кто может его читать и как временно получить права root.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "Права файла",
            entries = listOf(
                KnowledgeEntry(
                    title = "ls -l",
                    summary = "Показывает права в виде -rwxr-xr-x: владелец, группа, остальные.",
                    detail = "r — чтение, w — запись, x — запуск. Для каталога x означает право войти в него.",
                    code = "ls -l ~/script.sh",
                ),
                KnowledgeEntry(
                    title = "chmod 644",
                    summary = "Владелец читает и пишет, остальные только читают.",
                    detail = "Каждая цифра — сумма: 4 чтение, 2 запись, 1 запуск. Обычные права для текстов и настроек.",
                    code = "chmod 644 index.html",
                ),
                KnowledgeEntry(
                    title = "chmod 755",
                    summary = "Владелец делает всё, остальные читают и запускают.",
                    detail = "Обычные права для каталогов и скриптов. 600 и 700 закрывают доступ всем, кроме владельца.",
                    code = "chmod 755 ~/bin/deploy.sh",
                ),
                KnowledgeEntry(
                    title = "chmod +x",
                    summary = "Делает файл исполняемым.",
                    detail = "u+x даёт право только владельцу, a-w снимает запись у всех.",
                    code = "chmod +x backup.sh",
                ),
                KnowledgeEntry(
                    title = "chmod -R",
                    summary = "Меняет права рекурсивно для каталога и всего внутри.",
                    detail = "chmod -R 755 делает исполняемыми и обычные файлы. Точнее разделить через find -type f и -type d.",
                    code = "find ~/site -type f -exec chmod 644 {} +",
                ),
                KnowledgeEntry(
                    title = "chown",
                    summary = "Меняет владельца и группу файла.",
                    detail = "-R применяет к каталогу целиком. Требует прав root.",
                    code = "sudo chown -R anna:www-data /var/www/site",
                ),
                KnowledgeEntry(
                    title = "umask",
                    summary = "Задаёт права, которые снимаются с новых файлов при создании.",
                    detail = "При umask 022 файлы создаются с 644, каталоги с 755. 077 закрывает новые файлы от всех.",
                    code = "umask 027",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Кто я",
            entries = listOf(
                KnowledgeEntry(
                    title = "whoami",
                    summary = "Выводит имя текущего пользователя.",
                    code = "whoami",
                ),
                KnowledgeEntry(
                    title = "id",
                    summary = "Показывает UID, основную группу и все дополнительные группы.",
                    detail = "UID 0 — это root, обычные пользователи начинаются с 1000.",
                    code = "id anna",
                ),
                KnowledgeEntry(
                    title = "groups",
                    summary = "Перечисляет группы пользователя.",
                    detail = "Новая группа появляется в сеансе только после повторного входа.",
                    code = "groups",
                ),
                KnowledgeEntry(
                    title = "who",
                    summary = "Показывает, кто сейчас вошёл в систему и с какого терминала.",
                    code = "who",
                ),
                KnowledgeEntry(
                    title = "w",
                    summary = "Показывает вошедших пользователей и что они сейчас запускают.",
                    code = "w",
                ),
                KnowledgeEntry(
                    title = "last",
                    summary = "Выводит историю входов в систему.",
                    detail = "Читает /var/log/wtmp. lastb показывает неудачные попытки входа.",
                    code = "last -n 20",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Пользователи",
            entries = listOf(
                KnowledgeEntry(
                    title = "useradd -m",
                    summary = "Создаёт пользователя вместе с домашним каталогом.",
                    detail = "Без -m каталог не создаётся. -s задаёт оболочку. В Debian есть интерактивный adduser.",
                    code = "sudo useradd -m -s /bin/bash anna",
                ),
                KnowledgeEntry(
                    title = "passwd",
                    summary = "Меняет пароль текущего или указанного пользователя.",
                    detail = "Пароль другого пользователя меняет только root. -l блокирует вход по паролю.",
                    code = "sudo passwd anna",
                ),
                KnowledgeEntry(
                    title = "usermod -aG",
                    summary = "Добавляет пользователя в дополнительную группу.",
                    detail = "Без -a пользователь потеряет все прежние дополнительные группы.",
                    code = "sudo usermod -aG sudo anna",
                ),
                KnowledgeEntry(
                    title = "userdel -r",
                    summary = "Удаляет пользователя вместе с домашним каталогом и почтой.",
                    detail = "Без -r домашний каталог остаётся на диске.",
                    code = "sudo userdel -r olduser",
                ),
                KnowledgeEntry(
                    title = "getent passwd",
                    summary = "Выводит учётные записи, включая сетевые из LDAP.",
                    detail = "Поля через двоеточие: имя, x, UID, GID, описание, домашний каталог, оболочка.",
                    code = "getent passwd anna",
                ),
                KnowledgeEntry(
                    title = "getent group",
                    summary = "Показывает группу и её участников.",
                    code = "getent group sudo",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Повышение прав",
            entries = listOf(
                KnowledgeEntry(
                    title = "sudo",
                    summary = "Запускает одну команду от имени root.",
                    detail = "Запрашивает пароль текущего пользователя, а не root, и запоминает его на несколько минут.",
                    code = "sudo systemctl restart nginx",
                ),
                KnowledgeEntry(
                    title = "sudo -i",
                    summary = "Открывает оболочку root с его окружением.",
                    detail = "Все команды после этого выполняются от root, пока не будет exit.",
                    code = "sudo -i",
                ),
                KnowledgeEntry(
                    title = "sudo -l",
                    summary = "Показывает, какие команды текущему пользователю разрешено запускать через sudo.",
                    code = "sudo -l",
                ),
                KnowledgeEntry(
                    title = "su -",
                    summary = "Переключается на другого пользователя с его окружением.",
                    detail = "Спрашивает пароль целевого пользователя. Без дефиса остаются переменные и каталог прежнего.",
                    code = "su - anna",
                ),
                KnowledgeEntry(
                    title = "visudo",
                    summary = "Безопасно правит /etc/sudoers с проверкой синтаксиса.",
                    detail = "Ошибка в sudoers, записанная напрямую, лишает доступа к sudo. Свои правила удобнее класть в /etc/sudoers.d/.",
                    code = "sudo visudo -f /etc/sudoers.d/anna",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Особые биты",
            entries = listOf(
                KnowledgeEntry(
                    title = "Setuid",
                    summary = "Программа запускается с правами владельца файла, а не пользователя.",
                    detail = "Так работает passwd: обычный пользователь меняет файл, доступный только root. В ls -l видна буква s вместо x.",
                    code = "ls -l /usr/bin/passwd",
                ),
                KnowledgeEntry(
                    title = "Setgid на каталоге",
                    summary = "Новые файлы в каталоге получают группу каталога.",
                    detail = "Удобно для общих папок команды: все файлы остаются в одной группе.",
                    code = "sudo chmod g+s /srv/shared",
                ),
                KnowledgeEntry(
                    title = "Sticky bit",
                    summary = "Удалять файлы в каталоге может только их владелец.",
                    detail = "Стоит на /tmp: писать могут все, но удалить чужой файл нельзя. В ls -l видна буква t в конце.",
                    code = "sudo chmod +t /srv/upload",
                ),
                KnowledgeEntry(
                    title = "getfacl",
                    summary = "Показывает расширенные права ACL для отдельных пользователей и групп.",
                    detail = "Знак + в конце строки прав в ls -l означает, что у файла есть ACL.",
                    code = "getfacl /srv/shared",
                ),
                KnowledgeEntry(
                    title = "setfacl",
                    summary = "Выдаёт права конкретному пользователю, не меняя владельца и группу.",
                    detail = "-m добавляет правило, -x удаляет, -R применяет рекурсивно.",
                    code = "setfacl -m u:anna:rw /srv/shared/report.txt",
                ),
            ),
        ),
    ),
)
