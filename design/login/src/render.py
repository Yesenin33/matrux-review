import os, re, subprocess
from PIL import Image

ROOT = "/root/palma"
OUT = f"{ROOT}/design/login"
SRC = f"{OUT}/src"
FONT = f"{ROOT}/app/src/main/res/font"


BASE = f"""
:root {{
  --canvas: #f5f5f7;
  --surface: #ffffff;
  --ink: #1d1d1f;
  --muted: #6e6e73;
  --shadow: 0 1px 2px rgba(0,0,0,.04), 0 8px 24px rgba(0,0,0,.06);
  --sans: "Inter", system-ui, sans-serif;
  --mono: "Mono", ui-monospace, monospace;
}}
@font-face {{ font-family: "Inter"; src: url("file://{FONT}/inter.ttf"); font-weight: 100 900; }}
@font-face {{ font-family: "Mono"; src: url("file://{FONT}/mono.ttf"); font-weight: 500; }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
html, body {{ width: 390px; height: 844px; overflow-x: clip; background: var(--canvas); }}
body {{ font-family: var(--sans); color: var(--ink); -webkit-font-smoothing: antialiased; }}
.screen {{ position: relative; width: 390px; height: 844px; padding: 0 20px; display: flex; flex-direction: column; }}
.display {{ font-size: 32px; line-height: 36px; font-weight: 600; letter-spacing: -0.02em; }}
.title {{ font-size: 20px; line-height: 26px; font-weight: 600; letter-spacing: -0.01em; }}
.body {{ font-size: 15px; line-height: 22px; font-weight: 400; }}
.code {{ font-family: var(--mono); font-size: 13px; line-height: 20px; font-weight: 500; letter-spacing: -0.01em; }}
.muted {{ color: var(--muted); }}
.push {{ flex: 1; }}
.cta {{ height: 56px; border-radius: 999px; background: var(--surface); color: var(--ink); border: 0; box-shadow: var(--shadow);
  font: 600 15px/22px var(--sans); display: flex; align-items: center; justify-content: center; width: 100%; }}
.head {{ padding-top: 48px; }}
.foot {{ padding-bottom: 48px; }}
"""

VARIANTS = {}

VARIANTS["v1"] = (
    """
.getty { display: flex; flex-direction: column; }
.getty .code { margin-top: 8px; }
.getty .cta { margin-top: 24px; }
""",
    """
<div class="screen">
  <div class="push"></div>
  <div class="getty">
    <h1 class="display">Matrux</h1>
    <p class="code muted">matrux login:</p>
    <button class="cta">Продолжить с VK ID</button>
  </div>
  <div class="push"></div>
</div>
""",
)

VARIANTS["v2"] = (
    """
.pair { display: flex; flex-direction: column; }
.pair .body { margin-top: 8px; }
""",
    """
<div class="screen">
  <div class="head pair">
    <h1 class="display">Matrux</h1>
    <p class="display muted">Alpine + XFCE</p>
    <p class="body muted">Запущена</p>
  </div>
  <div class="push"></div>
  <div class="foot"><button class="cta">Продолжить с VK ID</button></div>
</div>
""",
)

VARIANTS["v3"] = (
    """
.log { display: flex; flex-direction: column; }
.log .code { white-space: pre; }
.end { display: flex; flex-direction: column; gap: 24px; }
""",
    """
<div class="screen">
  <div class="log head">
    <p class="code muted">export XDG_RUNTIME_DIR=/tmp/runtime-root</p>
    <p class="code muted">export DISPLAY=:0</p>
    <p class="code">exec dbus-run-session -- xfce4-session</p>
  </div>
  <div class="push"></div>
  <div class="end foot">
    <h1 class="display">Matrux</h1>
    <button class="cta">Продолжить с VK ID</button>
  </div>
</div>
""",
)

VARIANTS["v4"] = (
    """
.names { display: flex; flex-direction: column; margin-bottom: 24px; }
""",
    """
<div class="screen">
  <div class="head"><p class="title">Matrux</p></div>
  <div class="push"></div>
  <div class="foot">
    <div class="names">
      <p class="display">Alpine + XFCE</p>
      <p class="display">Debian 12</p>
      <p class="display">Ubuntu + XFCE</p>
      <p class="display">Arch + XFCE</p>
    </div>
    <button class="cta">Продолжить с VK ID</button>
  </div>
</div>
""",
)


def page(css, body):
    return f'<!doctype html><html lang="ru"><head><meta charset="utf-8"><style>{BASE}{css}</style></head><body>{body}</body></html>'


def shoot(html_path, png_path):
    subprocess.run([
        "google-chrome", "--headless=new", "--disable-gpu", "--no-sandbox",
        "--hide-scrollbars", f"--user-data-dir=/tmp/matrux-chrome-{os.path.basename(png_path)}",
        "--disable-lcd-text", "--force-device-scale-factor=3", "--window-size=390,844",
        f"--screenshot={png_path}", "file://" + html_path,
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)


frames = []
for name, (css, body) in VARIANTS.items():
    h = f"{SRC}/{name}.html"
    open(h, "w", encoding="utf-8").write(page(css, body))
    p = f"{OUT}/{name}.png"
    shoot(h, p)
    im = Image.open(p).convert("RGB")
    assert im.size == (1170, 2532), im.size
    im.save(p, optimize=True)
    frames.append(im)
    print(name, im.size)

scale = 3
fw, fh = 1170 // scale, 2532 // scale
gap = 48
sheet = Image.new("RGB", (gap + len(frames) * (fw + gap), fh + 2 * gap), (255, 255, 255))
mask = Image.new("L", (fw, fh), 0)
from PIL import ImageDraw
ImageDraw.Draw(mask).rounded_rectangle((0, 0, fw - 1, fh - 1), radius=40, fill=255)
edge = Image.new("RGB", (fw + 2, fh + 2), (210, 210, 215))
emask = Image.new("L", (fw + 2, fh + 2), 0)
ImageDraw.Draw(emask).rounded_rectangle((0, 0, fw + 1, fh + 1), radius=41, fill=255)
for i, im in enumerate(frames):
    x = gap + i * (fw + gap)
    sheet.paste(edge, (x - 1, gap - 1), emask)
    sheet.paste(im.resize((fw, fh), Image.LANCZOS), (x, gap), mask)
sheet.save(f"{OUT}/all.png", optimize=True)
print("all", sheet.size)
