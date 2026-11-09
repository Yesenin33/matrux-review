import subprocess, sys, time, os
sys.path.insert(0, "/root/palma/repro")
import importlib.util
spec = importlib.util.spec_from_file_location("vs", "/root/palma/repro/vnc_shot.py")

# reuse capture by running the script
def shot(path):
    subprocess.run(["python3", "/root/palma/repro/vnc_shot.py", path, "5902"],
                   check=True, capture_output=True)

def rows(path):
    from PIL import Image
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    out = []
    for y in range(h):
        s = 0
        for x in range(0, w, 8):
            r, g, b = px[x, y]
            s += (r + g + b) / 3
        out.append(s / (w // 8))
    return out

def panel_band(path):
    r = rows(path)
    h = len(r)
    bright = [y for y, v in enumerate(r) if v > 120]
    if not bright:
        return None
    # contiguous band
    return (bright[0], bright[-1], len(bright))

if __name__ == "__main__":
    for p in sys.argv[1:]:
        pos = "p=%s;x=0;y=0" % p
        subprocess.run(["chroot", "/root/palma/image-build/debian13-rootfs",
                        "/usr/bin/qemu-aarch64", "/bin/sh", "-c",
                        'export TMPDIR=/tmp DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS="unix:path=/tmp/dbus-4EEuszAmc2,guid=1c4a3dc6838334fd201eae9f6af1904a"; '
                        'xfconf-query -c xfce4-panel -p /panels/panel-1/position -s "%s"' % pos],
                       check=False, capture_output=True)
        time.sleep(3)
        f = "/root/palma/repro/probe-%s.png" % p
        shot(f)
        print(p, pos, panel_band(f), flush=True)
