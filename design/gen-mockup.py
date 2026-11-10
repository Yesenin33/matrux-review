#!/usr/bin/env python3
"""Сборка макета Matrux из реального контента knowledge/*.kt и съёмка в PNG."""
import glob
import os
import re

KDIR = "/root/palma/app/src/main/java/ru/palma/app/ui/knowledge"
OUT = "/root/palma/design/shots"
LOGO = "file:///root/palma/app/src/main/res/drawable-xxxhdpi"
ICONS = {
    "start": "M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09|M9 12a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.4 22.4 0 0 1-4 2z",
    "files": "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z",
    "linux": "M4 17l6-6-6-6|M12 19h8",
    "terms": "M12 5v16|M20.001 19A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2 5 5 0 0 1 4-2z",
    "packages": "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z|M12 22V12",
    "users": "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2|M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z",
    "services": "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2",
    "network": "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z|M2 12h20|M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20z",
    "disks": "M2.212 11.577a2 2 0 0 0-.212.896V18a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-5.527a2 2 0 0 0-.212-.896L18.55 5.11A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z|M21.946 12.013H2.054",
    "logs": "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z|M14 2v5a1 1 0 0 0 1 1h5|M16 13H8",
    "scripts": "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z|M10 12.5 8 15l2 2.5|m14 12.5 2 2.5-2 2.5",
    "git": "M6 3v12|M18 9a9 9 0 0 1-9 9|M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z|M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
    "hardware": "M12 20v2|M12 2v2|M17 20v2|M17 2v2|M7 20v2|M7 2v2",
    "languages": "m16 18 6-6-6-6|m8 6-6 6 6 6",
}

PENG = ("M28.068 31.355c-2.229 0-8.468.785-10.068 1.832-1.601-1.047-7.84-1.832-10.069-1.832"
        "-2.564 0-1.161 1.039-1.161 2.322C6.77 34.96 5.367 36 7.931 36c2.229 0 8.468-.785 10.069-1.832"
        "C19.601 35.215 25.84 36 28.068 36c2.565 0 1.161-1.04 1.161-2.322 0-1.283 1.405-2.323-1.161-2.323z")
PENG_BODY = ("M31.73 15.866c-1.25-2.499-3.152-4.995-4.942-6.723C24.337 3.711 20.759 0 18 0s-6.337 3.71"
             "-8.788 9.143c-1.791 1.729-3.693 4.224-4.943 6.724-2.438 4.876-3.116 11.426-2.078 11.944"
             ".873.437 2.324-1.552 4.208-5.082C6.667 33.604 13.446 33.678 18 33.678c4.553 0 11.333-.073"
             " 11.601-10.947 1.884 3.528 3.335 5.517 4.207 5.08 1.038-.519.361-7.069-2.078-11.945z")
PENG_BELLY = ("M21.675 6.943c-.85.607-2.172 1.186-3.675 1.186s-2.825-.578-3.675-1.185c-3.302 2.137"
              "-5.615 7.06-5.615 12.798 0 7.695 4.159 13.936 9.29 13.936 5.132 0 9.291-6.24 9.291-13.936"
              " 0-5.738-2.313-10.662-5.616-12.799z")


def parse():
    secs = []
    for path in sorted(glob.glob(os.path.join(KDIR, "*Content.kt"))):
        s = open(path, encoding="utf-8").read()
        secs.append(dict(
            id=re.search(r'id\s*=\s*"([^"]+)"', s).group(1),
            title=re.search(r'title\s*=\s*"([^"]+)"', s).group(1),
            pro=re.search(r'pro\s*=\s*(true|false)', s).group(1) == "true",
            branch="START" if re.search(r"Branch\.START", s) else "DEEPER",
            count=s.count("KnowledgeEntry("),
        ))
    order = ["start", "files", "linux", "terms", "packages", "users", "services", "network",
             "disks", "logs", "scripts", "git", "hardware", "languages"]
    secs.sort(key=lambda x: order.index(x["id"]) if x["id"] in order else 99)
    return secs


