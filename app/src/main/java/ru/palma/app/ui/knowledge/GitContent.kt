package ru.palma.app.ui.knowledge

val git: KnowledgeSection = KnowledgeSection(
    id = "git",
    title = "Основные команды git",
    lead = "История, ветки и совместная работа — от первого коммита до rebase.",
    pro = true,
    groups = listOf(
        KnowledgeGroup(
            title = "Начало работы",
            entries = listOf(
                KnowledgeEntry(
                    title = "git config --global user.name",
                    summary = "Задаёт имя автора для всех будущих коммитов.",
                    detail = "Так же задаётся user.email. Без --global настройка действует только в текущем репозитории.",
                    code = "git config --global user.name \"Anna Ivanova\"",
                ),
                KnowledgeEntry(
                    title = "git init",
                    summary = "Создаёт новый репозиторий в текущем каталоге.",
                    detail = "-b main сразу задаёт имя первой ветки. Вся история хранится в скрытом каталоге .git.",
                    code = "git init -b main",
                ),
                KnowledgeEntry(
                    title = "git clone",
                    summary = "Скачивает репозиторий со всей историей.",
                    detail = "--depth 1 берёт только последний коммит — быстро, если история не нужна.",
                    code = "git clone --depth 1 git@github.com:anna/site.git",
                ),
                KnowledgeEntry(
                    title = ".gitignore",
                    summary = "Перечисляет файлы, которые git не должен отслеживать.",
                    detail = "Уже закоммиченный файл продолжает отслеживаться: уберите его из индекса через git rm --cached.",
                    code = "printf 'node_modules/\\n.env\\n*.log\\n' >> .gitignore",
                ),
                KnowledgeEntry(
                    title = "git remote add",
                    summary = "Связывает локальный репозиторий с удалённым.",
                    detail = "origin — принятое имя основного удалённого репозитория.",
                    code = "git remote add origin git@github.com:anna/site.git",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Ежедневные",
            entries = listOf(
                KnowledgeEntry(
                    title = "git status",
                    summary = "Показывает изменённые, добавленные и неотслеживаемые файлы.",
                    detail = "-s выводит коротко, по строке на файл.",
                    code = "git status -s",
                ),
                KnowledgeEntry(
                    title = "git add",
                    summary = "Добавляет изменения в индекс для следующего коммита.",
                    detail = "git add . берёт всё в каталоге, -p позволяет выбрать отдельные куски изменений.",
                    code = "git add -p",
                ),
                KnowledgeEntry(
                    title = "git commit -m",
                    summary = "Сохраняет проиндексированные изменения в истории с сообщением.",
                    detail = "-a дополнительно индексирует все изменённые отслеживаемые файлы, но не новые.",
                    code = "git commit -m \"Добавить форму входа\"",
                ),
                KnowledgeEntry(
                    title = "git commit --amend",
                    summary = "Исправляет последний коммит: сообщение или содержимое.",
                    detail = "Создаёт новый коммит вместо старого. Не применяйте к уже отправленным коммитам, иначе понадобится force push.",
                    code = "git commit --amend --no-edit",
                ),
                KnowledgeEntry(
                    title = "git diff",
                    summary = "Показывает непроиндексированные изменения построчно.",
                    detail = "--staged показывает то, что уйдёт в коммит. git diff main..feature сравнивает ветки.",
                    code = "git diff --staged",
                ),
                KnowledgeEntry(
                    title = "git restore",
                    summary = "Отменяет изменения в файле или убирает его из индекса.",
                    detail = "Без флагов возвращает файл к последнему коммиту — правки пропадут. --staged только снимает файл с индекса.",
                    code = "git restore --staged config.yml",
                ),
                KnowledgeEntry(
                    title = "git log --oneline --graph",
                    summary = "Показывает историю коммитов компактно, с ветвлениями.",
                    detail = "--all добавляет все ветки, -n 20 ограничивает число коммитов.",
                    code = "git log --oneline --graph --all",
                ),
                KnowledgeEntry(
                    title = "git show",
                    summary = "Показывает содержимое и изменения конкретного коммита.",
                    detail = "Без аргумента показывает последний коммит. --stat выводит только список файлов.",
                    code = "git show --stat HEAD~1",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Ветки",
            entries = listOf(
                KnowledgeEntry(
                    title = "git branch",
                    summary = "Показывает, создаёт и удаляет ветки.",
                    detail = "-a добавляет удалённые ветки, -d удаляет слитую ветку, -D — любую.",
                    code = "git branch -a",
                ),
                KnowledgeEntry(
                    title = "git switch",
                    summary = "Переключает на существующую ветку.",
                    detail = "git switch - возвращает на предыдущую ветку. Незакоммиченные правки переносятся, если не конфликтуют.",
                    code = "git switch main",
                ),
                KnowledgeEntry(
                    title = "git switch -c",
                    summary = "Создаёт новую ветку и сразу переходит на неё.",
                    detail = null,
                    code = "git switch -c feature/login",
                ),
                KnowledgeEntry(
                    title = "git checkout",
                    summary = "Переключает ветки или достаёт файл из другого коммита.",
                    detail = "Старая универсальная команда. Для веток понятнее switch, для файлов — restore.",
                    code = "git checkout main -- README.md",
                ),
                KnowledgeEntry(
                    title = "git merge",
                    summary = "Вливает изменения другой ветки в текущую.",
                    detail = "--no-ff всегда создаёт коммит слияния, чтобы в истории было видно, где была ветка.",
                    code = "git merge --no-ff feature/login",
                ),
                KnowledgeEntry(
                    title = "git rebase -i",
                    summary = "Переписывает последние коммиты: склеивает, переименовывает, удаляет.",
                    detail = "В редакторе pick оставляет коммит, squash склеивает с предыдущим, reword меняет сообщение.",
                    code = "git rebase -i HEAD~3",
                ),
                KnowledgeEntry(
                    title = "git rebase --continue",
                    summary = "Продолжает rebase после ручного разрешения конфликта.",
                    detail = "Перед этим добавьте исправленные файлы через git add. git rebase --abort отменяет всё.",
                    code = "git rebase --continue",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Совместная работа",
            entries = listOf(
                KnowledgeEntry(
                    title = "git remote -v",
                    summary = "Показывает адреса подключённых удалённых репозиториев.",
                    detail = null,
                    code = "git remote -v",
                ),
                KnowledgeEntry(
                    title = "git fetch",
                    summary = "Скачивает новые коммиты с сервера, не меняя ваши файлы.",
                    detail = "--prune убирает ссылки на ветки, удалённые на сервере.",
                    code = "git fetch --prune",
                ),
                KnowledgeEntry(
                    title = "git pull --rebase",
                    summary = "Забирает изменения и ставит ваши коммиты поверх них.",
                    detail = "История остаётся линейной, без лишних коммитов слияния.",
                    code = "git pull --rebase origin main",
                ),
                KnowledgeEntry(
                    title = "git push -u",
                    summary = "Отправляет ветку на сервер и запоминает связь с ней.",
                    detail = "После -u хватает простого git push. Вместо --force используйте --force-with-lease — он не затрёт чужие коммиты.",
                    code = "git push -u origin feature/login",
                ),
                KnowledgeEntry(
                    title = "git push origin --delete",
                    summary = "Удаляет ветку на сервере.",
                    detail = "Локальную ветку это не трогает — её удаляют через git branch -d.",
                    code = "git push origin --delete feature/login",
                ),
                KnowledgeEntry(
                    title = "git tag",
                    summary = "Ставит метку на коммит, обычно номер версии.",
                    detail = "-a создаёт аннотированный тег с сообщением. Теги не отправляются сами: нужен git push origin --tags.",
                    code = "git tag -a v1.2.0 -m \"Релиз 1.2.0\"",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "История и откат",
            entries = listOf(
                KnowledgeEntry(
                    title = "git reset --soft",
                    summary = "Отменяет коммиты, оставляя их изменения в индексе.",
                    detail = "Удобно, чтобы пересобрать несколько коммитов в один.",
                    code = "git reset --soft HEAD~1",
                ),
                KnowledgeEntry(
                    title = "git reset --hard",
                    summary = "Возвращает ветку и рабочие файлы к указанному коммиту.",
                    detail = "Незакоммиченные правки удаляются безвозвратно. Потерянные коммиты ещё можно найти через reflog.",
                    code = "git reset --hard origin/main",
                ),
                KnowledgeEntry(
                    title = "git revert",
                    summary = "Создаёт коммит, обратный указанному.",
                    detail = "Безопасный откат для общей ветки: история не переписывается.",
                    code = "git revert a1b2c3d",
                ),
                KnowledgeEntry(
                    title = "git cherry-pick",
                    summary = "Переносит отдельный коммит из другой ветки в текущую.",
                    detail = "-x дописывает в сообщение хэш исходного коммита.",
                    code = "git cherry-pick -x a1b2c3d",
                ),
                KnowledgeEntry(
                    title = "git reflog",
                    summary = "Показывает все перемещения HEAD, включая отменённые.",
                    detail = "Спасает после неудачного reset или rebase: найдите нужный хэш и вернитесь через git reset --hard.",
                    code = "git reflog -n 20",
                ),
                KnowledgeEntry(
                    title = "git log --author",
                    summary = "Показывает коммиты конкретного автора.",
                    detail = "--since \"2 weeks ago\" ограничивает период, -S \"текст\" ищет коммиты, где менялась строка.",
                    code = "git log --author=\"Anna\" --oneline",
                ),
            ),
        ),
        KnowledgeGroup(
            title = "Служебное",
            entries = listOf(
                KnowledgeEntry(
                    title = "git stash",
                    summary = "Прячет незакоммиченные изменения и очищает рабочий каталог.",
                    detail = "git stash pop возвращает спрятанное, -u прячет и неотслеживаемые файлы, git stash list показывает стек.",
                    code = "git stash push -u -m \"черновик формы\"",
                ),
                KnowledgeEntry(
                    title = "git clean -fd",
                    summary = "Удаляет неотслеживаемые файлы и каталоги.",
                    detail = "Сначала запустите git clean -nd: он покажет, что будет удалено, ничего не трогая.",
                    code = "git clean -fd",
                ),
                KnowledgeEntry(
                    title = "git blame",
                    summary = "Показывает автора и коммит для каждой строки файла.",
                    detail = "-L 10,30 ограничивает диапазон строк.",
                    code = "git blame -L 10,30 src/app.js",
                ),
                KnowledgeEntry(
                    title = "git bisect",
                    summary = "Находит коммит с ошибкой двоичным поиском по истории.",
                    detail = "Отмечайте каждый проверенный коммит через git bisect good или bad. git bisect reset завершает поиск.",
                    code = "git bisect start HEAD v1.0.0",
                ),
            ),
        ),
    ),
)
