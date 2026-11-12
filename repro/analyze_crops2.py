#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import subprocess, io
from PIL import Image, ImageOps
from collections import Counter

def ocr_crop(path, box, scale=4, psm=6, thresh=140):
    img = Image.open(path).convert("L").crop(box)
    w, h = img.size
    big = img.resize((w*scale, h*scale), Image.LANCZOS)
    big = ImageOps.autocontrast(big).point(lambda p: 255 if p > thresh else 0)
    buf = io.BytesIO(); big.save(buf, format="PNG")
    p = subprocess.run(["tesseract", "stdin", "stdout", "-l", "rus+eng",
                        "--psm", str(psm)], input=buf.getvalue(), capture_output=True)
    return p.stdout.decode("utf-8", "replace").strip()

DIR = "/root/palma/repro/"

print("== k1: строки, кроп x470-1080 (без иконок) ==")
for y0, y1 in ((138, 240), (262, 362), (390, 500), (520, 620), (648, 748), (778, 880), (908, 1008)):
    t = ocr_crop(DIR+"k1.png", (470, y0, 1080, y1), psm=7)
    print(f"  y{y0}-{y1}: '{t}'")

print("\n== k1: низ экрана y1700-2400 (что за регион x455 y1745 и иконка внизу) ==")
t = ocr_crop(DIR+"k1.png", (0, 1680, 1080, 2400), psm=11)
print(f"  низ: '{t}'")
t = ocr_crop(DIR+"k1.png", (150, 1690, 1050, 1830), psm=6)
print(f"  y1690-1830 x150-1050: '{t}'")

print("\n== k1: фон между строками (цвет карточек) и иконки ==")
img = Image.open(DIR+"k1.png").convert("RGB")
for (y) in (150, 260, 390):
    col = img.getpixel((800, y))
    print(f"  строка {y} (x800): {col}")
col = img.getpixel((300, 172))  # внутри иконки первой строки?
print(f"  иконка 1-й строки (300,172): {col}")

print("\n== p2: правая панель — вертикальные срезы ==")
for y in (100, 300, 500, 700, 900, 1050):
    row = [img.getpixel((x, y)) for x in range(1100, 2400, 130)]
    print(f"  y{y}: {row}")

print("\n== Насыщенные (яркие) пиксели по файлам ==")
for f in ("k1.png","k2.png","k3.png","n1.png","n2.png","n3.png","p2.png"):
    im = Image.open(DIR+f).convert("RGB")
    w, h = im.size
    px = im.load()
    n_sat = 0; n_light = 0; total = 0
    sat_colors = Counter()
    for y in range(0, h, 4):
        for x in range(0, w, 4):
            r, g, b = px[x, y]
            mx, mn = max(r,g,b), min(r,g,b)
            total += 1
            if mx - mn > 70 and mx > 100:
                n_sat += 1
                if (n_sat - 1) < 60:  # помнить первые цвета
                    sat_colors[(r//32*32, g//32*32, b//32*32)] += 1
            if mx > 200:
                n_light += 1
    print(f"  {f}: насыщенных {round(100*n_sat/total,2)}% ({n_sat}), светлых {round(100*n_light/total,2)}%")
    if sat_colors:
        print(f"      первые насыщенные оттенки (квантованные): {sat_colors.most_common(5)}")

print("\n== p2: кроп правой части с поро гом ниже (иконки на панели) ==")
t = ocr_crop(DIR+"p2.png", (1150, 60, 2400, 1080), psm=11, thresh=110)
print(f"  p2 правая половина psm11 thresh110: '{t}'")
