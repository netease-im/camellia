# Redis 8.12 真实实例验证

这是手动运行的端到端验证，不在默认 Maven 单元测试中执行。只依赖 Python 标准库、JDK 21、Redis 8.12 的 `redis-server/redis-cli` 和对应模块。

脚本自动选择空闲端口，启动一个独立 Redis、3 个集群主节点及真实 proxy 进程。所有 Redis 数据、配置和日志写入临时目录，正常结束或测试失败后会清理本次启动的进程。不会连接已有 Redis 实例，不使用 `FLUSHALL`。

## 准备 classpath

在仓库根目录执行：

```bash
mvnJDK21 -pl camellia-redis-proxy/camellia-redis-proxy-core -am \
  -DskipTests package dependency:build-classpath \
  -Dmdep.outputFile=/tmp/camellia-e2e-deps

python3 - <<'PY'
from pathlib import Path
root = Path.cwd()
Path('/tmp/camellia-e2e-classpath').write_text(
    str(root / 'camellia-redis-proxy/camellia-redis-proxy-core/target/classes')
    + ':' + Path('/tmp/camellia-e2e-deps').read_text().strip()
)
PY
```

## 运行

将路径替换为本机对应的 Redis 源码/构建目录和模块产物：

```bash
python3 camellia-redis-proxy/camellia-redis-proxy-core/src/test/integration/redis812/run.py \
  --redis-home /path/to/redis-8.12.0 \
  --classpath-file /tmp/camellia-e2e-classpath \
  --module /path/to/redisbloom.so \
  --module /path/to/redistimeseries.so \
  --module /path/to/rejson.so \
  --module /path/to/redisearch.so \
  --require-modules \
  --report /tmp/camellia-redis812-e2e.json
```

默认使用本机 JDK 21 路径，可通过 `--java-home` 覆盖。Redis 8.12 核心默认已内置 Vector Set，不要重复加载 `vset.so`。

只验证部分模块时，可以省略缺少的 `--module` 和 `--require-modules`。缺少的模块会明确记为 `SKIP`；加上 `--require-modules` 后，任何跳过都会让脚本以失败状态退出。模块加载失败、测试断言失败也会返回非零状态。

需要允许监听本地临时端口。脚本输出日志目录；JSON 报告记录 Redis/模块实际版本、成功执行的新增命令、未执行命令、各场景状态及失败堆栈。`commands.txt` 是本次 issue #509 新增的 126 个命令清单，不能用测试场景数量代替命令覆盖数量。

## 覆盖内容

- 真实请求经过 proxy；直接读取 Redis，核实 key、Hash field、String/List/Set/ZSet value/member 的编码。
- Array、Vector Set、CMS、Top-K、t-digest、Hash/Set/String/Stream/JSON/Search/TimeSeries 的新增命令。
- `BLMOVEM`、`BZMOVE`、`TS.READ BLOCK 0`：确认请求在 Redis 阻塞、其他客户端不被拖住、写入后能唤醒，并检查返回值逆转换。
- 阻塞超时返回空值后，连接仍可继续使用。
- 3 主节点集群：同 slot 成功、跨 slot 拒绝，直接读取物理节点确认数据位置；集群 `BZMOVE` 和 `TS.READ BLOCK` 使用正确节点的独立连接。
- TimeSeries 查询结果、`TS.INFO` 的规则源/目标 key 逆转换，以及 `GROUPBY` 合成组名保持原样。

测试 converter 使用二进制安全的固定前缀：key 为 `e2e:`、field 为 `f:`、value/member 为 `v:`。不会转换 TS 数值、JSON 内容或 Search 查询。固定前缀让源/目标编码兼容，避免把跨 key 重新编码误算为 Redis 搬移命令的能力。

## Converter 历史问题回归

复用上面的 classpath，以下脚本无需额外 Redis 模块，会独立启动 Redis、3 主节点集群和 proxy，并在结束时清理进程：

```bash
python3 camellia-redis-proxy/camellia-redis-proxy-core/src/test/integration/redis812/converter_regressions.py \
  --redis-home /path/to/redis-8.12.0 \
  --classpath-file /tmp/camellia-e2e-classpath \
  --java-home /path/to/jdk-21 \
  --report /tmp/camellia-converter-regressions.json
```

覆盖 Hash 自增参数、HSTRLEN field、HRANDFIELD WITHVALUES、HSCAN NOVALUES、SET GET、ZRANK/ZREVRANK WITHSCORE，以及 SORT/GEORADIUS/GEORADIUSBYMEMBER 的目标 key 转换、前缀外数据保护和集群同 slot/跨 slot 路由。
HSTRLEN 校验的是编码后存储值的长度；Hash 自增直接预置数值，避免将 value 编码限制误判为 field 转换问题。SORT 仅验证主 key 和 STORE 目标，不验证外部 BY/GET key 模式，限制见 [converter 文档](../../../../../../docs/camellia-redis-proxy/plugin/converter.md)。

2026-11-24：真实 Redis 8.12.0 上 8 项场景全部通过；相关 Maven 单测 58 项通过（0 失败、0 错误）。
