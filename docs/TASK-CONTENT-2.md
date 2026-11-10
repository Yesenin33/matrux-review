Ты — редактор технического контента Android-приложения Matrux (Linux в proot на телефоне). Нужно написать 9 новых файлов раздела «Разделы» (обучающие справки).

## Что уже есть (не трогать, не редактировать)
- `app/src/main/java/ru/palma/app/ui/knowledge/Knowledge.kt` — модели, только читать.
- `LanguagesContent.kt`, `LinuxContent.kt`, `TermsContent.kt`, `HardwareContent.kt`, `GitContent.kt` — готовый контент, стиль брать из них.

## Модель (уже существует)
```kotlin
data class KnowledgeEntry(
    val title: String,
    val summary: String,
    val detail: String? = null,
    val code: String? = null,
)
data class KnowledgeGroup(val title: String, val entries: List<KnowledgeEntry>)
data class KnowledgeSection(
    val id: String,
    val title: String,
    val lead: String,
    val pro: Boolean,
    val branch: Branch = Branch.DEEPER,
    val groups: List<KnowledgeGroup>,
)
enum class Branch { START, DEEPER }
```

## Задача: ровно 9 новых файлов, по одной top-level `val` в каждом

| файл | имя val | id | title | pro | branch |
|---|---|---|---|---|---|
| `FirstStepsContent.kt` | `firstSteps` | `start` | Первые шаги | false | Branch.START |
| `FilesContent.kt` | `files` | `files` | Файлы и каталоги | false | Branch.START |
| `PackagesContent.kt` | `packages` | `packages` | Пакеты и обновления | true | Branch.DEEPER |
| `UsersContent.kt` | `users` | `users` | Права и пользователи | true | Branch.DEEPER |
| `ServicesContent.kt` | `services` | `services` | Службы и процессы | true | Branch.DEEPER |
| `NetworkContent.kt` | `network` | `network` | Сеть и ssh | true | Branch.DEEPER |
| `DisksContent.kt` | `disks` | `disks` | Диски и память | true | Branch.DEEPER |
| `LogsContent.kt` | `logs` | `logs` | Логи и поиск | true | Branch.DEEPER |
| `ScriptsContent.kt` | `scripts` | `scripts` | Скрипты | true | Branch.DEEPER |

Строки идут в порядке таблицы к пользователю: сначала то, что нужно новичку, потом то, что нужно тому, кто уже работает в терминале.

## Содержание по разделам (ориентир, можно уточнять)

