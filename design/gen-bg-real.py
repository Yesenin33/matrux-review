#!/usr/bin/env python3
"""Превью реальных фонов Matrux: паттерны Hero Patterns (CC BY), текстуры Poly Haven (CC0), фото (CC0)."""
import os
from playwright.sync_api import sync_playwright
from PIL import Image

SRC = "/root/.hermes/cache/scratch/real-bg"
OUT = "/root/palma/design/shots"

CSS = """
@font-face{font-family:Manrope;src:url("file:///root/palma/tmp-assets/Manrope");font-weight:200 800}
:root{--canvas:#f5f5f7;--surface:#fff;--alt:#fafafa;--ink:#1d1d1f;--ink-soft:#515154;--muted:#6e6e73;--accent:#0071e3;--tint:#eaf2fd}
*{box-sizing:border-box;margin:0;padding:0}
body{background:#e4e4ea;font-family:Manrope,system-ui;color:var(--ink);padding:26px;display:flex;flex-wrap:wrap;gap:26px 30px}
.col{display:flex;flex-direction:column;align-items:center;gap:8px}
.tag{font-size:11.5px;font-weight:600;color:#3a3a3c}
.sub{font-size:10px;color:#6e6e73}
.phone{zoom:1.28;width:276px;height:592px;background:var(--canvas);border-radius:32px;overflow:hidden;position:relative;
box-shadow:0 2px 6px rgba(0,0,0,.06),0 18px 40px rgba(0,0,0,.1)}
.status{height:26px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;font-size:9px;color:var(--muted);position:relative;z-index:3}
.screen{position:absolute;inset:0;display:flex;flex-direction:column}
.body{padding:0 14px;flex:1;position:relative;z-index:3}
.bg{position:absolute;inset:0;z-index:1;pointer-events:none}
.bg>*{position:absolute;inset:0;display:block}
.scrim{position:absolute;inset:0;z-index:2;pointer-events:none}
.display{font-size:22px;line-height:25px;font-weight:600;letter-spacing:-.02em}
.title{font-size:14px;line-height:18px;font-weight:600}
.body-t{font-size:10.5px;line-height:15px}
.cap{font-size:8.5px;line-height:11px;color:var(--muted)}
.muted{color:var(--muted)}
.card{background:var(--surface);border-radius:14px;padding:14px;box-shadow:0 1px 2px rgba(0,0,0,.04),0 5px 16px rgba(0,0,0,.055)}
.row{display:flex;align-items:center;gap:11px}
.tile{width:32px;height:32px;border-radius:9px;background:var(--tint);display:flex;align-items:center;justify-content:center;flex:none}
.tile svg{width:15px;height:15px;stroke:var(--accent);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.grow{flex:1}.chev{width:13px;height:13px;stroke:var(--muted);fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round}
.stack>*+*{margin-top:9px}
.btn{height:44px;border-radius:10px;background:var(--accent);color:#fff;display:flex;align-items:center;justify-content:center;font-size:11.5px;font-weight:600}
.cluster{font-size:10.5px;line-height:15px;font-weight:600;color:var(--muted)}
"""

HERO = """<div class="body">
  <div class="display">Ubuntu + XFCE</div>
  <div class="body-t muted" style="margin-top:4px">XFCE · образ 277,9 МБ · 8,3 ГБ свободно</div>
  <div class="stack" style="margin-top:12px"><div class="btn">Запустить систему</div>
    <div class="body-t" style="text-align:center;color:var(--accent)">Сменить систему</div></div>
  <div class="title" style="margin-top:16px">Разделы</div>
  <div class="cluster" style="margin-top:10px">Начало</div>
  <div class="stack" style="margin-top:8px">
    <div class="card row"><span class="tile"><svg viewBox="0 0 24 24"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/></svg></span>
      <div class="grow"><div class="body-t" style="font-weight:600">Первые шаги</div><div class="cap" style="margin-top:2px">12 записей</div></div>
      <svg class="chev" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg></div>
    <div class="card row"><span class="tile"><svg viewBox="0 0 24 24"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2z"/></svg></span>
      <div class="grow"><div class="body-t" style="font-weight:600">Файлы и каталоги</div><div class="cap" style="margin-top:2px">24 записи</div></div>
      <svg class="chev" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg></div>
    <div class="card row"><span class="tile"><svg viewBox="0 0 24 24"><path d="M4 17l6-6-6-6"/><path d="M12 19h8"/></svg></span>
      <div class="grow"><div class="body-t" style="font-weight:600">Команды Linux</div><div class="cap" style="margin-top:2px">36 записей</div></div>
      <svg class="chev" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg></div>
  </div>
</div>"""

