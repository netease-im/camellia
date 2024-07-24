
## 关于lua和function

* camellia-redis-proxy支持eval/evalsha/eval_ro/evalsha_ro/fcall/fcall_ro/tfcall/tfcallasync，当代理到redis-cluster或者自定义分片时，proxy会检查每个key是否指向同一个后端node或者slot
* proxy也支持script命令，支持load/flush/exists/kill参数，当使用load/flush/kill参数时，proxy会把脚本发往所有的写后端，当使用exists参数时，会发往所有读后端
* proxy也支持function命令，支持delete/flush/kill/list/load/restore参数，当使用delete/flush/kill/load/restore参数时，会发给所有写后端，当使用dump/list参数时，会发给任意读后端（redis-cluster会发给任意一个主节点）
* proxy也支持tfunction命令，支持delete/load参数，当使用delete/load参数时，会发给所有写后端，当使用list参数时，会发给任意读后端（redis-cluster会发给任意一个主节点）

### 例子一
```
redis-cluster://@127.0.0.1:6379,127.0.0.1:6380
```
* script load/flush会发给上述redis-cluster的所有master节点，并返回第一个master的回包
* script exists会发给上述redis-cluster的所有master节点，并汇总每个master的回包之和
* function delete/flush/kill/list/load/restore会发给上述redis-cluster的所有master节点，并返回第一个master的回包
* function dump/list会发给上述redis-cluster的任意一个master节点
* tfunction delete/load会发给上述redis-cluster的所有master节点，并返回第一个master的回包
* tfunction list会发给上述redis-cluster的任意一个master节点

### 例子二
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://@127.0.0.1:6379",
    "type": "rw_separate",
    "write": "redis://@127.0.0.1:6378"
  }
}
```
* script load/flush会发给redis://@127.0.0.1:6378
* script exists会发给redis://@127.0.0.1:6379
* function delete/flush/kill/list/load/restore会发给redis://@127.0.0.1:6378
* function dump/list会发给redis://@127.0.0.1:6379
* tfunction delete/load会发给redis://@127.0.0.1:6378
* tfunction list会发给redis://@127.0.0.1:6379

### 例子三
```json
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://@127.0.0.1:6379",
      "1-3-5": "redis-cluster://@127.0.0.1:6378,127.0.0.1:6377"
    },
    "bucketSize": 6
  }
}
```
* script load/flush会发给redis://@127.0.0.1:6379以及redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的所有master节点
* script exists会发给redis://@127.0.0.1:6379以及redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的所有master节点，并汇总结果返回
* function delete/flush/kill/list/load/restore会发给redis://@127.0.0.1:6379以及redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的所有master节点
* function dump/list会发给redis://@127.0.0.1:6379或者redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的任意一个master节点
* tfunction delete/load会发给redis://@127.0.0.1:6379以及redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的所有master节点
* tfunction list会发给redis://@127.0.0.1:6379或者redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的任意一个master节点