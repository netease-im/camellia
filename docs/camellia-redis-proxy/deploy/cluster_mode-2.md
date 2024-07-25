
## cluster-mode-2

* 中心化的cluster-mode方案，首先选举一个leader节点，并且由leader节点来完成节点的扩缩容
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
    "cluster.mode.consensus.leader.selector.class.name": "com.netease.nim.camellia.redis.proxy.cluster.provider.RedisConsensusLeaderSelector"
    "redis.consensus.leader.selector.redis.url": "redis://@127.0.0.1:6379"
    "redis.consensus.leader.selector.redis.key": "xxxxx"
  transpond:
    type: local
    local:
      type: simple
      resource: redis://@127.0.0.1:6379
```     

### cluster.mode.consensus.leader.selector.class.name

* 用于选主的实现类，当前支持基于redis的leader-selector（默认）
* 也可以自己基于zk/etcd/nacos实现，或者proxy集群内部自我实现选举

#### redis.consensus.leader.selector.redis.url

* 用于选主的redis地址

#### redis.consensus.leader.selector.redis.key

* 用于选主的redis-key

### 流程

* 选举出leader
* follower定期发送心跳给leader，确认slot-map是否发生变化，以及告知leader新节点加入
* leader定期发送心跳给follower，确认follower是否健康
* 当leader感知到节点上下线，生成一份新的slot-map，并广播给所有follower
* 当leader宕机后，会重新选一个leader

