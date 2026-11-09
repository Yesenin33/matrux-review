import socket
import struct
import sys
import zlib

HOST, PORT = "127.0.0.1", int(sys.argv[2]) if len(sys.argv) > 2 else 5901


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


def main():
    sock = socket.create_connection((HOST, PORT), timeout=10)
    version = recv_exact(sock, 12)
    print("server version", version.decode().strip())
    sock.sendall(b"RFB 003.008\n")
    count = recv_exact(sock, 1)[0]
    types = recv_exact(sock, count)
    print("security types", list(types))
    sock.sendall(bytes([1]))
    result = struct.unpack(">I", recv_exact(sock, 4))[0]
    print("security result", result)
    sock.sendall(b"\x01")
    header = recv_exact(sock, 24)
    width, height = struct.unpack(">HH", header[:4])
    name_len = struct.unpack(">I", header[20:24])[0]
    name = recv_exact(sock, name_len).decode("utf-8", "replace")
    print("desktop", width, "x", height, repr(name))
    sock.sendall(struct.pack(">BBH", 2, 0, 1) + struct.pack(">i", 0))
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
        if msg_type == 1:
            raise RuntimeError("server sent colour map")
        if msg_type == 2:
            raise RuntimeError("server has bell message")
        if msg_type == 3:
            print("server cut text:", recv_exact(sock, 4))
            continue
        raise RuntimeError("unknown server message %d" % msg_type)
    nonblack = sum(1 for i in range(0, len(pixels), 4) if any(pixels[i:i + 3]))
    print("non-black pixels", nonblack, "of", width * height)
    with open(sys.argv[1], "wb") as out:
        out.write(png(width, height, bytes(pixels)))
    print("saved", sys.argv[1])


main()
