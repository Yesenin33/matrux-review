import socket
import struct
import sys
import time
import zlib

HOST = "127.0.0.1"
LOWER = "абцдефгхийклмнопярстужвьызшэщчъ"
CYRILLIC_A = 0x06C1
CYRILLIC_CAPITAL_A = 0x06E1


def keysym(letter):
    if letter == "ё":
        return 0x06A3
    if letter == "Ё":
        return 0x06B3
    if letter in LOWER:
        return CYRILLIC_A + LOWER.index(letter)
    if letter.upper() in LOWER.upper() and letter != letter.lower():
        return CYRILLIC_CAPITAL_A + LOWER.upper().index(letter)
    if 0x20 <= ord(letter) <= 0x7E:
        return ord(letter)
    if letter == "\n":
        return 0xFF0D
    return None


def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise RuntimeError("connection closed")
        buf += chunk
    return buf


def png(width, height, pixels):
    raw = b"".join(b"\x00" + pixels[y * width * 4:(y + 1) * width * 4] for y in range(height))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b"")


def pointer(sock, x, y, buttons):
    sock.sendall(struct.pack(">BBHH", 5, buttons, x, y))


def key(sock, code, down):
    sock.sendall(struct.pack(">BBHI", 4, 1 if down else 0, 0, code))


def send(sock, code):
    key(sock, code, True)
    time.sleep(0.03)
    key(sock, code, False)
    time.sleep(0.03)


def main():
    out = sys.argv[1]
    port = int(sys.argv[2])
    click = None
    text = ""
    extra = []
    combo = []
    rest = sys.argv[3:]
    i = 0
    while i < len(rest):
        if rest[i] == "--click":
            click = tuple(int(v) for v in rest[i + 1].split(","))
            i += 2
        elif rest[i] == "--type":
            text = rest[i + 1]
            i += 2
        elif rest[i] == "--keysym":
            extra.append(int(rest[i + 1], 0))
            i += 2
        elif rest[i] == "--combo":
            combo = [int(v, 0) for v in rest[i + 1].split(",")]
            i += 2
        else:
            i += 1

    sock = socket.create_connection((HOST, port), timeout=10)
    version = recv_exact(sock, 12)
    sock.sendall(b"RFB 003.008\n")
    count = recv_exact(sock, 1)[0]
    recv_exact(sock, count)
    sock.sendall(bytes([1]))
    if struct.unpack(">I", recv_exact(sock, 4))[0] != 0:
        raise RuntimeError("auth failed")
    sock.sendall(b"\x01")
    header = recv_exact(sock, 24)
    width, height = struct.unpack(">HH", header[:4])
    name_len = struct.unpack(">I", header[20:24])[0]
    recv_exact(sock, name_len)
    print("guest", width, "x", height, version.decode().strip())
    sock.sendall(struct.pack(">BBH", 2, 0, 1) + struct.pack(">i", 0))

    if click:
        pointer(sock, click[0], click[1], 1)
        time.sleep(0.1)
        pointer(sock, click[0], click[1], 0)
        time.sleep(0.4)
        print("клик", click)

    if combo:
        for code in combo:
            key(sock, code, True)
        time.sleep(0.05)
        for code in reversed(combo):
            key(sock, code, False)
        time.sleep(0.5)
        print("сочетание", [hex(c) for c in combo])
    for letter in text:
        code = keysym(letter)
        if code is None:
            print("пропуск", repr(letter))
            continue
        send(sock, code)
        print("набрано", repr(letter), hex(code))
    for code in extra:
        send(sock, code)
        print("нажато", hex(code))

    time.sleep(0.8)
    sock.sendall(struct.pack(">BBHHHH", 3, 0, 0, 0, width, height))
    pixels = bytearray(width * height * 4)
    while True:
        msg_type = recv_exact(sock, 1)[0]
        if msg_type == 0:
            recv_exact(sock, 1)
            rects = struct.unpack(">H", recv_exact(sock, 2))[0]
            for _ in range(rects):
                x, y, w, h, enc = struct.unpack(">HHHHi", recv_exact(sock, 12))
                if enc != 0:
                    raise RuntimeError("encoding %d not raw" % enc)
                data = recv_exact(sock, w * h * 4)
                for row in range(h):
                    start = ((y + row) * width + x) * 4
                    pixels[start:start + w * 4] = data[row * w * 4:(row + 1) * w * 4]
            break
        if msg_type == 3:
            recv_exact(sock, 4)
            continue
        raise RuntimeError("unknown server message %d" % msg_type)
    with open(out, "wb") as handle:
        handle.write(png(width, height, bytes(pixels)))
    print("сохранено", out)


main()