CSS = """
@font-face{font-family:Manrope;src:url("file:///root/palma/tmp-assets/Manrope");font-weight:200 800;font-display:block}
@font-face{font-family:Mono;src:url("file:///root/palma/tmp-assets/JetBrainsMono-Medium.ttf");font-weight:500;font-display:block}
:root{--canvas:#f5f5f7;--surface:#fff;--alt:#fafafa;--ink:#1d1d1f;--ink-soft:#515154;--muted:#6e6e73;
--accent:#0071e3;--tint:#eaf2fd;--success:#1d8a4e;--hair:#d2d2d7}
*{box-sizing:border-box;margin:0;padding:0}
body{background:#e9e9ee;font-family:Manrope,system-ui;color:var(--ink);padding:40px;display:block}
.wrap{width:414px}
.phone{zoom:1.5;width:276px;height:592px;background:var(--canvas);border-radius:32px;overflow:hidden;position:relative;
box-shadow:0 2px 6px rgba(0,0,0,.06),0 18px 40px rgba(0,0,0,.10)}
.status{height:26px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;font-size:9px;color:var(--muted)}
.screen{position:absolute;inset:0;display:flex;flex-direction:column}
.body{padding:0 14px;flex:1;overflow:hidden}
.display{font-size:22px;line-height:25px;font-weight:600;letter-spacing:-.02em}
.title{font-size:14px;line-height:18px;font-weight:600;letter-spacing:-.01em}
.body-t{font-size:10.5px;line-height:15px}
.strong{font-weight:600}
.cap{font-size:8.5px;line-height:11px;color:var(--muted)}
.muted{color:var(--muted)}.soft{color:var(--ink-soft)}
.card{background:var(--surface);border-radius:14px;padding:14px;box-shadow:0 1px 2px rgba(0,0,0,.04),0 5px 16px rgba(0,0,0,.055)}
.row{display:flex;align-items:center;gap:11px}
.tile{width:32px;height:32px;border-radius:9px;background:var(--tint);display:flex;align-items:center;justify-content:center;flex:none}
.tile img{width:16px;height:16px;object-fit:contain}
.tile svg{width:15px;height:15px;stroke:var(--accent);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.grow{flex:1;min-width:0}
.chev{width:13px;height:13px;stroke:var(--muted);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round;flex:none}
.lock{width:12px;height:12px;stroke:var(--muted);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round;flex:none}
.bar{display:flex;align-items:center;gap:6px;height:30px;padding:0 12px;background:var(--alt);border-radius:9px}
.bar svg{width:12px;height:12px;stroke:var(--muted);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.code{font-family:Mono;font-size:9px;line-height:13px;color:var(--accent);background:var(--tint);border-radius:8px;padding:6px 8px;margin-top:7px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.cmd{font-family:Mono;font-size:10px;font-weight:500}
.copy{width:12px;height:12px;stroke:var(--muted);fill:none;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round;flex:none}
.ok{color:var(--success);font-size:8.5px;font-weight:600}
.entry+.entry{margin-top:11px}
.nav{position:absolute;z-index:9;left:0;right:0;bottom:0;height:52px;display:flex;align-items:flex-end;justify-content:space-around;padding-bottom:8px;background:var(--surface);box-shadow:0 -1px 0 rgba(0,0,0,.04)}
.nav div{display:flex;flex-direction:column;align-items:center;gap:3px;font-size:8px;color:var(--muted)}
.nav .on{color:var(--accent)}
.nav svg{width:15px;height:15px;stroke:currentColor;fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.pill{display:inline-block;font-size:8px;font-weight:600;padding:2px 7px;border-radius:99px;background:#e7f4ec;color:var(--success);white-space:nowrap}
.pill.pro{background:#ececef;color:var(--muted)}
.subrow{background:var(--alt);border-radius:9px;padding:8px 11px;margin-top:6px}
.subrow.on{background:var(--tint)}
.btn{height:44px;border-radius:10px;background:var(--accent);color:#fff;display:flex;align-items:center;justify-content:center;font-size:11.5px;font-weight:600}
.back{display:flex;align-items:center;gap:6px;height:34px;color:var(--accent);font-size:10.5px}
.back svg{width:13px;height:13px;stroke:currentColor;fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.term{display:flex;align-items:center;gap:9px}
.term .t{font-size:11.5px;font-weight:600}
.price{font-size:11px;font-weight:600}
.peng{width:58px;height:58px;border-radius:99px;background:var(--tint);display:flex;align-items:center;justify-content:center;flex:none}
.peng svg{width:34px;height:34px}
.stack>*+*{margin-top:9px}
.aurora{position:absolute;inset:0;overflow:hidden;z-index:0}
.aurora i{position:absolute;border-radius:50%}
.aurora .a{width:290px;height:290px;left:-70px;top:-140px;background:radial-gradient(circle,rgba(185,210,244,.42),rgba(185,210,244,0) 70%)}
.aurora .b{width:240px;height:240px;right:-90px;top:120px;background:radial-gradient(circle,rgba(228,231,237,.85),rgba(228,231,237,0) 70%)}
.screen>*{position:relative;z-index:1}
.cluster{font-size:10.5px;line-height:15px;font-weight:600;color:var(--muted)}
"""

