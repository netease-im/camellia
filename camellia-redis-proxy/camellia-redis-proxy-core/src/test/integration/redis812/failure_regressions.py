#!/usr/bin/env python3
"""Verify that converter exceptions reject requests/replies instead of passing data through."""
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
    key = env.client(env.proxy(route, "failure-key"))
    value = env.client(env.proxy(route, "failure-value"))
    reply = env.client(env.proxy(route, "failure-reply"))
    direct.cmd("SET", "raw-secret", "sentinel")
    direct.cmd("SET", "e2e:encoded", "v:secret")

    def key_failure():
        error(key, "ERR request conversion failed", "GET", "raw-secret")
        error(key, "ERR request conversion failed", "SET", "raw-secret", "overwrite")
        equal(direct.cmd("GET", "raw-secret"), b"sentinel")
        equal(direct.cmd("EXISTS", "e2e:raw-secret"), 0)
        equal(key.cmd("PING"), b"PONG")

    def value_failure():
        error(value, "ERR request conversion failed", "SET", "new-key", "plaintext")
        equal(direct.cmd("EXISTS", "new-key", "e2e:new-key"), 0)
        equal(value.cmd("PING"), b"PONG")

    def reply_failure():
        error(reply, "ERR reply conversion failed", "GET", "encoded")
        error(reply, "ERR reply conversion failed", "MGET", "encoded", "encoded")
        equal(reply.cmd("GET", "missing"), None)
        equal(reply.cmd("PING"), b"PONG")

    env.check("Key conversion failure prevents raw reads and writes", key_failure)
    env.check("Value conversion failure prevents partially converted writes", value_failure)
    env.check("Reply conversion failure does not expose encoded values", reply_failure)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--redis-home", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--report", default="/tmp/camellia-failure-regressions.json")
    args = parser.parse_args()
    args.module = []
    env = Environment(args)
    try:
        run(env)
    finally:
        env.close()
        pathlib.Path(args.report).write_text(json.dumps(env.results, indent=2))
    raise SystemExit(1 if any(row["status"] != "PASS" for row in env.results) else 0)
