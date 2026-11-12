#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Точечный OCR по кропам."""
import subprocess, io
from PIL import Image, ImageOps

def ocr_crop(path, box, scale=4, psm=6, thresh=140):
    img = Image.open(path).convert("L").crop(box)
    w, h = img.size
    big = img.resize((w*scale, h*scale), Image.LANCZOS)
    big = ImageOps.autocontrast(big).point(lambda p: 255 if p > thresh else 0)
    buf = io.BytesIO(); big.save(buf, format="PNG")
    p = subprocess.run(["tesseract", "stdin", "stdout", "-l", "rus+eng",
                        "--psm", str(psm)], input=buf.getvalue(), capture_output=True)
    return p.stdout.decode("utf-8", "replace").strip()

files = {
    "k1.png": (1080, 2400), "k2.png": (2400, 1080), "k3.png": (1080, 2400),
    "n1.png": (1080, 2400), "n2.png": (2400, 1080), "n3.png": (1080, 2400),
    "p2.png": (2400, 1080),
}
DIR = "/root/palma/repro/"

print("== 1) Верхняя полоса (статус-бар + заголовок), y0-130 ==")
for f in files:
    w, h = files[f]
    t = ocr_crop(DIR+f, (0, 0, w, min(130, h)), psm=6)
    print(f"{f}: '{t}'")

print("\n== 2) k1: строки приложений по отдельности ==")
for y0 in (130, 250, 380, 510, 640, 770, 900):
    t = ocr_crop(DIR+"k1.png", (0, y0, 1080, y0+120), psm=7)
    print(f"  y{y0}: '{t}'")

print("\n== 3) p2: левая колонка строк ==")
for y0 in (130, 250, 380, 510, 640, 770, 890):
    t = ocr_crop(DIR+"p2.png", (0, y0, 1100, y0+120), psm=7)
    print(f"  y{y0}: '{t}'")

print("\n== 4) p2: правая панель (x1100-2400) ==")
t = ocr_crop(DIR+"p2.png", (1100, 40, 2400, 1080), psm=11)
print(f"  правая панель: '{t}'")
t = ocr_crop(DIR+"p2.png", (1400, 380, 1850, 640), psm=6)
print(f"  светлый блок y380-640: '{t}'")
t = ocr_crop(DIR+"p2.png", (1850, 600, 2400, 1080), psm=6)
print(f"  правый нижний: '{t}'")

print("\n== 5) k3/n3: центр экрана (символы © >) ==")
for f in ("k3.png", "n3.png"):
    t = ocr_crop(DIR+f, (400, 1000, 700, 1180), psm=7, thresh=110)
    print(f"  {f} центр: '{t}'")
    t = ocr_crop(DIR+f, (0, 200, 1080, 1000), psm=11, thresh=110)
    print(f"  {f} верх-контент: '{t}'")

print("\n== 6) k2/n2 (ландшафт, чёрные): поиск любого текста по всей площади ==")
for f in ("k2.png", "n2.png"):
    t = ocr_crop(DIR+f, (0, 90, 2400, 1080), psm=11, thresh=90)
    print(f"  {f} под статус-баром: '{t}'")

print("\n== 7) Поиск слов Главная/Знания/Linux/Matrux во всех OCR ==")
for f in files:
    for psm in (3, 11):
        img = Image.open(DIR+f).convert("L")
        w, h = img.size
        big = img.resize((w*3, h*3), Image.LANCZOS)
        big = ImageOps.autocontrast(big).point(lambda p: 255 if p > 150 else 0)
        buf = io.BytesIO(); big.save(buf, format="PNG")
        p = subprocess.run(["tesseract", "stdin", "stdout", "-l", "rus+eng",
                            "--psm", str(psm)], input=buf.getvalue(), capture_output=True)
        txt = p.stdout.decode("utf-8", "replace")
        hits = [ln for ln in txt.splitlines() if any(k in ln.lower() for k in ("главн", "знани", "linux", "matrux", "matr"))]
        if hits:
            print(f"  {f} psm{psm}: {hits}")
print("  (готово)")
