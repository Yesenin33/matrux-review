Matrux — материалы для редизайна (обновлено 12.11.2026)
======================================================

Внутри:
- app-src/ — весь app/src/main: Kotlin (java/), ресурсы (res/), assets/ (шпаргалки и курсы в json), AndroidManifest.xml.
- build/ — app-build.gradle.kts, root-build.gradle.kts, settings.gradle.kts, gradle.properties (Compose BOM, версии, minSdk/targetSdk, версия приложения), StandShots.kt (стенд Paparazzi — как рендерятся кадры).
- screenshots/ — 55 кадров: 48 светлых + 7 тёмных, рендер Paparazzi в размере Pixel 5, 1170x2532 px (393x851 dp, xxhdpi).

Экраны (префикс в имени файла):
00-onboard .. 00-onboard-5 — онбординг, 5 слайдов
01-home-free / 01-home-pro / 01-home-locked-window — Главная (без Pro, с Pro, окно предложения подписки)
02-catalog-* — Каталог: free, offline, signature, empty, loading, switch, running, ready
03-install-* — установка дистрибутива: running, unpacking, no-network, metered, 402, failed, done, idle
04-launch-* — запуск системы: running, failed, 403
05-desktop-* — рабочий стол (VNC): waiting, lost, stuck
06-subs-* — Подписка Pro: free, pro, billing-error, sign-in, signed-in, no-store
09-knowledge-* — Знания: free, free-closed, pro
10-course-*, 11-lesson-* — урок курса: задача, успех, провал, вывод, веб-проверка
12-sources — открытая шпаргалка (Route.SECTION)
20-dark-onboard, 21-dark-home, 22-dark-catalog, 23-dark-knowledge, 24-dark-subs, 25-dark-install, 26-dark-lesson — тёмная тема

Тема:
- Светлая и тёмная палитры в ui/theme/Color.kt: LightColors / DarkColors, токены canvas, surface, surfaceAlt, ink, inkSoft, muted, accent, accentPress, accentTint, success, successTint, warning, hairline, danger, onAccent.
- MatruxTheme(dark = ...) в ui/theme/Theme.kt; darkColorScheme + переключение иконок статус-бара.
- Переключатель темы — в шапке «Главной» (HomeScreen.kt, ThemeSwitch: пилюля с солнцем и луной, выбранный сегмент — поверхность с синей иконкой). Значение хранится в prefs (`theme` = "light"/"dark", пусто = системная), состояние в AppGraph.theme, применение в MainActivity.Root().
- Тёмная тема — не инверсия: отдельные значения фона, карточек, акцента и текста; фон-плата на тёмном гасится до 5 %, в онбординге — до 10 %.

Чего в проекте НЕТ:
- Отдельного экрана «Настройки»: кнопка с ползунками в рабочем столе — панель действий VNC-сессии.
- Отдельного экрана терминала: Linux-стол целиком в DesktopScreen через VNC.
- XML-вёрстки: весь интерфейс на Jetpack Compose, одно Activity.

Навигация (MainActivity.kt):
- enum class Tab: HOME, CATALOG, KNOWLEDGE, SUBSCRIPTION — нижняя панель (ui/components/Scaffold.kt).
- enum class Route: HOME, CATALOG, INSTALL, KNOWLEDGE, SUBSCRIPTION, LAUNCH, DESKTOP, SECTION, COURSE, LESSON, SOURCES — стек в AnimatedContent.
- CTA на Главной («Установите дистрибутив...») ведёт на Tab.CATALOG.

Типографика и компоненты: ui/theme/Type.kt (Inter), Shape.kt, Motion.kt; ui/components/: Backdrop.kt (фон-плата), Controls.kt (PrimaryButton, GhostButton, Card), Icons.kt (Lucide), Scaffold.kt (нижняя навигация), Terms.kt.
