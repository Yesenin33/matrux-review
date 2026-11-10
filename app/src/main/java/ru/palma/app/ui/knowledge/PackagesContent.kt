package ru.palma.app.ui.knowledge

val packages: KnowledgeSection = KnowledgeSection(
    id = "packages",
    title = "Пакеты и обновления",
    lead = "Установка, обновление и удаление программ в Debian, Arch и Fedora.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "Debian и Ubuntu",
            entries = listOf(
                KnowledgeEntry(
                    title = "apt update",
                    summary = "Скачивает свежие списки пакетов из репозиториев.",
                    detail = "Сами программы не обновляет. Без него apt install может искать уже удалённые версии.",
                    code = "sudo apt update",
                ),
                KnowledgeEntry(
                    title = "apt upgrade",
                    summary = "Обновляет установленные пакеты до новых версий.",
                    detail = "Не удаляет пакеты. Если обновление требует удаления, пакет остаётся на старой версии.",
                    code = "sudo apt upgrade -y",
                ),
                KnowledgeEntry(
                    title = "apt full-upgrade",
                    summary = "Обновляет систему, удаляя пакеты, если этого требуют зависимости.",
                    detail = "Нужен при переходе между версиями дистрибутива. Список удаляемого стоит прочитать до ответа Y.",
                    code = "sudo apt full-upgrade",
                ),
                KnowledgeEntry(
                    title = "apt install",
                    summary = "Устанавливает пакеты вместе с зависимостями.",
                    detail = "--no-install-recommends не тянет рекомендуемые пакеты и экономит место.",
                    code = "sudo apt install htop git curl",
                ),
                KnowledgeEntry(
                    title = "apt remove",
                    summary = "Удаляет пакет, оставляя его настройки в /etc.",
                    code = "sudo apt remove nginx",
                ),
                KnowledgeEntry(
                    title = "apt purge",
                    summary = "Удаляет пакет вместе с системными настройками.",
                    detail = "Файлы в домашнем каталоге пользователя не трогает.",
                    code = "sudo apt purge nginx",
                ),
                KnowledgeEntry(
                    title = "apt search",
                    summary = "Ищет пакеты по имени и описанию.",
                    code = "apt search markdown",
                ),
                KnowledgeEntry(
                    title = "apt show",
                    summary = "Показывает версию, размер, зависимости и описание пакета.",
                    code = "apt show ffmpeg",
                ),
                KnowledgeEntry(
                    title = "dpkg -l",
                    summary = "Выводит список установленных пакетов с версиями.",
                    detail = "ii в первой колонке — пакет установлен, rc — удалён, но настройки остались.",
                    code = "dpkg -l | grep python3",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Arch",
            entries = listOf(
                KnowledgeEntry(
                    title = "pacman -Syu",
                    summary = "Обновляет базу пакетов и всю систему разом.",
                    detail = "Arch не поддерживает частичные обновления: -Sy без -u ломает зависимости.",
                    code = "sudo pacman -Syu",
                ),
                KnowledgeEntry(
                    title = "pacman -S",
                    summary = "Устанавливает пакеты.",
                    detail = "--needed пропускает пакеты, которые уже стоят последней версии.",
                    code = "sudo pacman -S --needed git base-devel",
                ),
                KnowledgeEntry(
                    title = "pacman -Rs",
                    summary = "Удаляет пакет и зависимости, которые больше никому не нужны.",
                    detail = "-R удаляет только сам пакет, -Rns заодно стирает его файлы настроек.",
                    code = "sudo pacman -Rs nodejs",
                ),
                KnowledgeEntry(
                    title = "pacman -Ss",
                    summary = "Ищет пакеты в репозиториях.",
                    code = "pacman -Ss terminal",
                ),
                KnowledgeEntry(
                    title = "pacman -Qi",
                    summary = "Показывает сведения об установленном пакете.",
                    detail = "-Si показывает то же для пакета из репозитория.",
                    code = "pacman -Qi firefox",
                ),
                KnowledgeEntry(
                    title = "pacman -Qe",
                    summary = "Выводит пакеты, установленные явно, а не как зависимости.",
                    code = "pacman -Qe | wc -l",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Fedora",
            entries = listOf(
                KnowledgeEntry(
                    title = "dnf install",
                    summary = "Устанавливает пакеты с зависимостями.",
                    code = "sudo dnf install htop",
                ),
                KnowledgeEntry(
                    title = "dnf upgrade",
                    summary = "Обновляет все установленные пакеты.",
                    detail = "dnf update — старое имя той же команды.",
                    code = "sudo dnf upgrade",
                ),
                KnowledgeEntry(
                    title = "dnf search",
                    summary = "Ищет пакеты по имени и описанию.",
                    code = "dnf search editor",
                ),
                KnowledgeEntry(
                    title = "dnf remove",
                    summary = "Удаляет пакет и зависимости, поставленные только ради него.",
                    code = "sudo dnf remove httpd",
                ),
                KnowledgeEntry(
                    title = "dnf info",
                    summary = "Показывает версию, репозиторий и описание пакета.",
                    code = "dnf info git",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Чистка",
            entries = listOf(
                KnowledgeEntry(
                    title = "apt autoremove",
                    summary = "Удаляет зависимости, которые остались после удаления пакетов.",
                    detail = "--purge заодно стирает их настройки.",
                    code = "sudo apt autoremove --purge",
                ),
                KnowledgeEntry(
                    title = "apt clean",
                    summary = "Удаляет скачанные .deb из кэша в /var/cache/apt/archives.",
                    detail = "apt autoclean удаляет только те, которых уже нет в репозиториях.",
                    code = "sudo apt clean",
                ),
                KnowledgeEntry(
                    title = "pacman -Sc",
                    summary = "Удаляет из кэша старые версии пакетов.",
                    detail = "-Scc очищает кэш полностью, откат на прежнюю версию после этого невозможен без скачивания.",
                    code = "sudo pacman -Sc",
                ),
                KnowledgeEntry(
                    title = "dnf autoremove",
                    summary = "Удаляет пакеты, которые остались без зависимых.",
                    code = "sudo dnf autoremove",
                ),
                KnowledgeEntry(
                    title = "dnf clean all",
                    summary = "Очищает кэш метаданных и скачанных пакетов.",
                    code = "sudo dnf clean all",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Универсальные",
            entries = listOf(
                KnowledgeEntry(
                    title = "dpkg -i",
                    summary = "Устанавливает .deb из файла.",
                    detail = "Зависимости не скачивает. apt install ./file.deb делает то же и подтягивает их.",
                    code = "sudo dpkg -i ./code_amd64.deb",
                ),
                KnowledgeEntry(
                    title = "apt --fix-broken install",
                    summary = "Доставляет недостающие зависимости после неудачной установки.",
                    code = "sudo apt --fix-broken install",
                ),
                KnowledgeEntry(
                    title = "apt-mark hold",
                    summary = "Запрещает обновлять выбранный пакет.",
                    detail = "apt-mark unhold снимает запрет, apt-mark showhold показывает список.",
                    code = "sudo apt-mark hold postgresql",
                ),
                KnowledgeEntry(
                    title = "snap install",
                    summary = "Ставит программу из Snap Store со всеми библиотеками внутри.",
                    detail = "Требует службы snapd под systemd, поэтому в proot не работает.",
                    code = "sudo snap install hello-world",
                ),
                KnowledgeEntry(
                    title = "flatpak install",
                    summary = "Ставит графическую программу из Flathub в изолированной среде.",
                    detail = "Песочнице нужны пространства имён ядра, в proot запуск обычно не удаётся.",
                    code = "flatpak install flathub org.gimp.GIMP",
                ),
                KnowledgeEntry(
                    title = "./configure && make",
                    summary = "Собирает программу из исходников.",
                    detail = "sudo make install кладёт файлы в /usr/local мимо пакетного менеджера, удалять их придётся вручную.",
                    code = "./configure --prefix=/usr/local && make",
                ),
            ),
        ),
    ),
)
