# 目录
* 关于scan的说明
* 关于lua的说明
* 关于使用redis-shake进行数据迁移的说明

## 关于scan

* camellia-redis-proxy支持scan命令，不管后端是代理的的redis-standalone还是redis-sentinel还是redis-cluster
* 当proxy的路由配置是自定义分片时（比如2个redis-cluster集群组成一个逻辑上的大集群，或者多组redis-sentinel主从组成一个大集群），scan命令仍然是有效的，proxy会按照顺序依次扫描每个读后端
* 当proxy的后端是redis-cluster或者定义分片时，每次scan返回的cursor从数值上看可能很大，这是因为cursor上记录了后端node的index，调用方不需要关心

### 例子一
```
redis-cluster://@127.0.0.1:6379,127.0.0.1:6380
```
* scan会扫描redis-cluster://@127.0.0.1:6379,127.0.0.1:6380的所有master节点

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
* scan会扫描redis://@127.0.0.1:6379

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
* scan会扫描redis://@127.0.0.1:6379以及redis-cluster://@127.0.0.1:6378,127.0.0.1:6377的所有master节点


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

## 关于使用redis-shake进行数据迁移的说明
* camellia-redis-proxy实现了标准的redis协议，包括scan命令，因此你可以从redis-standalone/redis-sentinel/redis-cluster或者twemproxy/codis迁移数据到camellia，当然也可以反向进行
* camellia-redis-proxy支持使用redis-shake的sync和rump两种模式进行数据的迁入，sync模式支持存量数据和增量数据的迁移，rump模式仅支持存量数据迁移
* 因为redis-shake在迁移之前会通过info命令校验redis版本，1.0.50以及之前的camellia-redis-proxy版本的info命令回包使用\n进行换行，之后的版本使用\r\n进行换行，而redis-shake默认使用\r\n来识别info命令回包，因此请使用1.0.51及之后的版本来对接redis-shake
* redis-shake的下载地址：https://github.com/alibaba/RedisShake

### 典型场景一
* 背景：从redis-cluster迁移到多套redis-sentinel
* 源集群是一套redis-cluster
* 目标集群是：camellia-redis-proxy + redis-sentinel集群*N

### 典型场景二
* 背景：从1套redis-cluster迁移到多套redis-cluster组成自定义分片的逻辑大集群
* 源集群是一套redis-cluster
* 目标集群是：camellia-redis-proxy + redis-cluster集群*N

