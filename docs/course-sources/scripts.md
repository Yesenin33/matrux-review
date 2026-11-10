# Скрипты под задачи — рабочий разбор источников

Обращение: 2026-11-10. В приложение файл не попадает.

## Источники

| Источник | Ссылка | Лицензия |
|---|---|---|
| M. Cooper, Advanced Bash-Scripting Guide, пер. А. Киселёв | https://rus-linux.net/MyLDP/BOOKS/abs-guide/flat/abs-book.html | Open Publication License v1.0 (указана в переводе) |
| То же, зеркало opennet | https://www.opennet.ru/docs/RUS/bash_scripting_guide/ | Open Publication License v1.0 |
| «Введение в Linux и Bash», younglinux | https://younglinux.info/bash/linux | © younglinux.info |
| Справочник GNU Bash | https://www.gnu.org/software/bash/manual/bash.html | GNU FDL |
| EbookFoundation, free-programming-books (рус.) | https://github.com/EbookFoundation/free-programming-books/blob/main/books/free-programming-books-ru.md | CC BY 4.0 |

## Конспект

- Скрипт: первая строка `#!/bin/sh`; запуск `sh file.sh` или `chmod +x` и `./file.sh`; код возврата `$?`, `0` — успех.
- Команды: `echo`, `printf` (переносимее `echo`), `pwd`, `ls`, `cd`, `mkdir -p`, `touch`, `cp`, `mv`, `rm`.
- Текст и конвейеры: `cat`, `head -n`, `tail -n`, `wc -l`, `grep` (`-c`, `-v`, `-i`), `sort` (`-n`, `-r`), `uniq -c` (только соседние повторы — сначала `sort`), `cut -d -f`, `tr`; `|` передаёт вывод на вход следующей команде; `>` перезапись, `>>` дозапись, `2>` поток ошибок.
- Переменные: `name=value` без пробелов вокруг `=`; `"$name"` в кавычках; подстановка команды `$(cmd)`; арифметика `$((a + b))`; позиционные параметры `$1`, `$#`.
- Условия: `if [ "$a" -gt 3 ]; then ... fi`; строки `=`/`!=`, числа `-eq -ne -lt -le -gt -ge`, файлы `-f -d -e`; `&&` и `||`; `case ... esac`.
- Циклы: `for f in *.txt; do ... done`, `while read -r line; do ... done < file`, `seq`.
- Архивы и права: `tar -czf a.tar.gz dir`, `tar -tzf`, `tar -xzf`; `chmod 755`, `chmod +x`, `ls -l` — `rwx` для владельца, группы, остальных; восьмеричные права: r=4, w=2, x=1.
- cron: пять полей `минута час день месяц день_недели`; `crontab -e`; `0 3 * * *` — каждый день в 03:00; `*/15 * * * *` — каждые 15 минут. Внутри proot службы cron нет — в курсе это сказано, расписание проверяется разбором строки.
- Разбор логов: `awk '{print $1}'`, частоты `sort | uniq -c | sort -rn | head`.

## Совместимость

В Alpine оболочка — BusyBox `sh`, `bash` может не быть. Поэтому задачи написаны на POSIX `sh` и проверены запуском `sh` (dash/BusyBox-совместимо).

## Карта уроков

1. Первые шаги (открыт): первые команды · вывод и перенаправление.
2. Файлы и каталоги.
3. Текст и конвейеры: фильтры · сортировка и подсчёт.
4. Переменные и условия: переменные · условия.
5. Циклы.
6. Архивы и права.
7. cron и резервные копии.
8. Разбор логов.
