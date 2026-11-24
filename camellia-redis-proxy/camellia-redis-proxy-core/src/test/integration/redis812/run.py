#!/usr/bin/env python3
"""Real Redis/proxy integration checks; creates and cleans up its own processes.

Uses only Python's standard library. Never connects to an existing Redis server.
"""
import argparse
import concurrent.futures
import json
import pathlib
import random
import socket
import subprocess
import tempfile
import time
import traceback


class RedisError(Exception):
    pass


class Client:
    def __init__(self, port, timeout=4, executed=None):
        self.socket = socket.create_connection(("127.0.0.1", port), timeout)
        self.stream = self.socket.makefile("rb")
        self.executed = executed

    def close(self):
        self.stream.close()
        self.socket.close()

    def cmd(self, *args):
        args = [x if isinstance(x, bytes) else str(x).encode() for x in args]
        self.socket.sendall(b"*%d\r\n" % len(args) + b"".join(b"$%d\r\n" % len(x) + x + b"\r\n" for x in args))
        result = self.read()
        if self.executed is not None:
            self.executed.add(args[0].decode().upper())
        return result

    def read(self):
        line = self.stream.readline()
        if not line:
            raise EOFError("Redis connection closed")
        kind, body = line[:1], line[1:-2]
        if kind == b"-":
            raise RedisError(body.decode(errors="replace"))
        if kind == b"+":
            return body
        if kind == b":":
            return int(body)
        if kind == b"$":
            size = int(body)
            if size < 0:
                return None
            value = self.stream.read(size)
            assert self.stream.read(2) == b"\r\n"
            return value
        if kind == b"*":
            size = int(body)
            return None if size < 0 else [self.read() for _ in range(size)]
        raise ValueError("Unexpected RESP2 reply: %r" % line)


def equal(actual, expected):
    assert actual == expected, (actual, expected)


def error(client, expected, *args):
    try:
        client.cmd(*args)
    except RedisError as exc:
        assert expected in str(exc), (args, str(exc))
    else:
        raise AssertionError("Expected %s for %r" % (expected, args))