CHEV = '<svg class="chev" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg>'
LOCK = ('<svg class="lock" viewBox="0 0 24 24"><path d="M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5'
        'a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>')
COPY = ('<svg class="copy" viewBox="0 0 24 24"><path d="M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H10'
        'a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10'
        'c1.1 0 2 .9 2 2"/></svg>')
BADGE = ('<svg class="copy" style="stroke:var(--accent)" viewBox="0 0 24 24"><path d="M3.85 8.62a4 4 0 0 1 4.78-4.77'
         ' 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0'
         ' 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76z"/><path d="m16 9-5.5 5.5L8 12"/></svg>')
SEARCH = ('<svg viewBox="0 0 24 24"><path d="M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z"/>'
          '<path d="m21 21-4.34-4.34"/></svg>')
BACK = '<svg viewBox="0 0 24 24"><path d="m15 18-6-6 6-6"/></svg>'
NAV = {
    "home": '<path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
    "list": '<path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h10"/>',
    "card": '<path d="M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"/><path d="M2 10h20"/>',
}


def phone(inner, name, nav=None):
    body = f'<div class="wrap" data-name="{name}"><div class="phone">{inner}'
    if nav:
        items = "".join(
            f'<div class="{"on" if k == nav else ""}"><svg viewBox="0 0 24 24">{v}</svg>{t}</div>'
            for k, v, t in [("home", NAV["home"], "Главная"), ("list", NAV["list"], "Каталог"),
                            ("card", NAV["card"], "Подписка")]
        )
        body += f'<div class="nav">{items}</div>'
    body += '</div></div>'
    return body


def logo_tile(file):
    return (f'<span class="tile" style="background:var(--alt)">'
            f'<img src="{LOGO}/{file}" alt=""/></span>')


def ic_tile(sid):
    paths = "".join(f'<path d="{d}"/>' for d in ICONS.get(sid, "M4 7h16").split("|"))
    return f'<span class="tile"><svg viewBox="0 0 24 24">{paths}</svg></span>'


def count_word(n):
    if n % 10 == 1 and n % 100 != 11:
        return "запись"
    if 2 <= n % 10 <= 4 and not 12 <= n % 100 <= 14:
        return "записи"
    return "записей"


def card_row(s, paid):
    badge = LOCK if (s["pro"] and not paid) else CHEV
    return (f'<div class="card row">{ic_tile(s["id"])}<div class="grow">'
            f'<div class="body-t strong">{s["title"]}</div>'
            f'<div class="cap" style="margin-top:2px">{s["count"]} {count_word(s["count"])}</div>'
            f'</div>{badge}</div>')


def screen_home(secs):
    start = [s for s in secs if s["branch"] == "START"]
    rows = "".join(card_row(s, False) for s in start)
    inner = f'''<div class="aurora"><i class="a"></i><i class="b"></i></div>
    <div class="screen"><div class="status"><span>22:41</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="display">Ubuntu + XFCE</div>
      <div class="body-t muted" style="margin-top:4px">XFCE · образ 277,9 МБ · 8,3 ГБ свободно</div>
      <div class="stack" style="margin-top:12px"><div class="btn">Запустить систему</div>
        <div class="body-t" style="text-align:center;color:var(--accent)">Сменить систему</div></div>
      <div class="title" style="margin-top:16px">Разделы</div>
      <div class="cluster" style="margin-top:10px">Начало</div>
      <div class="stack" style="margin-top:8px">{rows}</div>
    </div></div>'''
    return phone(inner, "01-home", nav="home")


def screen_sections(secs):
    start = [s for s in secs if s["branch"] == "START"]
    deep = [s for s in secs if s["branch"] == "DEEPER"]
    rows1 = "".join(card_row(s, False) for s in start)
    rows2 = "".join(card_row(s, False) for s in deep[:4])
    inner = f'''<div class="screen"><div class="status"><span>22:42</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="display">Разделы</div>
      <div class="cluster" style="margin-top:12px">Начало</div>
      <div class="stack" style="margin-top:8px">{rows1}</div>
      <div class="cluster" style="margin-top:13px">Дальше</div>
      <div class="stack" style="margin-top:8px">{rows2}</div>
    </div></div>'''
    return phone(inner, "02-sections")


