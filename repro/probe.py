import socket, struct, sys, time
PORT=int(sys.argv[1])
s=socket.create_connection(("127.0.0.1",PORT),timeout=10)
def rx(n):
    b=b""
    while len(b)<n:
        c=s.recv(n-len(b))
        if not c: raise RuntimeError("closed")
        b+=c
    return b
print("ver",rx(12))
s.sendall(b"RFB 003.008\n")
c=rx(1)[0]; print("sec",list(rx(c)))
s.sendall(bytes([1])); print("secres",rx(4))
s.sendall(b"\x01")
h=rx(24); w,ht=struct.unpack(">HH",h[:4]); nl=struct.unpack(">I",h[20:24])[0]
print("fb",w,ht,"pf",list(h[4:20]))
name=rx(nl); print("name",name)
s.sendall(struct.pack(">BBH",2,0,1)+struct.pack(">i",0))
s.sendall(struct.pack(">BBHHHH",3,0,0,0,w,ht))
for i in range(6):
    t=rx(1)[0]; print("--- msg type",t)
    if t==0:
        px=rx(1)[0]; n=struct.unpack(">H",rx(2))[0]
        print("   padding",px,"rects",n)
        for j in range(n):
            r=rx(12); x,y,rw,rh,enc=struct.unpack(">HHHHi",r)
            print("   rect",x,y,rw,rh,"enc",enc)
            if enc==0:
                d=rx(rw*rh*4); print("   raw bytes",len(d),"first16",d[:16].hex())
            elif enc==1:
                print("   copyrect src",struct.unpack(">HH",rx(4)))
            else:
                print("   hex around:",r.hex()); sys.exit(0)
    elif t==3:
        rx(3); n=struct.unpack(">I",rx(4))[0]; print("   cuttext",rx(n))
    else:
        print("   other",t); sys.exit(0)