1. **Первые шаги** (новичок, 4 группы, 22–26 записей): «Как устроен Linux» (дистрибутив, ядро, оболочка, что такое root), «Вход и выход» (login, logout, reboot, poweroff, exit, Ctrl+D/Ctrl+C), «Где что лежит» (/ — /etc — /home — /var/log — /usr/bin — /tmp — /opt), «Справка» (man, man -k, --help, apropos, info, tldr).
2. **Файлы и каталоги** (5 групп, 30–34): «Навигация» (pwd, cd, cd -, ls -la, ls -lh, tree), «Создать и удалить» (mkdir -p, touch, rm, rm -rf, rmdir, cp -r, mv), «Смотреть содержимое» (cat, less, head, tail, wc -l, file, stat), «Поиск» (find -name, find -mtime, locate, which, whereis), «Архивы» (tar -czf, tar -xzf, zip, unzip, ln -s).
3. **Пакеты и обновления** (5 групп, 30–34): «Debian и Ubuntu» (apt update, apt upgrade, apt install, apt remove, apt search, dpkg -l), «Arch» (pacman -Syu, -S, -R, -Ss, -Qi), «Fedora» (dnf install, update, search, remove), «Чистка и следы» (apt autoremove, apt clean, pacman -Sc, dnf clean all), «Универсальные» (snap install, flatpak install, ./configure && make, dpkg -i, apt --fix-broken install).
4. **Права и пользователи** (5 групп, 28–32): «Права файла» (ls -l, chmod 644/755, chmod +x, chown user:group, chmod -R, umask), «Кто я» (whoami, id, groups, who, last), «Пользователи» (useradd -m, passwd, usermod -aG, userdel, getent passwd), «Повышение прав» (sudo, sudo -i, su -, visudo, sudo -l), «Особые биты» (setuid, sticky bit, ACL: getfacl/setfacl).
5. **Службы и процессы** (5 групп, 32–36): «systemctl» (status, start, stop, restart, enable, disable, list-units --failed), «Процессы» (ps aux, top, htop, pgrep, kill -15, kill -9, pkill -f, nice, renice), «Автозапуск по таймеру» (systemctl list-timers, crontab -e, crontab -l, @reboot), «Своя служба» (юнит-файл: [Unit]/[Service]/[Install], daemon-reload), «Ресурсы» (ulimit -a, systemd-run --scope -p MemoryMax).
6. **Сеть и ssh** (5 групп, 30–34): «Свои адреса» (ip a, ip r, hostname -I, /etc/hosts, resolv.conf), «Проверка» (ping -c 4, curl -I, dig +short, nslookup, traceroute, mtr), «Порты» (ss -tulpn, ss -tan, lsof -i :80, nc -zv host 22), «ssh» (ssh user@host, ssh -p, ключи: ssh-keygen -t ed25519, ssh-copy-id, ~/.ssh/config, scp, rsync -avz), «Файрвол» (ufw allow/status/enable, firewall-cmd --add-port, iptables -L).
7. **Диски и память** (4–5 групп, 26–30): «Место» (df -h, du -sh, du -xh --max-depth=1, ncdu, find -size +1G), «Устройства» (lsblk, blkid, lsof +L1, mount, /etc/fstab, umount), «Разметка» (fdisk -l, mkfs.ext4, parted, swap: mkswap/swapon, swapon --show), «Память» (free -h, vmstat 1, /proc/meminfo, drop_caches), «Состояние диска» (smartctl -a, dmesg | grep -i error).
8. **Логи и поиск** (5 групп, 30–34): «journalctl» (-u unit, -f, -n 100, --since, -p err, --disk-usage), «Файлы логов» (/var/log/syslog, /var/log/auth.log, /var/log/dpkg.log, dmesg, tail -f), «grep» (-rn, -i, -v, -A/-B/-C, --include, -c), «Разбор текста» (cut -d, sort | uniq -c, awk '{print $2}', sed -i 's/a/b/g', tr, xargs), «Конвейеры на деле» (grep по логу + sort + head; du | sort -h | tail).
9. **Скрипты** (5 групп, 28–32): «Основа» (#!/usr/bin/env bash, chmod +x, set -euo pipefail, комментарии, запуск ./script.sh), «Переменные и аргументы» ($1, $@, $#, "$(date +%F)", read -p, export), «Ветвления и циклы» (if [[ ]], case, for f in *.log, while read -r line), «Функции» (function name(), return, локальные переменные), «Практика» (резервная копия каталога tar с датой, ротация старых логов find -mtime +7 -delete, проверка доступности хоста curl -fsS, запуск по cron каждый час).

## Требования к тексту
- Всё по-русски, «вы» не используется, без вступлений и обращений.
- `title` — команда или короткий термин (`ls -la`, `apt upgrade`, `journalctl -u`). Для понятий — термин (`Ядро`, `Sticky bit`).
- `summary` — одно предложение, что делает, до 90 символов, без «просто», «легко», «давайте».
- `detail` — 1–2 предложения, только если нужны нюансы (разница -9 и -15, что делает -p, чем опасно rm -rf). Не дублировать summary.
- `code` — рабочая команда с реальными путями и флагами, до 60 символов, без приглашения `$`, без комментариев.
- Никаких подсказок вида «нажмите», «попробуйте», «не забудьте», «это просто». Только факты о командах.
- Групповые заголовки короткие: «Основные», «Навигация», «Проверка».
- Не выдумывать флаги. Если не уверен в синтаксисе — использовать общеизвестный.

## Границы
- Создать только 9 перечисленных файлов в `app/src/main/java/ru/palma/app/ui/knowledge/`.
- Не менять существующие файлы, `build.gradle`, ресурсы, другие экраны.
- `package ru.palma.app.ui.knowledge` в каждом файле, импортов не требуется.
- В конце: собрать `./gradlew :app:compileDebugKotlin` нельзя (сеть), поэтому просто проверь синтаксис глазами: каждое `KnowledgeEntry(...)` с именованными аргументами, запятые на месте.