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
    values_port = env.proxy("redis://@127.0.0.1:%d" % port, "values")
    values = env.client(values_port)
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

    def missing_values():
        for args in [("LPOP", "nil-list"), ("RPOP", "nil-list"), ("LINDEX", "nil-list", 0),
                     ("SPOP", "nil-set"), ("SRANDMEMBER", "nil-set"), ("ZRANDMEMBER", "nil-zset")]:
            equal(values.cmd(*args), None)
        for name in ["BZPOPMIN", "BZPOPMAX"]:
            equal(values.cmd(name, "nil-zset", 0.01), None)
        equal(values.cmd("GET", "nil-string"), None)
        equal(values.cmd("PING"), b"PONG")

    def conditional_set():
        values.cmd("SET", "conditional", "old")
        equal(values.cmd("SET", "conditional", "new", "IFEQ", "old", "GET"), b"old")
        equal(values.cmd("SET", "conditional", "skip", "IFNE", "new"), None)
        equal(values.cmd("SET", "conditional", "last", "IFNE", "different", "PX", 10000, "GET"), b"new")
        digest = direct.cmd("DIGEST", "e2e:conditional")
        equal(values.cmd("SET", "conditional", "digest-new", "IFDEQ", digest, "GET"), b"last")
        digest = direct.cmd("DIGEST", "e2e:conditional")
        equal(values.cmd("SET", "conditional", "skip", "IFDNE", digest), None)

    def set_algebra():
        values.cmd("SADD", "set-a", "a", "b")
        values.cmd("SADD", "set-b", "b", "c")
        equal(sorted(values.cmd("SUNION", "set-a", "set-b")), [b"a", b"b", b"c"])
        equal(values.cmd("SINTER", "set-a", "set-b"), [b"b"])
        equal(values.cmd("SDIFF", "set-a", "set-b"), [b"a"])
        equal(values.cmd("SMOVE", "set-a", "set-dest", "a"), 1)
        equal(values.cmd("SMEMBERS", "set-dest"), [b"a"])
        equal(direct.cmd("SMEMBERS", "e2e:set-dest"), [b"v:a"])
        equal(values.cmd("SMOVE", "set-a", "set-dest", "missing"), 0)

    def list_moves():
        for name, tail in [("LMOVE", ["LEFT", "RIGHT"]), ("BLMOVE", ["LEFT", "RIGHT", 0.01]),
                           ("RPOPLPUSH", []), ("BRPOPLPUSH", [0.01])]:
            source = "legacy-" + name
            destination = source + "-dest"
            values.cmd("RPUSH", source, "a")
            equal(values.cmd(name, source, destination, *tail), b"a")
            equal(values.cmd("LRANGE", destination, 0, -1), [b"a"])
            equal(values.cmd(name, source, destination, *tail), None)

    def multi_pops():
        for name in ["LMPOP", "BLMPOP", "ZMPOP", "BZMPOP"]:
            first, second = "empty-" + name, "pop-" + name
            if "ZMPOP" in name:
                values.cmd("ZADD", second, 1, "a", 2, "b")
                side, payload = "MIN", [[b"a", b"1"], [b"b", b"2"]]
            else:
                values.cmd("RPUSH", second, "a", "b")
                side, payload = "LEFT", [b"a", b"b"]
            prefix = [0.01] if name.startswith("B") else []
            equal(values.cmd(name, *prefix, 2, first, second, side, "COUNT", 2), [second.encode(), payload])
            equal(values.cmd(name, *prefix, 2, first, second, side), None)

    def stream_keys():
        item = values.cmd("XADD", "reply-stream", "*", "field", "value")
        expected = [[b"reply-stream", [[item, [b"field", b"value"]]]]]
        equal(values.cmd("XREAD", "STREAMS", "reply-stream", 0), expected)
        for group, consumer in [("STREAMS", "BLOCK"), ("BLOCK", "STREAMS")]:
            values.cmd("XGROUP", "CREATE", "reply-stream", group, 0)
            equal(values.cmd("XREADGROUP", "GROUP", group, consumer, "STREAMS", "reply-stream", ">"), expected)

    def zset_algebra():
        values.cmd("ZADD", "algebra-a", 1, "a", 2, "b")
        values.cmd("ZADD", "algebra-b", 3, "b", 4, "c")
        equal(values.cmd("ZUNION", 2, "algebra-a", "algebra-b"), [b"a", b"c", b"b"])
        equal(values.cmd("ZINTER", 2, "algebra-a", "algebra-b", "WITHSCORES"), [b"b", b"5"])
        equal(values.cmd("ZDIFF", 2, "algebra-a", "algebra-b"), [b"a"])
        equal(values.cmd("ZDIFF", 2, "algebra-a", "algebra-b", "WITHSCORES"), [b"a", b"1"])

    for name, test in [("Hash numeric increments", increments), ("HSTRLEN field conversion", strlen),
                       ("HRANDFIELD WITHVALUES and nil", random_fields), ("HSCAN NOVALUES", scan),
                       ("SET GET", set_get), ("ZRANK/ZREVRANK WITHSCORE", ranks),
                       ("SORT/GEO store destinations and namespace isolation", stores),
                       ("Missing values and legacy blocking timeouts", missing_values),
                       ("SET IFEQ/IFNE and digest conditions", conditional_set),
                       ("Set algebra decoding and SMOVE member conversion", set_algebra),
                       ("Legacy List moves and nil responses", list_moves),
                       ("Multi-key List/ZSet pops use the actual source", multi_pops),
                       ("Stream reply keys and option-like group/consumer names", stream_keys),
                       ("ZSet algebra member decoding and scores", zset_algebra)]:
        env.check(name, test)

    env.check("BLMPOP wakeup decodes the actual source", lambda: env.blocking(
        values_port, direct, "BLMPOP", [2, 2, "blocked-list-empty", "blocked-list-source", "LEFT", "COUNT", 2],
        lambda producer: producer.cmd("RPUSH", "blocked-list-source", "a", "b"),
        [b"blocked-list-source", [b"a", b"b"]]))
    env.check("BZMPOP wakeup decodes members and preserves scores", lambda: env.blocking(
        values_port, direct, "BZMPOP", [2, 2, "blocked-zset-empty", "blocked-zset-source", "MIN", "COUNT", 2],
        lambda producer: producer.cmd("ZADD", "blocked-zset-source", 1, "a", 2, "b"),
        [b"blocked-zset-source", [[b"a", b"1"], [b"b", b"2"]]]))

    values.cmd("XGROUP", "CREATE", "blocking-stream", "STREAMS", 0, "MKSTREAM")
    env.check("XREADGROUP blocking wakeup preserves logical key", lambda: env.blocking(
        values_port, direct, "XREADGROUP", ["GROUP", "STREAMS", "BLOCK", "BLOCK", 2000, "STREAMS", "blocking-stream", ">"],
        lambda producer: producer.cmd("XADD", "blocking-stream", "1-0", "f", "v"),
        [[b"blocking-stream", [[b"1-0", [b"f", b"v"]]]]]))

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
