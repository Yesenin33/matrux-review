#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Анализ скриншотов Matrux: OCR (tesseract rus+eng) + пиксельные замеры."""
import subprocess, os, io, sys
from PIL import Image, ImageOps, ImageFilter

FILES = ["k1.png", "k2.png", "k3.png", "n1.png", "n2.png", "n3.png", "p2.png"]
DIR = "/root/palma/repro/"

def ocr(img, psm=3, scale=3):
    """Upscale and OCR with tesseract rus+eng."""
    w, h = img.size
    big = img.convert("L").resize((w*scale, h*scale), Image.LANCZOS)
    big = ImageOps.autocontrast(big)
    # binarize softly
    big = big.point(lambda p: 255 if p > 150 else 0)
    buf = io.BytesIO()
    big.save(buf, format="PNG")
    p = subprocess.run(["tesseract", "stdin", "stdout", "-l", "rus+eng",
                        "--psm", str(psm)], input=buf.getvalue(),
                       capture_output=True)
    return p.stdout.decode("utf-8", "replace")

def ocr_tsv(img, scale=3, psm=11):
    """OCR with word boxes (for locating elements)."""
    w, h = img.size
    big = img.convert("L").resize((w*scale, h*scale), Image.LANCZOS)
    big = ImageOps.autocontrast(big).point(lambda p: 255 if p > 150 else 0)
    buf = io.BytesIO(); big.save(buf, format="PNG")
    p = subprocess.run(["tesseract", "stdin", "stdout", "-l", "rus+eng",
                        "--psm", str(psm), "tsv"], input=buf.getvalue(),
                       capture_output=True)
    rows = []
    for line in p.stdout.decode("utf-8", "replace").splitlines()[1:]:
        f = line.split("\t")
        if len(f) < 12: continue
        try:
            conf = float(f[10]); txt = f[11].strip()
        except ValueError:
            continue
        if conf > 30 and txt:
            x, y, ww, hh = int(f[6])//scale, int(f[7])//scale, int(f[8])//scale, int(f[9])//scale
            rows.append((y, x, txt, round(conf)))
    rows.sort()
    return rows

def dominant_colors(img, n=8):
    q = img.quantize(colors=n, method=Image.MEDIANCUT).convert("RGB")
    from collections import Counter
    cnt = Counter(q.getdata())
    total = img.size[0]*img.size[1]
    out = []
    for col, c in cnt.most_common(n):
        out.append((col, round(100*c/total, 1)))
    return out

def band_stats(img, y0, y1):
    """Average color + dominant colors of horizontal band."""
    crop = img.crop((0, y0, img.size[0], y1))
    avg = tuple(round(v) for v in ImageStat_mean(crop))
    return avg, dominant_colors(crop, 4)

def ImageStat_mean(im):
    # fast mean via resize
    sm = im.resize((1,1), Image.LANCZOS)
    return sm.getpixel((0,0))[:3]

def colored_regions(img, bg_tol=40, min_area=800):
    """Find rectangles of saturated color (cards/plates). Returns list of (x,y,w,h,avgcolor,area)."""
    from collections import deque
    w, h = img.size
    px = img.convert("RGB")
    visited = [[False]*w for _ in range(h)]
    regions = []
    # background = most common color of full image
    q = px.quantize(colors=6, method=Image.MEDIANCUT).convert("RGB")
    from collections import Counter
    bg, _ = Counter(q.getdata()).most_common(1)[0]
    def far(c):
        return abs(c[0]-bg[0]) + abs(c[1]-bg[1]) + abs(c[2]-bg[2]) > 60
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            if visited[y][x]: continue
            c = px.getpixel((x, y))
            if not far(c): continue
            # BFS
            qq = deque([(x, y)]); visited[y][x] = True
            pts = []
            while qq:
                cx, cy = qq.popleft()
                pts.append((cx, cy))
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    nx, ny = cx+dx, cy+dy
                    if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                        cc = px.getpixel((nx, ny))
                        if abs(cc[0]-c[0])+abs(cc[1]-c[1])+abs(cc[2]-c[2]) < 60:
                            visited[ny][nx] = True
                            qq.append((nx, ny))
            if len(pts) >= min_area:
                xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
                mx, Mx, my, My = min(xs), max(xs), min(ys), max(ys)
                region = px.crop((mx, my, Mx, My))
                avg = tuple(round(v) for v in ImageStat_mean(region))
                regions.append((mx, my, Mx-mx, My-my, avg, len(pts)))
    regions.sort(key=lambda r: (r[1], r[0]))
    return regions

def analyze(fname):
    path = DIR + fname
    img = Image.open(path).convert("RGB")
    w, h = img.size
    print(f"=== {fname}: {w}x{h} ===")
    # full OCR
    txt3 = ocr(img, psm=3)
    txt11 = ocr(img, psm=11)
    alltxt = txt3 + "\n" + txt11
    # print cleaned OCR text (unique lines)
    seen = set(); lines = []
    for ln in (txt3 + "\n" + txt11).splitlines():
        ln = ln.strip()
        if ln and ln not in seen:
            seen.add(ln); lines.append(ln)
    print("-- OCR текст (psm3+psm11) --")
    for ln in lines[:60]:
        print("   |", ln)
    # word boxes, grouped into lines
    boxes = ocr_tsv(img)
    print("-- OCR слова с координатами (y,x: text) --")
    cur_y = -10; linebuf = []
    for y, x, t, conf in boxes:
        if abs(y - cur_y) > 18:
            if linebuf:
                print("   y%03d: %s" % (cur_y, " ".join(t for _,_,t,_ in sorted(linebuf))))
            linebuf = [(x, y, t, conf)]; cur_y = y
        else:
            linebuf.append((x, y, t, conf))
    if linebuf:
        print("   y%03d: %s" % (cur_y, " ".join(t for _,_,t,_ in sorted(linebuf))))
    # color profile bands
    print("-- Цветовые замеры --")
    bands = [("статус-бар", 0, min(90, h//12)), ("шапка", int(h*0.06), int(h*0.16)),
             ("контент верх", int(h*0.16), int(h*0.35)), ("середина", int(h*0.4), int(h*0.6)),
             ("низ контента", int(h*0.75), int(h*0.9)), ("нижняя навигация", max(0,h-220), h)]
    for name, y0, y1 in bands:
        if y1 <= y0: continue
        avg, dom = band_stats(img, y0, y1)
        print(f"   {name} y{y0}-{y1}: avg={avg} dom={dom}")
    # full dominant
    print("   доминанты всего экрана:", dominant_colors(img, 6))
    # colored regions
    regs = colored_regions(img)
    print("-- Цветные регионы (карточки/плашки), площадь>=800px --")
    if not regs:
        print("   (не найдено)")
    for r in regs[:25]:
        x, y, rw, rh, avg, area = r
        print(f"   x{x} y{y} {rw}x{rh} avg={avg} px={area}")
    print()

for f in FILES:
    try:
        analyze(f)
    except Exception as e:
        print(f"=== {f}: ОШИБКА {e} ===")
