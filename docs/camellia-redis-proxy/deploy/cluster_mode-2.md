
## cluster-mode-2

* 中心化的cluster-mode方案，基于外部系统选择一个leader节点，由leader节点来完成节点的扩缩容
* 相比方案一，在扩缩容时，本方案会尽可能的减少slot的变化
* 相比方案一，可以不需要提前知道节点的ip

### 配置

```yaml
server:
  port: 6381
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16378
  password: pass123
  cluster-mode-enable: true
  cport: 16380 #cluster-mode下的心跳端口，默认是proxy端口+10000
  cluster-mode-provider-class-name: com.netease.nim.camellia.redis.proxy.cluster.provider.ConsensusProxyClusterModeProvider
  config:
    "redis.consensus.leader.selector.redis.url": "redis://@127.0.0.1:6379"
    "redis.consensus.leader.selector.redis.key": "xxxxx"
  transpond:
    type: local
    local:
      type: simple
      resource: redis://@127.0.0.1:6379
```     

#### redis.consensus.leader.selector.redis.url

用于选主的redis地址

#### redis.consensus.leader.selector.redis.key

用于选主的redis-key

### 流程

* 选主
* follower定期发送心跳给leader
* leader定期发送心跳给follower
* 当leader感知到节点上下线，生成一份新的slot-map，并广播给所有follower
* 当leader宕机后，会重新选一个leader

