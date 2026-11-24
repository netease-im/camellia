#!/usr/bin/env python3
"""Verify discovery isolation using the built-in namespace converter and real Redis."""
import argparse
import json
import pathlib
import subprocess

from run import Environment, equal, error


def run(env):
    cp = pathlib.Path(env.args.classpath_file).read_text().strip()
    subprocess.run([str(env.java / "javac"), "-cp", cp, "-d", str(env.directory),
                    str(pathlib.Path(__file__).with_name("RedisIntegrationProxy.java"))], check=True)
    port = env.server()
    direct = env.client(port)
    route = "redis://@127.0.0.1:%d" % port
    active = env.client(env.proxy(route, "namespace"))
    disabled = env.client(env.proxy(route, "namespace-disabled"))
    plain = env.client(env.proxy(route))
    direct.cmd("SET", "2|other|secret", "foreign-value")
    foreign = env.client(port)
    foreign.cmd("SUBSCRIBE", "2|other|private-channel")

    def random_key():
        before = direct.cmd("INFO", "commandstats")
        error(active, "RANDOMKEY is not supported", "RANDOMKEY")
        after = direct.cmd("INFO", "commandstats")
        # Rejection must happen before forwarding RANDOMKEY to Redis.
        equal([line for line in before.splitlines() if line.startswith(b"cmdstat_randomkey:")],
              [line for line in after.splitlines() if line.startswith(b"cmdstat_randomkey:")])
        equal(active.cmd("PING"), b"PONG")

    env.check("Active namespace rejects RANDOMKEY before forwarding", random_key)
    env.check("Foreign-only channels are invisible", lambda: equal(active.cmd("PUBSUB", "CHANNELS"), []))
    own = env.client(port)
    own.cmd("SUBSCRIBE", "1|default|own-channel")

    def channels():
        equal(active.cmd("PUBSUB", "CHANNELS"), [b"own-channel"])
        equal(active.cmd("PUBSUB", "CHANNELS", "*"), [b"own-channel"])
        equal(active.cmd("PUBSUB", "CHANNELS", "own-*"), [b"own-channel"])
        equal(active.cmd("PUBSUB", "CHANNELS", "absent*"), [])

    env.check("Implicit and explicit channel patterns isolate tenants and decode names", channels)

    def unscoped(client):
        equal(client.cmd("RANDOMKEY"), b"2|other|secret")
        equal(sorted(client.cmd("PUBSUB", "CHANNELS")), [b"1|default|own-channel", b"2|other|private-channel"])

    env.check("Disabled tenant converter preserves unscoped discovery", lambda: unscoped(disabled))
    env.check("No key converter preserves original behavior", lambda: unscoped(plain))

    def shard_channels():
        foreign_shard = env.client(port)
        own_shard = env.client(port)
        foreign_shard.cmd("SSUBSCRIBE", "2|other|private-shard")
        own_shard.cmd("SSUBSCRIBE", "1|default|own-shard")
        equal(active.cmd("PUBSUB", "SHARDCHANNELS"), [b"own-shard"])
        equal(active.cmd("PUBSUB", "SHARDCHANNELS", "own-*"), [b"own-shard"])
        equal(active.cmd("PUBSUB", "SHARDNUMSUB", "own-shard"), [b"own-shard", 1])
        equal(sorted(disabled.cmd("PUBSUB", "SHARDCHANNELS")), [b"1|default|own-shard", b"2|other|private-shard"])

    def empty_unsubscribe():
        client = env.client(env.proxy(route, "namespace"))
        for name in ["UNSUBSCRIBE", "PUNSUBSCRIBE", "SUNSUBSCRIBE"]:
            equal(client.cmd(name), [name.lower().encode(), None, 0])
        equal(client.cmd("GET", "missing"), None)

    def short_hashtag():
        direct.cmd("SET", "1|default|{1}", "external-value")
        equal(active.cmd("KEYS", "*"), [b"{1}"])

    env.check("Shard channel discovery isolates tenants and decodes names", shard_channels)
    env.check("Empty unsubscribe preserves null channels and shared nil", empty_unsubscribe)
    env.check("Short externally written hashtag does not fail decoding", short_hashtag)

    if env.args.timeseries_module:
        def label_queries():
            direct.cmd("TS.ADD", "2|other|private-series", 1, 42, "LABELS", "app", "shared")
            active.cmd("TS.ADD", "own-series", 1, 7, "LABELS", "app", "shared")
            equal(active.cmd("TS.GET", "own-series"), [1, b"7"])
            for args in [("TS.QUERYINDEX", "app=shared"), ("TS.QUERYLABELS", "LABELS", "FILTER", "app=shared"),
                         ("TS.MGET", "FILTER", "app=shared"), ("TS.MRANGE", "-", "+", "FILTER", "app=shared"),
                         ("TS.MREVRANGE", "+", "-", "FILTER", "app=shared")]:
                error(active, "TimeSeries label queries are not supported", *args)
            equal(sorted(disabled.cmd("TS.QUERYINDEX", "app=shared")), [b"1|default|own-series", b"2|other|private-series"])
            equal(sorted(plain.cmd("TS.QUERYINDEX", "app=shared")), [b"1|default|own-series", b"2|other|private-series"])

        env.check("TimeSeries label queries reject active namespaces and preserve disabled behavior", label_queries)



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--redis-home", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--report", default="/tmp/camellia-namespace-regressions.json")
    parser.add_argument("--timeseries-module", help="Optional RedisTimeSeries module for cross-tenant label-query checks")
    args = parser.parse_args()
    args.module = [args.timeseries_module] if args.timeseries_module else []
    env = Environment(args)
    try:
        run(env)
    finally:
        env.close()
        pathlib.Path(args.report).write_text(json.dumps(env.results, indent=2))
    raise SystemExit(1 if any(row["status"] != "PASS" for row in env.results) else 0)