FADE = ("linear-gradient(180deg,rgba(245,245,247,0) 0%,rgba(245,245,247,.25) 46%,"
        "rgba(245,245,247,.78) 70%,#f5f5f7 84%)")
TOPDOWN = ("linear-gradient(180deg,rgba(245,245,247,.10) 0%,rgba(245,245,247,.45) 55%,#f5f5f7 86%)")
SOFT = "linear-gradient(180deg,rgba(245,245,247,0) 0%,rgba(245,245,247,.30) 58%,#f5f5f7 88%)"

VARIANTS = [
    ("B1", "схема, серая 12 %", "Hero Patterns · CC BY", "circuitBoard",
     f'<i style="background-image:url(&quot;file://{SRC}/circuitBoard.svg&quot;);background-size:130px;'
     f'opacity:.12"></i>', TOPDOWN),
    ("B2", "схема, голубая 22 %", "Hero Patterns · CC BY", "circuitBoard-blue",
     f'<i style="background-image:url(&quot;file://{SRC}/circuitBoard-blue.svg&quot;);background-size:130px;'
     f'opacity:.22"></i>', TOPDOWN),
    ("B3", "схема, графит 18 %", "Hero Patterns · CC BY", "circuitBoard-ink",
     f'<i style="background-image:url(&quot;file://{SRC}/circuitBoard-ink.svg&quot;);background-size:160px;'
     f'opacity:.18"></i>', TOPDOWN),
]


def page(key, name, lic, base, bg, scrim):
    return (f'<div class="col"><div class="phone"><div class="screen">'
            f'<div class="status"><span>22:41</span><span>LTE · 84 %</span></div>'
            f'<div class="bg">{bg}</div><div class="scrim" style="background:{scrim}"></div>{HERO}'
            f'</div></div><div class="tag">{key} — {name}</div><div class="sub">{lic} · {base}</div></div>')


html = ('<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>' + CSS + '</style></head><body>'
        + "".join(page(*v) for v in VARIANTS) + '</body></html>')
path = "/root/palma/design/bg-real.html"
open(path, "w", encoding="utf-8").write(html)

os.makedirs(OUT, exist_ok=True)
with sync_playwright() as p:
    br = p.chromium.launch(executable_path="/usr/bin/google-chrome",
                           args=["--no-sandbox", "--disable-dev-shm-usage", "--no-zygote",
                                 "--in-process-gpu", "--enable-unsafe-swiftshader",
                                 "--allow-file-access-from-files"])
    pg = br.new_page(viewport={"width": 1100, "height": 1900}, device_scale_factor=2)
    pg.goto("file://" + path)
    pg.wait_for_timeout(1800)
    boxes = pg.evaluate("""() => Array.from(document.querySelectorAll('.phone')).map(e => {
        const r = e.getBoundingClientRect(); return {x: r.x + scrollX, y: r.y + scrollY, w: r.width, h: r.height};})""")
    full = "/root/palma/design/_real.png"
    pg.screenshot(path=full, full_page=True)
    br.close()

im = Image.open(full).convert("RGB")
for i, b in enumerate(boxes):
    x, y = int(b["x"] * 2) - 4, int(b["y"] * 2) - 4
    box = (max(x, 0), max(y, 0), int((b["x"] + b["w"]) * 2) + 4, int((b["y"] + b["h"]) * 2) + 4)
    dst = os.path.join(OUT, f"real-{VARIANTS[i][0]}.png")
    im.crop(box).save(dst)
    print("SHOT", dst, os.path.getsize(dst))
