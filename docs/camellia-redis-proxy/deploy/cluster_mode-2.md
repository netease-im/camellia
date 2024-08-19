
## cluster-mode-2

* 中心化的cluster-mode方案，首先选举一个leader节点，并且由leader节点来完成节点的扩缩容

### 应用场景

* 期望使用redis-proxy，客户端支持redis-cluster协议
* 可以让proxy自己组成高可用集群（即能平滑扩缩容、节点宕机能自动fail-over）
* 相比于cluster-mode-1，可以不需要提前知道节点ip
* 相比于cluster-mode-1，节点扩缩容时会尽可能减少slot的变化（因此适合proxy后端是kv存储时，提升lru-cache的有效性）
* 在k8s环境下，客户端可以连接service域名，连接到任意一个节点获取slot信息，随后直连pod，性能上因为不依赖service底层的负载均衡机制，流量更均衡，性能损耗更小

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

#### cluster.mode.consensus.leader.selector.class.name

* 用于选主的实现类，当前支持基于redis的leader-selector（默认）
* 也可以自己基于zk/etcd/nacos实现，或者proxy集群内部自我实现选举

#### redis.consensus.leader.selector.redis.url

* 用于选主的redis地址

#### redis.consensus.leader.selector.redis.key

* 用于选主的redis-key
* 不同的集群，需要不一样
* 同一个集群内的不同节点，要求一样
* 建议使用一个随机字符串

### 流程

* 选举出leader
* follower定期发送心跳给leader，确认slot-map是否发生变化，以及告知leader新节点加入
* leader定期发送心跳给follower，确认follower是否健康
* 当leader感知到节点上下线，生成一份新的slot-map，并广播给所有follower
* 当leader宕机后，会重新选一个leader

### 在k8s中部署本模式的注意事项

* 如果客户端是jedis，不建议使用本模式（因为jedis每次刷新路由都是用的`cluster slots`命令返回的结果中的ip去发起，而不会用配置的初始svc域名
* 务必给proxy增加密码，且不同的集群密码需要不一样，避免发布后pod-ip被不同集群复用，导致访问互相串了
* 如果是其他客户端，务必测试以下场景：
  * proxy依次启动后，使用`cluster slots`或者`cluster nodes`命令访问，确认返回正常
  * 客户端使用redis-cluster模式去访问svc域名，验证正常
  * 客户端在启动并且访问正常后，不再请求，但是保持长链接，此时依次重启所有pod，从而所有pod的ip都发生了变化，随后再次验证客户端是否可以正常发送请求
  * 客户端在启动并且访问正常后，持续请求，此时依次重启所有pod，从而所有pod的ip都发生了变化，期间观察客户端使用有异常，或者异常是否持续