def screen_files(secs):
    inner = f'''<div class="screen"><div class="status"><span>22:43</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="back">{BACK}Назад</div>
      <div class="display">Файлы и каталоги</div>
      <div class="bar" style="margin-top:10px">{SEARCH}<span class="body-t muted">Поиск по командам и терминам</span></div>
      <div class="row" style="justify-content:space-between;margin-top:13px">
        <div class="body-t strong">Навигация</div><div class="cap">6 записей</div></div>
      <div class="card" style="margin-top:8px">
        <div class="entry"><div class="row"><div class="grow"><span class="cmd">pwd</span></div>{COPY}</div>
          <div class="body-t soft" style="margin-top:5px">Показывает полный путь текущего каталога.</div>
          <div class="code">pwd</div></div>
        <div class="entry"><div class="row"><div class="grow"><span class="cmd">cd -</span></div><span class="ok">Скопировано</span></div>
          <div class="body-t soft" style="margin-top:5px">Возвращает в предыдущий каталог.</div>
          <div class="code">cd -</div></div>
        <div class="entry"><div class="row"><div class="grow"><span class="cmd">ls -la</span></div>{COPY}</div>
          <div class="body-t soft" style="margin-top:5px">Все файлы, включая скрытые, с правами и датой.</div>
          <div class="code">ls -la ~</div></div>
      </div>
    </div></div>'''
    return phone(inner, "03-files")


def screen_pro(secs):
    inner = f'''<div class="screen"><div class="status"><span>22:44</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="back">{BACK}Назад</div>
      <div class="row"><div class="peng"><svg viewBox="0 0 36 36">
        <path fill="#1d1d1f" d="{PENG}"/><path fill="#1d1d1f" d="{PENG_BODY}"/>
        <path fill="#ffffff" d="{PENG_BELLY}"/></svg></div>
        <div class="display">Pro</div></div>
      <div class="card" style="margin-top:14px">
        <div class="term subrow on"><div class="body-t">1 месяц</div><div class="grow"></div>
          {BADGE}<div class="price" style="margin-left:6px">349 ₽</div></div>
        <div class="term subrow"><div class="body-t">3 месяца</div><div class="grow"></div>
          <span class="pill">−15 %</span><div class="price" style="margin-left:6px">890 ₽</div></div>
        <div class="term subrow"><div class="body-t">6 месяцев</div><div class="grow"></div>
          <span class="pill">−30 %</span><div class="price" style="margin-left:6px">1 466 ₽</div></div>
      </div>
      <div class="card" style="margin-top:12px">
        <div class="body-t soft">Базовые знания языков: Python, Go, Node.js</div>
        <div class="body-t soft" style="margin-top:7px">Основные команды git — от первого коммита до rebase</div>
        <div class="body-t soft" style="margin-top:7px">Устройство компьютера: процессор, память, шины, питание</div>
        <div class="body-t soft" style="margin-top:7px">Разделы: пакеты, права, службы, сеть, диски, логи, скрипты</div>
      </div>
    </div>
    <div style="padding:0 14px 14px"><div class="btn">Оформить Pro</div></div></div>'''
    return phone(inner, "04-pro")


