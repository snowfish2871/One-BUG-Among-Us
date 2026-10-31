"""把 maven.neoforged.net 的请求转到官方 CDN 镜像上的本地 HTTP 代理。

为什么需要它：开发机上 maven.neoforged.net 连不通，但官方 CDN 镜像
neoforged.forgecdn.net 是通的，两边的仓库路径完全一致。插件仓库可以在
settings.gradle 里换，但 NeoGradle 插件内部还会硬编码去访问前者，
于是构建会卡在配置阶段。这里起一个本地代理，把所有指向它的请求
在 TLS 层转发到镜像上，NeoGradle 那边完全无感。

用法：
    python maven-mirror-proxy.py [--port 8899]
然后在 gradle.properties 里让 Gradle 走它（见 tracks 里各轨道的配置）。

只做转发，不缓存、不改内容。
"""
import argparse
import socket
import threading

UPSTREAM_HOST = "neoforged.forgecdn.net"
UPSTREAM_PORT = 443
# 这些主机名一律改写到镜像上
REWRITE_HOSTS = {
    "maven.neoforged.net",
    "maven.neoforged.net.",
}
BUFSIZE = 65536


def pipe(src, dst, label):
    """把一个 socket 的数据搬到另一个，直到任一端关闭。"""
    try:
        while True:
            data = src.recv(BUFSIZE)
            if not data:
                break
            dst.sendall(data)
    except OSError:
        pass
    finally:
        for s in (src, dst):
            try:
                s.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass


def handle(client, addr, stats):
    try:
        # 读客户端的第一行，拿到 CONNECT 目标
        client.settimeout(30)
        head = b""
        while b"\r\n\r\n" not in head and len(head) < 65536:
            chunk = client.recv(4096)
            if not chunk:
                return
            head += chunk
            if b"\r\n" in head and not head.split(b"\r\n", 1)[0].startswith(b"CONNECT"):
                # 不是 CONNECT，说明客户端在发明文请求，直接拒掉
                client.sendall(b"HTTP/1.1 405 Method Not Allowed\r\n\r\n")
                return

        first_line = head.split(b"\r\n", 1)[0].decode("latin-1")
        parts = first_line.split()
        if len(parts) < 2 or parts[0].upper() != "CONNECT":
            client.sendall(b"HTTP/1.1 400 Bad Request\r\n\r\n")
            return

        hostport = parts[1]
        host = hostport.rsplit(":", 1)[0].strip("[]").lower()
        if host in REWRITE_HOSTS:
            target_host = UPSTREAM_HOST
            stats["rewritten"] += 1
        else:
            target_host = host
            stats["passthrough"] += 1

        upstream = socket.create_connection((target_host, UPSTREAM_PORT), timeout=30)
        client.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")

        client.settimeout(None)
        upstream.settimeout(None)
        t1 = threading.Thread(target=pipe, args=(client, upstream, "c->u"), daemon=True)
        t2 = threading.Thread(target=pipe, args=(upstream, client, "u->c"), daemon=True)
        t1.start()
        t2.start()
        t1.join()
        t2.join()
    except Exception:
        pass
    finally:
        try:
            client.close()
        except OSError:
            pass


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8899)
    args = ap.parse_args()

    stats = {"rewritten": 0, "passthrough": 0}
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("127.0.0.1", args.port))
    srv.listen(128)
    print(f"maven 镜像代理已启动: 127.0.0.1:{args.port} -> {UPSTREAM_HOST}")
    print(f"改写的主机: {', '.join(sorted(REWRITE_HOSTS))}")
    try:
        while True:
            client, addr = srv.accept()
            threading.Thread(target=handle, args=(client, addr, stats), daemon=True).start()
    except KeyboardInterrupt:
        print(f"\n退出。改写 {stats['rewritten']} 次，直通 {stats['passthrough']} 次")
    finally:
        srv.close()


if __name__ == "__main__":
    main()
