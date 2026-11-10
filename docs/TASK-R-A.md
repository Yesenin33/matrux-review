# TASK-R-A — каталог без галочек, подписка без «Знакомства», раздел «Знания» пустой

Проект: `/root/palma`. Модель: claude-opus-5-5.

## 1. Каталог: метка Pro-систем

Файл `app/src/main/java/ru/palma/app/ui/screens/CatalogScreen.kt`, функция `Tag(row: CatalogRow)`.

Сейчас при `row.image.pro` рисуется `Lucide.BadgeCheck` (галочка).

Требуется:
- если у пользователя **есть подписка Pro** — у Pro-систем не рисовать ничего вместо галочки (ни галочку, ни замок, ни текст). Метки `Запущено` / `Установлено` / `Бесплатно` остаются как есть.
- если подписки **нет** — вместо галочки рисовать замок: `LucideIcon(Lucide.Lock, Palma.colors.muted, size = Palma.space.iconSmall)`.
- признак наличия подписки уже есть в `CatalogUi`/состоянии (`paid` или `row.locked` — проверь по коду `app/src/main/java/ru/palma/app/ui/state/`). Если в `CatalogRow` нужного поля нет, добавь `paid: Boolean` и заполни при сборке `CatalogUi`.
- ветка `row.locked` сейчас рисует замок — она должна остаться, но не должна конфликтовать с новой логикой (порядок `when`: running → installed → locked → pro). Поведение: без подписки Pro-система показывает замок ровно один раз.
- Никаких текстовых подсказок рядом с меткой.

## 2. Подписка: убрать «Знакомство с Matrux»

Файл `app/src/main/java/ru/palma/app/ui/screens/SubscriptionScreen.kt`, около строки 147: строка `subs_onboarding`.

Требуется:
- удалить эту строку из экрана подписки и все связанные параметры (`onOnboarding`/`replay`-колбэк) из `SubscriptionActions`/вызовов в `MainActivity.kt`.
- строку `subs_onboarding` из `app/src/main/res/values/strings.xml` удалить, если она больше не используется; плюралы/ключи-сироты не оставлять.
- остальной экран подписки (тарифы, покупка) не меняется.

## 3. Раздел «Знания» — пустой экран

Файл `app/src/main/java/ru/palma/app/ui/screens/KnowledgeScreen.kt`, маршрут `Route.KNOWLEDGE` в `MainActivity.kt`.

Требуется:
- на экране остаётся только заголовок «Знания» (`ScreenTitle` с `R.string.knowledge_title`), ниже пусто. Ни курсов, ни шпаргалок, ни поиска, ни блоков.
- весь текущий контент (курсы, шпаргалки) убрать из UI. Данные в `app/src/main/assets/courses/` и `app/src/main/assets/sheets/` **не удалять** — они понадобятся позже.
- автооткрытие окна Pro на входе в раздел (`offerOnEnter`) убрать вместе с контентом.
- неиспользуемые после правки composable-функции в `KnowledgeScreen.kt` удалить, чтобы не оставалось мёртвого кода; `SectionScreen`, `sectionIcon`, `courseIcon` могут понадобиться задачe R-B — если они больше нигде не используются, оставь их в файле и не удаляй, но отметь это в отчёте.
- тесты стенда, которые ловят старый контент знаний, поправить: `app/src/test/java/ru/palma/app/stand/StandShots.kt` (снимки `09-knowledge-*`, `10-course-*`, `11-lesson-*`, `12-sources`) и `HomeTermsTest`/`ScreenStatesTest`, если падают.

## Границы

- Не трогать `dist/`, `art/`, `stand/`, `image-build/*rootfs`, `backend/data/images`, `backend/data/*.sqlite3`.
- Ни одной русской строки в Kotlin — только `strings.xml`.
- Никаких подсказок и пояснений в интерфейсе.
- Прогон Gradle — по одной задаче за раз (на машине ~3.8 ГБ RAM): сначала `./gradlew assembleDebug`, потом тесты.

## Что сделать перед сдачей

1. `./gradlew assembleDebug` — сборка обязана пройти.
2. Прогнать затронутые тесты, обновить эталонные снимки стенда, если они менялись осознанно.
3. Отчёт по форме: «доказано командой + вывод» / «не проверено». В отчёте перечислить: какие файлы правлены, что удалено из строк, какие тесты и с каким кодом прошли.
