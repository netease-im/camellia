#!/usr/bin/env python3
"""Legacy converter regressions against isolated real Redis/proxy processes."""
import argparse
import json
import pathlib
import subprocess
import time

from run import Environment, equal, error


def run(env):
    cp = pathlib.Path(env.args.classpath_file).read_text().strip()
    subprocess.run([str(env.java / "javac"), "-cp", cp, "-d", str(env.directory),
                    str(pathlib.Path(__file__).with_name("RedisIntegrationProxy.java"))], check=True)
    port = env.server()
    direct = env.client(port)
    values = env.client(env.proxy("redis://@127.0.0.1:%d" % port, "values"))
    keys = env.client(env.proxy("redis://@127.0.0.1:%d" % port, "keys"))

    def increments():
        # Seed numeric storage directly to isolate field conversion from value encoding.
        direct.cmd("HSET", "e2e:inc", "f:n", 0)
        equal(values.cmd("HINCRBY", "inc", "n", 1), 1)
        equal(values.cmd("HINCRBYFLOAT", "inc", "n", "0.5"), b"1.5")
        equal(direct.cmd("HGET", "e2e:inc", "f:n"), b"1.5")

    def strlen():
        values.cmd("HSET", "strlen", "a", "A")
        equal(values.cmd("HSTRLEN", "strlen", "a"), 3)  # physical value is v:A
        equal(values.cmd("HSTRLEN", "strlen", "missing"), 0)

    def random_fields():
        values.cmd("HSET", "random", "a", "A")
        equal(values.cmd("HRANDFIELD", "random", 1, "WITHVALUES"), [b"a", b"A"])
        equal(values.cmd("HRANDFIELD", "random", 1), [b"a"])
        equal(values.cmd("HRANDFIELD", "missing"), None)

    def scan():
        for count in [1, 2, 3]:
            key = "scan%d" % count
            for i in range(count):
                values.cmd("HSET", key, str(i), "A")
            cursor, fields = values.cmd("HSCAN", key, 0, "COUNT", 100, "NOVALUES")
            equal(cursor, b"0")
            equal(sorted(fields), [str(i).encode() for i in range(count)])
        values.cmd("HSET", "pattern", "NOVALUES", "A")
        equal(values.cmd("HSCAN", "pattern", 0, "MATCH", "f:NOVALUES"), [b"0", [b"NOVALUES", b"A"]])

    def set_get():
        values.cmd("SET", "string", "old")
        equal(values.cmd("SET", "string", "new", "GET"), b"old")
        equal(values.cmd("SET", "string", "skip", "NX", "GET"), b"new")
        equal(values.cmd("GET", "string"), b"new")
        equal(values.cmd("SET", "missing-string", "new", "GET"), None)

    def ranks():
        values.cmd("ZADD", "zset", 1, "a")
        equal(values.cmd("ZRANK", "zset", "a", "WITHSCORE"), [0, b"1"])
        equal(values.cmd("ZREVRANK", "zset", "a", "WITHSCORE"), [0, b"1"])
        equal(values.cmd("ZRANK", "zset", "missing", "WITHSCORE"), None)

    def stores():
        direct.cmd("SET", "sort-dest", "foreign-sentinel")
        keys.cmd("RPUSH", "sort-source", 2, 1)
        equal(keys.cmd("SORT", "sort-source", "STORE", "sort-dest"), 2)
        equal(direct.cmd("GET", "sort-dest"), b"foreign-sentinel")
        equal(direct.cmd("LRANGE", "e2e:sort-dest", 0, -1), [b"1", b"2"])
        equal(keys.cmd("SORT", "sort-source", "BY", "nosort", "GET", "#"), [b"2", b"1"])
        keys.cmd("GEOADD", "geo", 0, 0, "a")
        equal(keys.cmd("GEORADIUS", "geo", 0, 0, 1, "km", "STORE", "geo-dest"), 1)
        equal(keys.cmd("GEORADIUSBYMEMBER", "geo", "a", 1, "km", "STOREDIST", "geo-dist"), 1)
        equal(direct.cmd("ZRANGE", "e2e:geo-dest", 0, -1), [b"a"])
        equal(direct.cmd("ZSCORE", "e2e:geo-dist", "a"), b"0")
        equal(direct.cmd("EXISTS", "geo-dest", "geo-dist"), 0)

    for name, test in [("Hash numeric increments", increments), ("HSTRLEN field conversion", strlen),
                       ("HRANDFIELD WITHVALUES and nil", random_fields), ("HSCAN NOVALUES", scan),
                       ("SET GET", set_get), ("ZRANK/ZREVRANK WITHSCORE", ranks),
                       ("SORT/GEO store destinations and namespace isolation", stores)]:
        env.check(name, test)

    nodes = [env.server(True) for _ in range(3)]
    with (env.directory / "cluster-create.log").open("w") as log:
        subprocess.run([str(env.redis / "redis-cli"), "--cluster", "create",
                        *["127.0.0.1:%d" % p for p in nodes], "--cluster-replicas", "0", "--cluster-yes"],
                       check=True, stdout=log)
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        if all(b"cluster_state:ok" in env.client(p).cmd("CLUSTER", "INFO") for p in nodes):
            break
        time.sleep(0.1)
    else:
        raise TimeoutError("The temporary cluster did not converge")
    cluster = env.client(env.proxy("redis-cluster://@" + ",".join("127.0.0.1:%d" % p for p in nodes), "keys"))

    def cluster_stores():
        cluster.cmd("RPUSH", "{sort}:source", 2, 1)
        equal(cluster.cmd("SORT", "{sort}:source", "STORE", "{sort}:destination"), 2)
        equal(cluster.cmd("LRANGE", "{sort}:destination", 0, -1), [b"1", b"2"])
        cluster.cmd("GEOADD", "{geo}:source", 0, 0, "a")
        equal(cluster.cmd("GEORADIUS", "{geo}:source", 0, 0, 1, "km", "STORE", "{geo}:destination"), 1)
        equal(cluster.cmd("GEORADIUSBYMEMBER", "{geo}:source", "a", 1, "km", "STOREDIST", "{geo}:dist"), 1)
        equal(cluster.cmd("ZSCORE", "{geo}:dist", "a"), b"0")
        for args in [("SORT", "{a}:source", "STORE", "{b}:dest"),
                     ("GEORADIUS", "{a}:source", 0, 0, 1, "km", "STORE", "{b}:dest"),
                     ("GEORADIUSBYMEMBER", "{a}:source", "a", 1, "km", "STOREDIST", "{b}:dest")]:
            error(cluster, "CROSSSLOT", *args)

    env.check("Cluster STORE same-slot success and cross-slot rejection", cluster_stores)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--redis-home", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--report", default="/tmp/camellia-converter-regressions.json")
    args = parser.parse_args()
    args.module = []
    env = Environment(args)
    try:
        run(env)
    finally:
        env.close()
        pathlib.Path(args.report).write_text(json.dumps(env.results, indent=2))
    raise SystemExit(1 if any(row["status"] != "PASS" for row in env.results) else 0)
