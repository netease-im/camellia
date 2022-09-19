
## 关于lua

* camellia-redis-proxy支持eval/evalsha/eval_ro/evalsha_ro，当代理到redis-cluster或者自定义分片时，proxy会检查每个key是否指向同一个后端node或者slot
* proxy也支持script命令，支持load/flush/exists参数，当使用script load/flush时，proxy会把脚本发往所有的写后端，当使用script exists时，会发往所有读后端

### 例子一
```
redis-cluster://@127.0.0.1:6379,127.0.0.1:6380
```
* script load/flush会发给上述redis-cluster的所有master节点，并返回第一个master的回包
* script exists会发给上述redis-cluster的所有master节点，并汇总每个master的回包之和

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