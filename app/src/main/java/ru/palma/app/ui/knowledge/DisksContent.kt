package ru.palma.app.ui.knowledge

val disks: KnowledgeSection = KnowledgeSection(
    id = "disks",
    title = "Диски и память",
    lead = "Где кончается место, какие устройства подключены и чем занята оперативная память.",
    pro = true,
    branch = Branch.DEEPER,
    groups = listOf(
        KnowledgeGroup(
            title = "Место",
            entries = listOf(
                KnowledgeEntry(
                    title = "df -h",
                    summary = "Показывает свободное и занятое место на каждой файловой системе.",
                    detail = "В proot строки относятся к разделам Android, на которых лежит система.",
                    code = "df -h",
                ),
                KnowledgeEntry(
                    title = "df -i",
                    summary = "Показывает занятые inode — лимит на число файлов.",
                    detail = "Когда inode кончаются, запись невозможна даже при свободном месте.",
                    code = "df -i /",
                ),
                KnowledgeEntry(
                    title = "du -sh",
                    summary = "Выводит общий размер каталога.",
                    code = "du -sh ~/Downloads",
                ),
                KnowledgeEntry(
                    title = "du --max-depth=1",
                    summary = "Показывает размер каждого подкаталога на один уровень вниз.",
                    detail = "-x не заходит на другие файловые системы, -h выводит размеры в К, М и Г.",
                    code = "du -xh --max-depth=1 /var",
                ),
                KnowledgeEntry(
                    title = "ncdu",
                    summary = "Интерактивно показывает, что занимает место, с удалением по d.",
                    detail = "Ставится отдельным пакетом. -x не выходит за пределы текущей файловой системы.",
                    code = "ncdu -x /",
                ),
                KnowledgeEntry(
                    title = "find -size",
                    summary = "Находит файлы больше заданного размера.",
                    detail = "Суффиксы k, M, G. 2>/dev/null скрывает ошибки доступа.",
                    code = "find / -xdev -size +1G 2>/dev/null",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Устройства",
            entries = listOf(
                KnowledgeEntry(
                    title = "lsblk",
                    summary = "Рисует дерево дисков и разделов с размерами и точками монтирования.",
                    code = "lsblk",
                ),
                KnowledgeEntry(
                    title = "lsblk -f",
                    summary = "Добавляет файловую систему, метку и UUID каждого раздела.",
                    code = "lsblk -f",
                ),
                KnowledgeEntry(
                    title = "blkid",
                    summary = "Выводит UUID и тип файловой системы разделов.",
                    detail = "UUID не меняется при переподключении, в отличие от имени /dev/sdb1.",
                    code = "sudo blkid /dev/sdb1",
                ),
                KnowledgeEntry(
                    title = "mount",
                    summary = "Подключает раздел в каталог.",
                    detail = "Без аргументов выводит всё подключённое. В proot монтирование устройств недоступно.",
                    code = "sudo mount /dev/sdb1 /mnt/usb",
                ),
                KnowledgeEntry(
                    title = "umount",
                    summary = "Отключает раздел.",
                    detail = "Ошибка target is busy значит, что раздел занят. Кем — показывает lsof +f -- /mnt/usb.",
                    code = "sudo umount /mnt/usb",
                ),
                KnowledgeEntry(
                    title = "/etc/fstab",
                    summary = "Список разделов, которые подключаются при загрузке.",
                    detail = "Ошибка в fstab может остановить загрузку. sudo mount -a проверяет файл без перезагрузки.",
                    code = "sudo mount -a",
                ),
                KnowledgeEntry(
                    title = "lsof +L1",
                    summary = "Находит удалённые файлы, которые всё ещё держат открытыми процессы.",
                    detail = "Место от такого файла освобождается только после перезапуска процесса.",
                    code = "sudo lsof +L1",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Разметка",
            entries = listOf(
                KnowledgeEntry(
                    title = "fdisk -l",
                    summary = "Выводит таблицы разделов всех дисков.",
                    detail = "Без -l открывает диск для правки. Изменения записываются только по команде w.",
                    code = "sudo fdisk -l /dev/sdb",
                ),
                KnowledgeEntry(
                    title = "parted",
                    summary = "Размечает диски, включая GPT и диски больше 2 ТБ.",
                    detail = "Каждая команда в parted применяется сразу, без отдельного сохранения.",
                    code = "sudo parted /dev/sdb print",
                ),
                KnowledgeEntry(
                    title = "mkfs.ext4",
                    summary = "Создаёт файловую систему ext4 на разделе.",
                    detail = "Все данные на разделе стираются. Имя устройства стоит сверить через lsblk.",
                    code = "sudo mkfs.ext4 -L data /dev/sdb1",
                ),
                KnowledgeEntry(
                    title = "mkswap",
                    summary = "Готовит файл или раздел для подкачки.",
                    detail = "Файлу подкачки нужны права 600, иначе swapon предупреждает о небезопасности.",
                    code = "sudo mkswap /swapfile",
                ),
                KnowledgeEntry(
                    title = "swapon",
                    summary = "Включает подкачку.",
                    detail = "Для постоянного включения файл добавляется в /etc/fstab. swapoff отключает.",
                    code = "sudo swapon /swapfile",
                ),
                KnowledgeEntry(
                    title = "swapon --show",
                    summary = "Показывает активную подкачку, её размер и заполненность.",
                    code = "swapon --show",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Память",
            entries = listOf(
                KnowledgeEntry(
                    title = "free -h",
                    summary = "Показывает общую, занятую и доступную оперативную память.",
                    detail = "Смотреть стоит на available: buff/cache система отдаёт программам по требованию.",
                    code = "free -h",
                ),
                KnowledgeEntry(
                    title = "vmstat 1",
                    summary = "Каждую секунду выводит память, подкачку, ввод-вывод и нагрузку CPU.",
                    detail = "Ненулевые si и so означают активную работу подкачки и нехватку памяти.",
                    code = "vmstat 1 5",
                ),
                KnowledgeEntry(
                    title = "/proc/meminfo",
                    summary = "Подробная сводка ядра о памяти.",
                    code = "grep -E 'MemAvailable|SwapFree' /proc/meminfo",
                ),
                KnowledgeEntry(
                    title = "ps --sort=-%mem",
                    summary = "Выводит процессы, отсортированные по занятой памяти.",
                    detail = "RSS — память, реально занятая процессом, в килобайтах.",
                    code = "ps aux --sort=-%mem | head",
                ),
                KnowledgeEntry(
                    title = "drop_caches",
                    summary = "Сбрасывает файловый кэш ядра.",
                    detail = "Свободной памяти больше не становится по сути: кэш наполнится снова. Применяется для замеров скорости.",
                    code = "sync; echo 3 | sudo tee /proc/sys/vm/drop_caches",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Состояние диска",
            entries = listOf(
                KnowledgeEntry(
                    title = "smartctl -a",
                    summary = "Показывает SMART-данные диска: износ, ошибки, температуру.",
                    detail = "Входит в пакет smartmontools. Растущие Reallocated_Sector_Ct и Pending_Sector — признак умирающего диска.",
                    code = "sudo smartctl -a /dev/sda",
                ),
                KnowledgeEntry(
                    title = "smartctl -t short",
                    summary = "Запускает короткий самотест диска на пару минут.",
                    detail = "Результат появляется в выводе smartctl -a после завершения теста.",
                    code = "sudo smartctl -t short /dev/sda",
                ),
                KnowledgeEntry(
                    title = "dmesg | grep -i error",
                    summary = "Ищет ошибки устройств в сообщениях ядра.",
                    detail = "-T переводит метки в обычное время. В proot доступ к журналу ядра обычно закрыт.",
                    code = "sudo dmesg -T | grep -i error",
                ),
            ),
        ),
    ),
)
