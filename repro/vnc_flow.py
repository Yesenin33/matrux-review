import socket
import struct
import sys
import time

PORT = int(sys.argv[1])
SECONDS = float(sys.argv[2])
FORCE_BPP = int(sys.argv[3]) if len(sys.argv) > 3 else 0  # 0 = формат сервера


def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise RuntimeError("closed")
        buf += chunk
    return buf


def main():
    sock = socket.create_connection(("127.0.0.1", PORT), timeout=15)
    recv_exact(sock, 12)
    sock.sendall(b"RFB 003.008\n")
    count = recv_exact(sock, 1)[0]
    recv_exact(sock, count)
    sock.sendall(bytes([1]))
    recv_exact(sock, 4)
    sock.sendall(b"\x01")
    header = recv_exact(sock, 24)
    width, height = struct.unpack(">HH", header[:4])
    pf = header[4:20]
    srv_bpp, srv_depth, srv_true = pf[0], pf[1], pf[3]
    maxr, maxg, maxb = struct.unpack(">HHH", pf[4:10])
    sr, sg, sb = pf[10], pf[11], pf[12]
    print("сервер: %dx%d формат bpp=%d depth=%d true=%d маски %d/%d/%d сдвиги %d/%d/%d"
          % (width, height, srv_bpp, srv_depth, srv_true, maxr, maxg, maxb, sr, sg, sb))
    recv_exact(sock, struct.unpack(">I", header[20:24])[0])

    bytepp = srv_bpp // 8
    if FORCE_BPP:
        bytepp = FORCE_BPP // 8
        sock.sendall(struct.pack(">BBBB", 0, 0, 0, 0))
        if FORCE_BPP == 16:
            sock.sendall(struct.pack(">BBBB", 16, 16, 0, 1) + struct.pack(">HHH", 31, 63, 31)
                         + struct.pack(">HHH", 11, 5, 0) + b"\x00\x00\x00")
        else:
            sock.sendall(struct.pack(">BBBB", 32, 24, 0, 1) + struct.pack(">HHH", 255, 255, 255)
                         + struct.pack(">HHH", 16, 8, 0) + b"\x00\x00\x00")

    sock.sendall(struct.pack(">BBH", 2, 0, 1) + struct.pack(">i", 0))
    sock.sendall(struct.pack(">BBHHHH", 3, 0, 0, 0, width, height))

    end = time.time() + SECONDS
    updates = rects = data_bytes = full_rects = 0
    while time.time() < end:
        sock.settimeout(max(1.0, end - time.time()))
        try:
            msg_type = recv_exact(sock, 1)[0]
        except (socket.timeout, RuntimeError):
            break
        if msg_type == 0:
            recv_exact(sock, 1)
            n = struct.unpack(">H", recv_exact(sock, 2))[0]
            updates += 1
            rects += n
            for _ in range(n):
                x, y, w, h, enc = struct.unpack(">HHHHi", recv_exact(sock, 12))
                if enc == 0:
                    data_bytes += w * h * bytepp
                    if w == width:
                        full_rects += 1
                    recv_exact(sock, w * h * bytepp)
                elif enc == 1:
                    recv_exact(sock, 4)
                    data_bytes += 4
                elif enc in (-223, -224):
                    pass
                elif enc == 4:
                    recv_exact(sock, 4)
                    recv_exact(sock, w * h * bytepp)
                    data_bytes += w * h * bytepp
                else:
                    raise RuntimeError("кодирование %d — счётчик его не знает" % enc)
        elif msg_type == 3:
            recv_exact(sock, 3)
            recv_exact(sock, struct.unpack(">I", recv_exact(sock, 4))[0])
        elif msg_type == 1:
            recv_exact(sock, 6)
        else:
            raise RuntimeError("сообщение %d" % msg_type)

    mb = data_bytes / 1048576.0
    print("кадров %d, прямоугольников %d (во всю ширину %d), данных %.2f МБ, %.2f МБ/с, %.2f МБ на кадр"
          % (updates, rects, full_rects, mb, mb / max(0.001, SECONDS), mb / max(1, updates)))


main()
