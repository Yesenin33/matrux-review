#!/usr/bin/env python3
"""Четыре варианта лёгкого заднего фона для главной Matrux."""
import os
from playwright.sync_api import sync_playwright
from PIL import Image

OUT = "/root/palma/design/shots"

CSS = """
@font-face{font-family:Manrope;src:url("file:///root/palma/tmp-assets/Manrope");font-weight:200 800}
:root{--canvas:#f5f5f7;--surface:#fff;--alt:#fafafa;--ink:#1d1d1f;--ink-soft:#515154;--muted:#6e6e73;--accent:#0071e3;--tint:#eaf2fd;--hair:#d2d2d7}
*{box-sizing:border-box;margin:0;padding:0}
body{background:#e9e9ee;font-family:Manrope,system-ui;color:var(--ink);padding:36px;display:flex;gap:34px;align-items:flex-start}
.col{display:flex;flex-direction:column;align-items:center;gap:9px}
.tag{font-size:12px;font-weight:600;color:#3a3a3c}
.phone{zoom:1.5;width:276px;height:592px;background:var(--canvas);border-radius:32px;overflow:hidden;position:relative;
box-shadow:0 2px 6px rgba(0,0,0,.06),0 18px 40px rgba(0,0,0,.10)}
.status{height:26px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;font-size:9px;color:var(--muted);position:relative;z-index:2}
.screen{position:absolute;inset:0;display:flex;flex-direction:column}
.body{padding:0 14px;flex:1;position:relative;z-index:2}
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
.bg{position:absolute;inset:0;z-index:0;pointer-events:none}
.bg>*{position:absolute}
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
  </div>
</div>"""

VARIANTS = {
    "A-aurora": '''
<div class="bg">
  <i style="width:300px;height:300px;left:-80px;top:-150px;border-radius:50%;
     background:radial-gradient(circle,rgba(178,206,244,.50),rgba(178,206,244,0) 70%)"></i>
  <i style="width:250px;height:250px;right:-100px;top:110px;border-radius:50%;
     background:radial-gradient(circle,rgba(226,229,236,.90),rgba(226,229,236,0) 70%)"></i>
</div>''',
    "C-paper": '''
<div class="bg">
  <i style="inset:0;background:linear-gradient(180deg,#ffffff 0%,rgba(255,255,255,.55) 34%,rgba(245,245,247,0) 62%)"></i>
  <i style="inset:0;opacity:.30;background-image:url(&quot;data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='3'/%3E%3C/filter%3E%3Crect width='120' height='120' filter='url(%23n)' opacity='.30'/%3E%3C/svg%3E&quot;)"></i>
</div>''',
    "D-mesh": '''
<div class="bg">
  <i style="width:340px;height:340px;left:-120px;top:-120px;border-radius:50%;
     background:radial-gradient(circle,rgba(196,214,247,.46),rgba(196,214,247,0) 68%)"></i>
  <i style="width:200px;height:200px;left:120px;top:-60px;border-radius:50%;
     background:radial-gradient(circle,rgba(226,232,243,.75),rgba(226,232,243,0) 70%)"></i>
  <i style="inset:0;background-image:radial-gradient(rgba(29,29,31,.055) .8px,transparent .8px);
     background-size:13px 13px;-webkit-mask-image:linear-gradient(180deg,#000 0%,rgba(0,0,0,.35) 30%,transparent 55%);
     mask-image:linear-gradient(180deg,#000 0%,rgba(0,0,0,.35) 30%,transparent 55%)"></i>
</div>''',
}


def page(key, bg, note):
    return (f'<div class="col"><div class="phone"><div class="screen">'
            f'<div class="status"><span>22:41</span><span>LTE · 84 %</span></div>{bg}{HERO}</div></div>'
            f'<div class="tag">{key} — {note}</div></div>')


NOTES = {"A-aurora": "мягкие пятна", "C-paper": "светлый градиент + зерно", "D-mesh": "сетка точек + свечение"}

html = ('<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>' + CSS + '</style></head><body>'
        + "".join(page(k, v, NOTES[k]) for k, v in VARIANTS.items()) + '</body></html>')
path = "/root/palma/design/hero-variants.html"
open(path, "w", encoding="utf-8").write(html)

os.makedirs(OUT, exist_ok=True)
with sync_playwright() as p:
    br = p.chromium.launch(executable_path="/usr/bin/google-chrome",
                           args=["--no-sandbox", "--disable-dev-shm-usage", "--no-zygote",
                                 "--in-process-gpu", "--enable-unsafe-swiftshader"])
    pg = br.new_page(viewport={"width": 1400, "height": 1100}, device_scale_factor=2)
    pg.goto("file://" + path)
    pg.wait_for_timeout(1200)
    boxes = pg.evaluate("""() => Array.from(document.querySelectorAll('.phone')).map(e => {
        const r = e.getBoundingClientRect();
        return {x: r.x + scrollX, y: r.y + scrollY, w: r.width, h: r.height};
    })""")
    full = "/root/palma/design/_variants.png"
    pg.screenshot(path=full, full_page=True)
    br.close()

im = Image.open(full).convert("RGB")
for i, b in enumerate(boxes):
    x, y = int(b["x"] * 2) - 4, int(b["y"] * 2) - 4
    box = (max(x, 0), max(y, 0), int((b["x"] + b["w"]) * 2) + 4, int((b["y"] + b["h"]) * 2) + 4)
    dst = os.path.join(OUT, f"bg-{list(VARIANTS)[i].split('-')[0]}.png")
    im.crop(box).save(dst)
    print("SHOT", dst, os.path.getsize(dst))
