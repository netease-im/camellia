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


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--redis-home", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--report", default="/tmp/camellia-namespace-regressions.json")
    args = parser.parse_args()
    args.module = []
    env = Environment(args)
    try:
        run(env)
    finally:
        env.close()
        pathlib.Path(args.report).write_text(json.dumps(env.results, indent=2))
    raise SystemExit(1 if any(row["status"] != "PASS" for row in env.results) else 0)