def screen_subscription(secs):
    rows = "".join(
        f'<div class="term subrow{" on" if i == 2 else ""}">'
        f'<div class="body-t">{t}</div><div class="grow"></div>'
        + (f'<span class="pill">{d}</span>' if d else '')
        + f'<div class="price" style="margin-left:6px">{pr}</div></div>'
        for i, (t, pr, d) in enumerate([
            ("1 месяц", "349 ₽", ""),
            ("3 месяца", "890 ₽", "−15 %"),
            ("6 месяцев", "1 466 ₽", "−30 %"),
        ])
    )
    ben = "".join(
        f'<div class="body-t soft" style="margin-top:7px">{x}</div>'
        for x in ["Базовые знания языков: Python, Go, Node.js",
                  "Основные команды git — от первого коммита до rebase",
                  "Устройство компьютера: процессор, память, шины, питание",
                  "Разделы: пакеты, права, службы, сеть, диски, логи, скрипты"]
    )
    inner = f'''<div class="screen"><div class="status"><span>22:45</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="display">Подписка</div>
      <div class="card" style="margin-top:12px">
        <div class="row" style="justify-content:space-between">
          <div class="title">Free</div><div class="cap">текущий</div></div>
        <div class="title" style="margin-top:8px">0 ₽</div>
        <div class="body-t soft" style="margin-top:9px">Alpine и терминал</div>
        <div class="body-t soft" style="margin-top:8px">Работа без интернета</div>
      </div>
      <div class="card" style="margin-top:14px">
        <div class="row" style="justify-content:space-between">
          <div class="title">Pro</div><div class="cap">244 ₽ в месяц</div></div>
        <div style="margin-top:11px">{rows}</div>
        <div style="height:1px;background:var(--hair);margin-top:11px"></div>
        <div style="margin-top:11px">{ben}</div>
      </div>
    </div>
    <div style="padding:0 14px 14px"><div class="btn">Оформить Pro</div></div></div>'''
    return phone(inner, "05-subscription")


def screen_catalog():
    items = [
        ("Matrux Dev", "Debian 13 + VS Code · 1,1 ГБ", "pro", "logo_dev.png"),
        ("Alpine + XFCE", "Alpine · 271 МБ", "free", "logo_alpine.png"),
        ("Debian 13 + XFCE", "Debian 13 · 311 МБ", "pro", "logo_debian.png"),
        ("Ubuntu + XFCE", "Ubuntu · 265 МБ", "pro", "logo_ubuntu.png"),
        ("Arch Linux + XFCE", "Arch · 973 МБ", "pro", "logo_arch.png"),
    ]
    rows = ""
    for name, meta, kind, logo in items:
        pill = ('<span class="pill">Бесплатно</span>' if kind == "free"
                else '<span class="pill pro">Подписка Pro</span>')
        rows += (f'<div class="card row" style="margin-top:9px;align-items:flex-start">{logo_tile(logo)}'
                 f'<div class="grow"><div class="body-t strong">{name}</div>'
                 f'<div class="cap" style="margin-top:3px">{meta}</div></div>{pill}</div>')
    inner = f'''<div class="screen"><div class="status"><span>22:46</span><span>LTE · 84 %</span></div>
    <div class="body">
      <div class="display">Системы</div>
      {rows}
    </div></div>'''
    return phone(inner, "06-catalog", nav="list")


def build():
    secs = parse()
    pages = [screen_home(secs), screen_sections(secs), screen_files(secs), screen_pro(secs),
             screen_subscription(secs), screen_catalog()]
    html = ("<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
            f"<style>{CSS}</style></head><body>" + "".join(pages) + "</body></html>")
    path = "/root/palma/design/mockup-sections.html"
    open(path, "w", encoding="utf-8").write(html)
    return path, [(s["id"], s["title"], s["branch"], s["pro"], s["count"]) for s in secs]


def shoot(html_path):
    from playwright.sync_api import sync_playwright
    from PIL import Image
    os.makedirs(OUT, exist_ok=True)
    with sync_playwright() as p:
        br = p.chromium.launch(args=["--no-sandbox", "--disable-dev-shm-usage", "--single-process"])
        pg = br.new_page(viewport={"width": 520, "height": 1000}, device_scale_factor=2)
        pg.goto("file://" + html_path)
        pg.wait_for_timeout(1500)
        boxes = pg.evaluate("""() => Array.from(document.querySelectorAll('.wrap')).map(w => {
            const r = w.getBoundingClientRect();
            return {name: w.dataset.name, x: r.x + scrollX, y: r.y + scrollY, w: r.width, h: r.height};
        })""")
        full = "/root/palma/design/_full.png"
        pg.screenshot(path=full, full_page=True)
        br.close()
    im = Image.open(full).convert("RGB")
    out = []
    for b in boxes:
        x, y = int(b["x"] * 2) - 4, int(b["y"] * 2) - 4
        box = (max(x, 0), max(y, 0), int((b["x"] + b["w"]) * 2) + 4, int((b["y"] + b["h"]) * 2) + 4)
        dst = os.path.join(OUT, b["name"] + ".png")
        im.crop(box).save(dst, optimize=True)
        out.append((dst, os.path.getsize(dst)))
    return out


if __name__ == "__main__":
    path, meta = build()
    for m in meta:
        print("SEC", *m)
    for d, size in shoot(path):
        print("SHOT", d, size)