class Environment:
    def __init__(self, args):
        self.args = args
        self.directory = pathlib.Path(tempfile.mkdtemp(prefix="camellia-redis812-"))
        self.processes, self.clients, self.handles, self.results = [], [], [], []
        self.executed, self.inventory = set(), {}
        self.ports = set()
        self.java = pathlib.Path(args.java_home) / "bin"
        self.redis = pathlib.Path(args.redis_home) / "src"
        self.modules = [str(pathlib.Path(p).resolve()) for p in args.module]

    def port(self, cluster=False):
        while True:
            port = random.randrange(20000, 40000)
            if port in self.ports or port + 10000 in self.ports:
                continue
            probes = []
            try:
                for candidate in [port, port + 10000] if cluster else [port]:
                    probe = socket.socket()
                    probes.append(probe)
                    probe.bind(("127.0.0.1", candidate))
                self.ports.add(port)
                if cluster:
                    self.ports.add(port + 10000)
                return port
            except PermissionError:
                raise
            except OSError:
                pass
            finally:
                for probe in probes:
                    probe.close()

    def start(self, name, args):
        handle = (self.directory / (name + ".log")).open("wb")
        self.handles.append(handle)
        process = subprocess.Popen([str(x) for x in args], stdout=handle, stderr=subprocess.STDOUT)
        self.processes.append(process)
        return process

    def client(self, port, timeout=4):
        client = Client(port, timeout, self.executed)
        self.clients.append(client)
        return client

    def ready(self, port, process):
        deadline = time.monotonic() + 25
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError("Server exited; see " + str(self.directory))
            try:
                probe = Client(port, 0.5)
                try:
                    if probe.cmd("PING") == b"PONG":
                        return
                finally:
                    probe.close()
            except (OSError, RedisError, EOFError):
                time.sleep(0.05)
        raise TimeoutError("Server did not start on %d" % port)

    def server(self, cluster=False):
        port = self.port(cluster)
        folder = self.directory / str(port)
        folder.mkdir()
        args = [self.redis / "redis-server", "--port", port, "--bind", "127.0.0.1", "--protected-mode", "yes",
                "--save", "", "--appendonly", "no", "--dir", folder]
        for module in self.modules:
            args += ["--loadmodule", module]
        if cluster:
            # All client and cluster-bus sockets are bound to loopback only.
            args += ["--cluster-enabled", "yes", "--cluster-bus-port-protected-mode", "no",
                     "--cluster-config-file", "nodes.conf", "--cluster-node-timeout", "1000"]
        process = self.start("redis-%d" % port, args)
        self.ready(port, process)
        return port

    def proxy(self, upstream, mode="off"):
        port = self.port()
        cp = str(self.directory) + ":" + pathlib.Path(self.args.classpath_file).read_text().strip()
        process = self.start("proxy-%d" % port,
                             [self.java / "java", "-Xmx192m", "-cp", cp, "RedisIntegrationProxy", port, upstream, mode])
        self.ready(port, process)
        return port

    def check(self, name, fn):
        started = time.monotonic()
        try:
            fn()
            result = {"name": name, "status": "PASS"}
        except Exception as exc:
            result = {"name": name, "status": "FAIL", "error": str(exc), "traceback": traceback.format_exc()}
        result["seconds"] = round(time.monotonic() - started, 3)
        self.results.append(result)
        print(result["status"], name, result.get("error", ""), flush=True)

    def supported(self, client, command):
        return client.cmd("COMMAND", "INFO", command)[0] is not None

    def blocking(self, port, backend, name, args, produce, expected):
        reader = self.client(port, 5)
        producer = self.client(port, 1)
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            future = pool.submit(reader.cmd, name, *args)
            try:
                deadline = time.monotonic() + 3
                while time.monotonic() < deadline:
                    blocked = False
                    for line in backend.cmd("CLIENT", "LIST").splitlines():
                        fields = dict(field.split(b"=", 1) for field in line.split() if b"=" in field)
                        if fields.get(b"cmd") == name.lower().encode() and b"b" in fields.get(b"flags", b""):
                            blocked = True
                    if blocked and not future.done():
                        break
                    time.sleep(0.02)
                else:
                    raise AssertionError("Reader did not block on upstream")
                assert not future.done()
                start = time.monotonic()
                equal(producer.cmd("PING"), b"PONG")
                assert time.monotonic() - start < 0.8, "Unrelated connection was stalled"
                produce(producer)
                equal(future.result(timeout=2), expected)
            finally:
                # Ensure failed assertions do not leave an infinite BLOCK waiter.
                if not future.done():
                    reader.socket.shutdown(socket.SHUT_RDWR)

    def run(self):
        cp = pathlib.Path(self.args.classpath_file).read_text().strip()
        subprocess.run([str(self.java / "javac"), "-cp", cp, "-d", str(self.directory),
                        str(pathlib.Path(__file__).with_name("RedisIntegrationProxy.java"))], check=True)
        standalone = self.server()
        direct = self.client(standalone)
        version = next(line.split(b":", 1)[1].decode() for line in direct.cmd("INFO", "SERVER").splitlines() if line.startswith(b"redis_version:"))
        loaded = []
        for entry in direct.cmd("MODULE", "LIST"):
            module = dict(zip(entry[::2], entry[1::2]))
            loaded.append({"name": module[b"name"].decode(), "version": module[b"ver"]})
        self.inventory = {"redis_version": version, "loaded_modules": loaded}
        plain_port = self.proxy("redis://@127.0.0.1:%d" % standalone)
        values_port = self.proxy("redis://@127.0.0.1:%d" % standalone, "values")
        plain, values = self.client(plain_port), self.client(values_port)

        def strings():
            equal(values.cmd("MSETEX", 2, "{str}:a", b"\x00a\xff", "{str}:b", "b", "EX", 3600), 1)
            equal(direct.cmd("GET", "e2e:{str}:a"), b"v:\x00a\xff")
            equal(values.cmd("GET", "{str}:a"), b"\x00a\xff")
            equal(values.cmd("DELEX", "{str}:a", "IFEQ", b"\x00a\xff"), 1)
            plain.cmd("SET", "numeric", 10)
            plain.cmd("INCREX", "numeric", "BYINT", 2, "EX", 3600)
            equal(plain.cmd("GET", "numeric"), b"12")
            assert plain.cmd("DIGEST", "numeric")
            plain.cmd("SETBIT", "bitmap", 3, 1)
            equal(plain.cmd("BITRANGEINTARRAY", "bitmap", 0, 7, "BIT"), [3])

        def hashes():
            values.cmd("HSETEX", "hash", "FIELDS", 2, "FIELDS", "a", "b", "b")
            equal(direct.cmd("HGET", "e2e:hash", "f:FIELDS"), b"v:a")
            equal(values.cmd("HGETEX", "hash", "PX", 3600000, "FIELDS", 2, "FIELDS", "missing"), [b"a", None])
            for name, expiry in [("HEXPIRE", 3600), ("HPEXPIRE", 3600000),
                                 ("HEXPIREAT", int(time.time()) + 3600), ("HPEXPIREAT", int(time.time() * 1000) + 3600000)]:
                equal(values.cmd(name, "hash", expiry, "FIELDS", 1, "FIELDS"), [1])
            for name in ["HTTL", "HPTTL", "HEXPIRETIME", "HPEXPIRETIME"]:
                assert values.cmd(name, "hash", "FIELDS", 1, "FIELDS")[0] > 0
            equal(values.cmd("HPERSIST", "hash", "FIELDS", 1, "FIELDS"), [1])
            equal(values.cmd("HGETDEL", "hash", "FIELDS", 2, "FIELDS", "missing"), [b"a", None])

        def sets():
            values.cmd("SADDEX", "set", "MEMBERS", 2, "MEMBERS", "a")
            equal(direct.cmd("SISMEMBER", "e2e:set", "v:MEMBERS"), 1)
            for name, expiry in [("SEXPIRE", 3600), ("SPEXPIRE", 3600000),
                                 ("SEXPIREAT", int(time.time()) + 3600), ("SPEXPIREAT", int(time.time() * 1000) + 3600000)]:
                equal(values.cmd(name, "set", expiry, "MEMBERS", 1, "MEMBERS"), [1])
            for name in ["STTL", "SPTTL", "SEXPIRETIME", "SPEXPIRETIME"]:
                assert values.cmd(name, "set", "MEMBERS", 1, "MEMBERS")[0] > 0
            equal(values.cmd("SPERSIST", "set", "MEMBERS", 1, "MEMBERS"), [1])
            values.cmd("SADD", "{set}:a", "a", "b")
            values.cmd("SADD", "{set}:b", "b", "c")
            equal(values.cmd("SDIFFCARD", 2, "{set}:a", "{set}:b"), 1)
            equal(values.cmd("SUNIONCARD", 2, "{set}:a", "{set}:b"), 3)

        def streams():
            values.cmd("XGROUP", "CREATE", "stream", "g", "0", "MKSTREAM")
            item = values.cmd("XADD", "stream", "*", "field", "a")
            values.cmd("XREADGROUP", "GROUP", "g", "c", "STREAMS", "stream", ">")
            claimed = values.cmd("XAUTOCLAIM", "stream", "g", "c2", 0, "0-0", "COUNT", 1)
            equal(claimed[1][0][0], item)
            values.cmd("XNACK", "stream", "g", "SILENT", "IDS", 1, item)
            values.cmd("XACKDEL", "stream", "g", "IDS", 1, item)
            item2 = values.cmd("XADD", "stream", "*", "field", "b")
            values.cmd("XDELEX", "stream", "IDS", 1, item2)
            values.cmd("XCFGSET", "stream", "IDMP-DURATION", 60, "IDMP-MAXSIZE", 100)
            equal(direct.cmd("EXISTS", "e2e:stream"), 1)

        def arrays():
            values.cmd("ARSET", "array", 0, "1", "2", "3")
            values.cmd("ARMSET", "array", 4, "4", 5, "5")
            equal(values.cmd("ARGET", "array", 0), b"1")
            values.cmd("ARMGET", "array", 0, 5)
            for name, tail in [("ARGETRANGE", [0, 5]), ("ARLEN", []), ("ARCOUNT", []), ("ARINFO", []),
                               ("ARNEXT", []), ("ARLASTITEMS", [2]), ("ARSCAN", [0, 5]),
                               ("ARGREP", [0, 5, "EXACT", "1"]), ("AROP", [0, 5, "SUM"])]:
                values.cmd(name, "array", *tail)
            values.cmd("ARSEEK", "array", 6)
            values.cmd("ARINSERT", "array", "6")
            values.cmd("ARDEL", "array", 4)
            values.cmd("ARDELRANGE", "array", 4, 6)
            values.cmd("ARRING", "ring", 3, "a", "b")
            equal(direct.cmd("ARGET", "e2e:array", 0), b"1")
            equal(direct.cmd("EXISTS", "array"), 0)

        def moves():
            values.cmd("RPUSH", "{list}:s", "a", "b")
            equal(values.cmd("LMOVEM", "{list}:s", "{list}:d", "LEFT", "RIGHT", "COUNT", 2, "BULK"), [b"a", b"b"])
            equal(direct.cmd("LRANGE", "e2e:{list}:d", 0, -1), [b"v:a", b"v:b"])
            values.cmd("ZADD", "{zset}:s", 1.25, "a", 2.5, "NX")
            equal(values.cmd("ZMOVEMEMBER", "{zset}:s", "{zset}:d", "MEMBERS", 1, "NX", "NX"), [[b"NX", b"2.5"]])
            equal(values.cmd("ZMOVE", "{zset}:s", "{zset}:d", "MIN"), [[b"a", b"1.25"]])
            equal(direct.cmd("ZSCORE", "e2e:{zset}:d", "v:a"), b"1.25")

        self.check("String: MSETEX/DELEX/INCREX/DIGEST + binary value conversion", strings)
        self.check("Hash: all 12 new commands + fields/values/null replies", hashes)
        self.check("Set: all 12 new commands + member conversion", sets)
        self.check("Stream: all 5 new commands + key conversion", streams)
        self.check("Array: all 18 commands + underlying key inspection", arrays)
        self.check("List/ZSet moves: member conversion and destination data", moves)
        self.check("BLMOVEM: blocking connection isolation and wakeup",
                   lambda: self.blocking(values_port, direct, "BLMOVEM", ["{bl}:s", "{bl}:d", "LEFT", "RIGHT", 0],
                                         lambda p: p.cmd("RPUSH", "{bl}:s", "a"), [b"a"]))
        self.check("BZMOVE: blocking connection isolation and nested member decoding",
                   lambda: self.blocking(values_port, direct, "BZMOVE", ["{bz}:s", "{bz}:d", "MIN", 0],
                                         lambda p: p.cmd("ZADD", "{bz}:s", 1.25, "a"), [[b"a", b"1.25"]]))
        def blocking_timeout():
            started = time.monotonic()
            equal(values.cmd("BZMOVE", "{timeout}:s", "{timeout}:d", "MIN", 0.05), None)
            assert time.monotonic() - started >= 0.03
            values.cmd("RPUSH", "after-timeout", "a")
            equal(values.cmd("LRANGE", "after-timeout", 0, -1), [b"a"])
            equal(direct.cmd("LRANGE", "e2e:after-timeout", 0, -1), [b"v:a"])
        self.check("Blocking timeout produces nil and keeps connection usable", blocking_timeout)

        self.module_checks(values, direct, values_port)
        nodes = [self.server(True) for _ in range(3)]
        with (self.directory / "cluster-create.log").open("w") as cluster_log:
            subprocess.run([str(self.redis / "redis-cli"), "--cluster", "create", *["127.0.0.1:%d" % p for p in nodes],
                            "--cluster-replicas", "0", "--cluster-yes"], check=True, stdout=cluster_log)
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if all(b"cluster_state:ok" in self.client(p).cmd("CLUSTER", "INFO") for p in nodes):
                break
            time.sleep(0.1)
        else:
            raise TimeoutError("The temporary 3-master cluster did not converge")
        cluster_port = self.proxy("redis-cluster://@127.0.0.1:%d,127.0.0.1:%d,127.0.0.1:%d" % tuple(nodes), "values")
        cluster = self.client(cluster_port)

        def cluster_routing():
            equal(cluster.cmd("MSETEX", 2, "{cluster}:a", "a", "{cluster}:b", "b", "EX", 3600), 1)
            equal(cluster.cmd("GET", "{cluster}:a"), b"a")
            for args in [("MSETEX", 2, "{a}:x", "a", "{b}:x", "b", "EX", 3600),
                         ("LMOVEM", "{a}:s", "{b}:d", "LEFT", "RIGHT"),
                         ("BLMOVEM", "{a}:s", "{b}:d", "LEFT", "RIGHT", 0),
                         ("ZMOVE", "{a}:s", "{b}:d", "MIN"), ("BZMOVE", "{a}:s", "{b}:d", "MIN", 0),
                         ("ZMOVEMEMBER", "{a}:s", "{b}:d", "MEMBERS", 1, "a"),
                         ("SDIFFCARD", 2, "{a}:s", "{b}:s"), ("SUNIONCARD", 2, "{a}:s", "{b}:s")]:
                error(cluster, "CROSSSLOT", *args)
            # Find the physical owner instead of assuming a node/slot mapping.
            owners = 0
            for port in nodes:
                backend = self.client(port)
                try:
                    value = backend.cmd("GET", "e2e:{cluster}:a")
                    equal(value, b"v:a")
                    owners += 1
                except RedisError as exc:
                    assert str(exc).startswith("MOVED")
            equal(owners, 1)

        self.check("3-master cluster: same-slot success/cross-slot rejection/physical owner", cluster_routing)
        # Blocking command must use the owner node's dedicated connection.
        cluster.cmd("ZADD", "{cbz}:s", 1, "old")
        cluster.cmd("DEL", "{cbz}:s")
        owner = None
        for port in nodes:
            node = self.client(port)
            try:
                node.cmd("EXISTS", "e2e:{cbz}:s")
                owner = node
                break
            except RedisError as exc:
                assert str(exc).startswith("MOVED")
        assert owner is not None
        self.check("Cluster BZMOVE: correct node, isolated connection, wakeup",
                   lambda: self.blocking(cluster_port, owner, "BZMOVE", ["{cbz}:s", "{cbz}:d", "MAX", 0],
                                         lambda p: p.cmd("ZADD", "{cbz}:s", 3.5, "a"), [[b"a", b"3.5"]]))
        if self.supported(direct, "TS.CREATE"):
            def cluster_timeseries():
                cluster.cmd("TS.CREATE", "{cts}:a", "LABELS", "integration", "redis812")
                cluster.cmd("TS.CREATE", "{cts}:b")
                cluster.cmd("TS.MADD", "{cts}:a", 1000, 1.25, "{cts}:b", 1000, 2.5)
                assert cluster.cmd("TS.NRANGE", 2, "{cts}:a", "{cts}:b", "-", "+")
                for args in [("TS.MADD", "{a}:ts", 1, 1, "{b}:ts", 1, 2),
                             ("TS.NRANGE", 2, "{a}:ts", "{b}:ts", "-", "+"),
                             ("TS.NREVRANGE", 2, "{a}:ts", "{b}:ts", "-", "+"),
                             ("TS.CREATERULE", "{a}:ts", "{b}:ts", "AGGREGATION", "AVG", 1000),
                             ("TS.DELETERULE", "{a}:ts", "{b}:ts")]:
                    error(cluster, "CROSSSLOT", *args)
                node = self.owner(nodes, "e2e:{cts}:a")
                self.blocking(cluster_port, node, "TS.READ", ["{cts}:a", "$", "BLOCK", 0, 1],
                              lambda p: p.cmd("TS.ADD", "{cts}:a", 2000, 3.5), [[2000, b"3.5"]])
            self.check("Cluster TimeSeries: same-slot/cross-slot and TS.READ BLOCK wakeup", cluster_timeseries)

    def owner(self, nodes, key):
        for port in nodes:
            node = self.client(port)
            try:
                node.cmd("EXISTS", key)
                return node
            except RedisError as exc:
                assert str(exc).startswith("MOVED")
        raise AssertionError("No cluster node owns " + key)

    def module_checks(self, proxy, direct, port):
        # This list is deliberately reported, never silently counted as passing.
        for name, test in [("Vector Set", self.vectors), ("CMS/Top-K/t-digest", self.probabilistic),
                           ("JSON", self.json_commands), ("Search", self.search), ("TimeSeries", self.timeseries)]:
            command = {"Vector Set": "VADD", "CMS/Top-K/t-digest": "CMS.INITBYDIM", "JSON": "JSON.SET",
                       "Search": "FT.CREATE", "TimeSeries": "TS.CREATE"}[name]
            if self.supported(direct, command):
                self.check(name + ": real module command/converter checks", lambda test=test: test(proxy, direct, port))
            else:
                self.results.append({"name": name, "status": "SKIP", "reason": "Upstream module not loaded"})
                print("SKIP", name, "module not loaded", flush=True)

    def vectors(self, p, direct, port):
        p.cmd("VADD", "vectors", "VALUES", 2, 0.1, 0.2, "a")
        equal(p.cmd("VCARD", "vectors"), 1)
        equal(p.cmd("VDIM", "vectors"), 2)
        for name, tail in [("VSIM", ["ELE", "a"]), ("VEMB", ["a"]), ("VLINKS", ["a"]), ("VINFO", []),
                           ("VRANDMEMBER", []), ("VISMEMBER", ["a"]), ("VRANGE", ["-", "+"])]:
            p.cmd(name, "vectors", *tail)
        p.cmd("VSETATTR", "vectors", "a", '{"region":"cn"}')
        assert p.cmd("VGETATTR", "vectors", "a")
        equal(direct.cmd("VCARD", "e2e:vectors"), 1)
        equal(p.cmd("VREM", "vectors", "a"), 1)

    def probabilistic(self, p, direct, port):
        p.cmd("CMS.INITBYDIM", "{cms}:a", 100, 5)
        p.cmd("CMS.INCRBY", "{cms}:a", "a", 2)
        equal(p.cmd("CMS.QUERY", "{cms}:a", "a"), [2])
        p.cmd("CMS.INFO", "{cms}:a")
        p.cmd("CMS.INITBYPROB", "{cms}:b", 0.01, 0.01)
        p.cmd("CMS.INITBYDIM", "{cms}:dest", 100, 5)
        p.cmd("CMS.MERGE", "{cms}:dest", 1, "{cms}:a", "WEIGHTS", 2)
        equal(direct.cmd("CMS.QUERY", "e2e:{cms}:dest", "a"), [4])
        p.cmd("TOPK.RESERVE", "top", 3)
        p.cmd("TOPK.ADD", "top", "a", "b")
        p.cmd("TOPK.INCRBY", "top", "a", 2)
        for name, tail in [("TOPK.QUERY", ["a"]), ("TOPK.COUNT", ["a"]), ("TOPK.LIST", []), ("TOPK.INFO", [])]:
            p.cmd(name, "top", *tail)
        p.cmd("TDIGEST.CREATE", "{td}:a")
        p.cmd("TDIGEST.ADD", "{td}:a", 1, 2, 3)
        p.cmd("TDIGEST.CREATE", "{td}:dest")
        p.cmd("TDIGEST.MERGE", "{td}:dest", 1, "{td}:a")
        for name, tail in [("MIN", []), ("MAX", []), ("QUANTILE", [0.5]), ("CDF", [2]), ("TRIMMED_MEAN", [0, 1]),
                           ("RANK", [2]), ("REVRANK", [2]), ("BYRANK", [1]), ("BYREVRANK", [1]), ("INFO", [])]:
            p.cmd("TDIGEST." + name, "{td}:dest", *tail)
        p.cmd("TDIGEST.RESET", "{td}:dest")

    def json_commands(self, p, direct, port):
        p.cmd("JSON.MSET", "{json}:a", "$", '{"a":1}', "{json}:b", "$", '{}')
        p.cmd("JSON.MERGE", "{json}:a", "$", '{"b":2}')
        equal(json.loads(p.cmd("JSON.GET", "{json}:a")), {"a": 1, "b": 2})
        equal(json.loads(direct.cmd("JSON.GET", "e2e:{json}:a")), {"a": 1, "b": 2})

    def search(self, p, direct, port):
        p.cmd("FT.SUGADD", "dictionary", "hello", 1.5)
        equal(p.cmd("FT.SUGLEN", "dictionary"), 1)
        equal(p.cmd("FT.SUGGET", "dictionary", "he"), [b"hello"])
        equal(direct.cmd("FT.SUGLEN", "e2e:dictionary"), 1)
        equal(p.cmd("FT.SUGDEL", "dictionary", "hello"), 1)
        p.cmd("FT.CREATE", "idx", "ON", "HASH", "PREFIX", 1, "e2e:doc:", "SCHEMA", "title", "TEXT",
              "embedding", "VECTOR", "FLAT", 6, "TYPE", "FLOAT32", "DIM", 2, "DISTANCE_METRIC", "L2")
        p.cmd("FT.ALIASADD", "alias", "idx")
        assert b"alias" in p.cmd("FT.ALIASLIST", "idx")
        import struct
        vector = struct.pack("ff", 0.1, 0.2)
        direct.cmd("HSET", "e2e:doc:a", "title", "hello", "embedding", vector)
        p.cmd("FT.HYBRID", "idx", "SEARCH", "hello", "VSIM", "@embedding", "$v", "PARAMS", 2, "v", vector)

    def timeseries(self, p, direct, port):
        p.cmd("TS.CREATE", "{ts}:a", "LABELS", "region", "cn")
        p.cmd("TS.CREATE", "{ts}:b")
        p.cmd("TS.ALTER", "{ts}:a", "RETENTION", 1000000)
        p.cmd("TS.MADD", "{ts}:a", 1000, 1.25, "{ts}:b", 1000, 2.5)
        p.cmd("TS.INCRBY", "{ts}:a", 1, "TIMESTAMP", 1001)
        p.cmd("TS.DECRBY", "{ts}:a", 1, "TIMESTAMP", 1002)
        p.cmd("TS.RANGE", "{ts}:a", "-", "+")
        p.cmd("TS.REVRANGE", "{ts}:a", "-", "+")
        p.cmd("TS.NRANGE", 2, "{ts}:a", "{ts}:b", "-", "+")
        p.cmd("TS.NREVRANGE", 2, "{ts}:a", "{ts}:b", "-", "+")
        p.cmd("TS.GET", "{ts}:a")
        p.cmd("TS.READ", "{ts}:a", 0)
        p.cmd("TS.CREATE", "{ts}:rollup")
        p.cmd("TS.CREATERULE", "{ts}:a", "{ts}:rollup", "AGGREGATION", "AVG", 1000)
        info = p.cmd("TS.INFO", "{ts}:a", "DEBUG")
        info = dict(zip(info[::2], info[1::2]))
        equal(info[b"keySelfName"], b"{ts}:a")
        equal(info[b"rules"][0][0], b"{ts}:rollup")
        destination = p.cmd("TS.INFO", "{ts}:rollup")
        equal(dict(zip(destination[::2], destination[1::2]))[b"sourceKey"], b"{ts}:a")
        p.cmd("TS.DELETERULE", "{ts}:a", "{ts}:rollup")
        for name in ["TS.MGET", "TS.MRANGE", "TS.MREVRANGE"]:
            args = [] if name == "TS.MGET" else ["-", "+"]
            equal(p.cmd(name, *args, "FILTER", "region=cn")[0][0], b"{ts}:a")
        assert b"{ts}:a" in p.cmd("TS.QUERYINDEX", "region=cn")
        grouped = p.cmd("TS.MRANGE", "-", "+", "FILTER", "region=cn", "GROUPBY", "region", "REDUCE", "SUM")
        equal(grouped[0][0], b"region=cn")
        p.cmd("TS.QUERYLABELS", "LABELS", "FILTER", "region=cn")
        p.cmd("TS.DEL", "{ts}:a", 1000, 1000)
        p.cmd("TS.CREATE", "read-series")
        p.cmd("TS.CREATE", "BLOCK")
        equal(p.cmd("TS.READ", "BLOCK", 0), [])
        self.blocking(port, direct, "TS.READ", ["read-series", "$", "BLOCK", 0, 1],
                      lambda producer: producer.cmd("TS.ADD", "read-series", 123, 1.25), [[123, b"1.25"]])

    def close(self):
        for client in self.clients:
            try:
                client.close()
            except OSError:
                pass
        for process in reversed(self.processes):
            if process.poll() is None:
                process.terminate()
        for process in reversed(self.processes):
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        for handle in self.handles:
            handle.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--redis-home", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", default="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home")
    parser.add_argument("--module", action="append", default=[])
    parser.add_argument("--report", required=True)
    parser.add_argument("--require-modules", action="store_true", help="Fail if any module group is skipped")
    args = parser.parse_args()
    env = Environment(args)
    print("Temporary logs:", env.directory, flush=True)
    try:
        env.run()
    except Exception as exc:
        env.results.append({"name": "environment", "status": "FAIL", "error": str(exc), "traceback": traceback.format_exc()})
        print(traceback.format_exc(), flush=True)
    finally:
        env.close()
        expected = {line for line in pathlib.Path(__file__).with_name("commands.txt").read_text().splitlines() if line and not line.startswith("#")}
        report = {"redis": str(env.redis), "modules": env.modules, "inventory": env.inventory,
                  "logs": str(env.directory), "results": env.results,
                  "commands_executed": sorted(expected & env.executed), "commands_not_executed": sorted(expected - env.executed)}
        pathlib.Path(args.report).write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    failed = any(r["status"] == "FAIL" or (args.require_modules and r["status"] == "SKIP") for r in env.results)
    print("Results:", {status: sum(r["status"] == status for r in env.results) for status in ["PASS", "FAIL", "SKIP"]}, flush=True)
    raise SystemExit(1 if failed else 0)


if __name__ == "__main__":
    main()
