
## camellia-redis-proxy的gc策略

在redis中，可以给key添加过期时间，而通过kv存储模拟redis数据结构后，一个key被拆分为多个key  
此外kv存储也不一定提供ttl的功能，因此需要提供一个gc机制来回收kv存储中的数据  
kv存储中分为key-meta和sub-key两部分，因此gc也需要对这两部分别进行处理  

### key-meta

* 如果kv存储支持ttl，则直接设置ttl，则不需要上层进行处理
* 如果kv存储不支持ttl，则需要定期扫描相关前缀的key，检查key-meta中的expire-time字段，如果发现已经过期了，则需要删除
* 因为在删除key-meta时，可能业务刚好进行了覆盖写，可能误删，因此需要做并发控制
* 如果kv存储支持cas语义的check-and-delete，则可以直接安全的删除
* 如果kv存储不支持cas语义的check-and-delete，则gc线程在扫描到过期的key-meta后，会根据redis-cluster协议发送给指定proxy处理，从而和业务线程中对相关key的处理串行，避免并发
* 幸运的，hbase和obkv支持check-and-delete，tikv支持ttl


### sub-key

* gc线程会定时扫描相关前缀的key，并检查sub-key依赖的key-meta是否还存在
* 如果key-meta还在有效期内，则跳过
* 如果key-meta已经过期或者不存在，则删除sub-key即可
* 因为sub-key是一个多版本的设计，可以在gc线程安全的删除，不需要转发到业务线程中进行删除


### 配置

```properties
#是否开启gc
#假设业务访问的是3个proxy节点组成的伪redis-cluster集群，建议在这三个proxy节点上关闭gc
#额外部署一个proxy节点开启gc，且这个proxy节点不加入前面的集群
kv.gc.schedule.enable=false
#gc的频率，默认24h执行一次，按需设置（后续会提供更丰富的时间选择）
kv.gc.schedule.interval.seconds=86400
#如果kv存储既不支持ttl，又不支持check-and-delete，则key-meta的操作需要路由回业务proxy节点进行串行化操作，这里需要配上业务proxy组成的redis-cluster地址串
#底层是hbase、obkv、tikv时，均不需要设置本配置
kv.gc.clean.expired.key.meta.target.proxy.cluster.url=redis-cluster://passwd@127.0.0.1:6380,127.0.0.2:6380
```