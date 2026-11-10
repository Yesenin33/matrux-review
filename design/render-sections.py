import os, re, subprocess
from PIL import Image

SRC = "/root/palma/design/mockup-sections.html"
OUT = "/root/palma/design/shots"
TMP = "/root/palma/design/_src"
os.makedirs(OUT, exist_ok=True)
os.makedirs(TMP, exist_ok=True)

html = open(SRC, encoding="utf-8").read()
head, rest = html.split("<body>", 1)
body = rest.rsplit("</body>", 1)[0]
parts = re.split(r'(?=<div class="wrap")', body)
wraps = [p for p in parts if p.strip().startswith('<div class="wrap"')]

SCALE = 2
for w in wraps:
    name = re.search(r'data-name="([^"]+)"', w).group(1)
    page = head + "<body>" + w + "</body></html>"
    src = os.path.join(TMP, name + ".html")
    open(src, "w", encoding="utf-8").write(page)
    png = os.path.join(TMP, name + ".png")
    subprocess.run([
        "google-chrome", "--headless=new", "--disable-gpu", "--no-sandbox",
        "--hide-scrollbars", "--default-background-color=ffffffff",
        f"--force-device-scale-factor={SCALE}",
        "--window-size=520,1000",
        f"--screenshot={png}", "file://" + src,
    ], check=True, capture_output=True, timeout=120)
    im = Image.open(png)
    x, y = 40 * SCALE, 40 * SCALE
    box = (x - 4, y - 4, x + int(414 * SCALE) + 4, y + int(888 * SCALE) + 4)
    cut = im.convert("RGB").crop(box)
    dst = os.path.join(OUT, name + ".png")
    cut.save(dst, optimize=True)
    print(name, cut.size, os.path.getsize(dst))
