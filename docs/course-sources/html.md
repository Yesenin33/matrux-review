# HTML — рабочий разбор источников

Обращение: 2026-11-10. В приложение файл не попадает.

## Источники

| Источник | Ссылка | Лицензия |
|---|---|---|
| MDN Web Docs: структурирование содержимого | https://developer.mozilla.org/ru/docs/Learn_web_development/Core/Structuring_content | CC BY-SA 2.5, атрибуция «Материалы MDN, помощники Mozilla, CC BY-SA 2.5» |
| MDN: справочник HTML | https://developer.mozilla.org/ru/docs/Web/HTML | CC BY-SA 2.5 |
| MDN: `<form>` | https://developer.mozilla.org/ru/docs/Web/HTML/Element/form | CC BY-SA 2.5 |
| MDN: доступность | https://developer.mozilla.org/ru/docs/Learn_web_development/Core/Accessibility | CC BY-SA 2.5 |
| webref.ru, справочник HTML | https://webref.ru/html | CC BY-NC 4.0 (текст не используем, только сверка) |
| html5book.ru | https://html5book.ru/ | © автора |

Лицензия MDN проверена на https://developer.mozilla.org/en-US/docs/MDN/Writing_guidelines/Attrib_copyright_license — проза под CC-BY-SA v2.5.

## Конспект

- Документ: `<!DOCTYPE html>`, `<html lang="ru">`, `<head>` с `<meta charset="utf-8">`, `<meta name="viewport" ...>`, `<title>`; содержимое в `<body>`.
- Текст: заголовки `h1`–`h6` задают структуру, один `h1` на страницу; `p`, `strong` (важность), `em` (акцент), `br` только для переноса внутри смысловой единицы.
- Ссылки: `<a href>`; абсолютные и относительные адреса; `#id` для якоря; `target="_blank"` с `rel="noopener"`.
- Изображения: `<img src alt width height>`; `alt` обязателен, пустой `alt=""` для декоративных; `<figure>`/`<figcaption>`.
- Списки: `ul` (порядок не важен), `ol` (важен), `li`; вложенные списки внутри `li`.
- Таблицы: только для табличных данных; `table`, `caption`, `thead`, `tbody`, `tr`, `th` (с `scope`), `td`.
- Формы: `form action method`; `label for` ↔ `input id`; типы `text`, `email`, `number`, `checkbox`, `radio`; `name` нужен для отправки; `required`; `button type="submit"`.
- Семантика: `header`, `nav`, `main`, `article`, `section`, `aside`, `footer` — смысл для браузера, поиска и экранного диктора.
- Доступность: текстовая альтернатива, подписи полей, порядок заголовков без пропусков, кнопка — это `button`, а не `div` с обработчиком.

## Проверка

Задачи с живой проверкой: код идёт в WebView приложения, проверка — выражение JS по DOM (например, `document.querySelector('label[for=email]')`).

## Карта уроков

1. Начало (открыт): каркас документа · заголовки и абзацы.
2. Текст и ссылки: выделение текста · ссылки · изображения.
3. Списки и таблицы: списки · таблицы.
4. Формы: поля и подписи · выбор и отправка.
5. Семантика и доступность: разметка областей · доступность.
6. Своя страница.
