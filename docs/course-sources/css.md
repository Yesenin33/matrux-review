# CSS — рабочий разбор источников

Обращение: 2026-11-10. В приложение файл не попадает.

## Источники

| Источник | Ссылка | Лицензия |
|---|---|---|
| MDN: основы оформления | https://developer.mozilla.org/ru/docs/Learn_web_development/Core/Styling_basics | CC BY-SA 2.5 |
| MDN: блочная модель | https://developer.mozilla.org/ru/docs/Learn_web_development/Core/Styling_basics/Box_model | CC BY-SA 2.5 |
| MDN: основы flexbox | https://developer.mozilla.org/ru/docs/Web/CSS/CSS_flexible_box_layout/Basic_concepts_of_flexbox | CC BY-SA 2.5 |
| MDN: основы grid | https://developer.mozilla.org/ru/docs/Web/CSS/CSS_grid_layout/Basic_concepts_of_grid_layout | CC BY-SA 2.5 |
| MDN: медиазапросы | https://developer.mozilla.org/ru/docs/Web/CSS/CSS_media_queries/Using_media_queries | CC BY-SA 2.5 |
| MDN: CSS-анимации | https://developer.mozilla.org/ru/docs/Web/CSS/CSS_animations/Using_CSS_animations | CC BY-SA 2.5 |
| webref.ru, справочник CSS | https://webref.ru/css | CC BY-NC 4.0 (только сверка) |

## Конспект

- Подключение: `<link rel="stylesheet" href>`, `<style>` в `head`, атрибут `style` (крайний случай).
- Селекторы: тег, `.класс`, `#id`, потомок `a b`, дочерний `a > b`, атрибут `[type="email"]`, псевдоклассы `:hover`, `:first-child`, `:nth-child(2n)`.
- Каскад: важнее специфичность (id > класс > тег), при равной — позже объявленное правило; наследуются в основном текстовые свойства.
- Блочная модель: `content` → `padding` → `border` → `margin`; по умолчанию `box-sizing: content-box` — `width` без полей и рамки; `border-box` включает их. Вертикальные `margin` соседних блоков схлопываются.
- Шрифт и цвет: `font-family` со списком запасных и общим семейством в конце; `font-size`, `line-height` без единиц; `color`, `background-color`; `rem` относительно корня.
- Flex: `display: flex` на контейнере; главная ось `flex-direction`; `justify-content` по главной оси, `align-items` по поперечной; `gap`; `flex: 1` у элемента делит свободное место.
- Grid: `display: grid`, `grid-template-columns: repeat(3, 1fr)`, `fr` — доля свободного места; `gap`; `grid-column: span 2`.
- Адаптив: `<meta name="viewport">`, `@media (min-width: 600px) {}`; подход «сначала мобильный».
- Анимация: `transition: opacity 260ms` (перечислять свойства, не `all`); `@keyframes` + `animation`; анимировать дешёвые `transform` и `opacity`; `@media (prefers-reduced-motion: reduce)`.

## Проверка

Живая проверка в WebView через `getComputedStyle`: например, `getComputedStyle(document.querySelector('.box')).display === 'flex'`.

## Карта уроков

1. Начало (открыт): подключение стилей · селекторы.
2. Блок: каскад и специфичность · блочная модель.
3. Текст: шрифт и цвет.
4. Раскладка: flex · grid.
5. Адаптив и движение: медиазапросы · анимации.
6. Своя вёрстка.
