## 把proxy当做一个监控redis集群状态的平台（通过http接口暴露）
你可以使用http接口去请求proxy，并把需要探测的redis地址传递给proxy，proxy会以json格式返回目标redis集群的信息  

### 请求示例  
```
http://127.0.0.1:16379/detect?url=redis-cluster://@127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002
```
支持针对redis-standalone、redis-sentinel、redis-cluster三种redis集群的监控

### 返回示例
包括如下信息：
* 集群状态
* 集群内存情况（总内存、已用内存）
* 集群key数量（总key数、带ttl的key数量）
* 集群qps
* 节点信息，包括各个节点的内存、key数量、tps等信息
```json
{
    "info":
      [
        {
          "used_memory_human": "609.05M",
          "maxmemory_hum": "12.00G",
          "qps": 361,
          "expire_key_count": 196393,
          "maxmemory": 12884901888,
          "used_memory": 638635736,
          "key_count": 200906,
          "type": "cluster",
          "memory_used_rate": 0.04956465649108092,
          "memory_used_rate_human": "4.96%",
          "url": "redis-cluster://@127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002",
          "status": "ok"
        }
    ],
    "nodeInfo":
    [
        {
            "used_memory_human": "152.41M",
            "role": "master",
            "connected_slaves": 1,
            "slave0": "ip=127.0.0.1,port=7003,state=online,offset=10001397878,lag=1",
            "expire_key_count": 49002,
            "maxmemory": 3221225472,
            "maxmemory_human": "3.00G",
            "node_url": "@127.0.0.1:7000",
            "used_memory": 159808936,
            "key_count": 50120,
            "memory_used_rate_human": "4.96%",
            "avg_ttl": 141492103,
            "redis_version": "4.0.9",
            "hz": 10,
            "qps": 98,
            "memory_used_rate": 0.04961122324069341,
            "maxmemory_policy": "noeviction"
        },
        {
            "used_memory_human": "152.37M",
            "role": "master",
            "connected_slaves": 1,
            "slave0": "ip=127.0.0.1,port=7004,state=online,offset=11558147522,lag=1",
            "expire_key_count": 49186,
            "maxmemory": 3221225472,
            "maxmemory_human": "3.00G",
            "node_url": "@127.0.0.1:7002",
            "used_memory": 159768192,
            "key_count": 50349,
            "memory_used_rate_human": "4.96%",
            "avg_ttl": 150670909,
            "redis_version": "4.0.9",
            "hz": 10,
            "qps": 97,
            "memory_used_rate": 0.0495985746383667,
            "maxmemory_policy": "noeviction"
        },
        {
            "used_memory_human": "152.13M",
            "role": "master",
            "connected_slaves": 1,
            "slave0": "ip=127.0.0.1,port=7005,state=online,offset=12976183697,lag=1",
            "expire_key_count": 49106,
            "maxmemory": 3221225472,
            "maxmemory_human": "3.00G",
            "node_url": "@127.0.0.1:7003",
            "used_memory": 159517808,
            "key_count": 50208,
            "memory_used_rate_human": "4.95%",
            "avg_ttl": 100739796,
            "redis_version": "4.0.9",
            "hz": 10,
            "qps": 70,
            "memory_used_rate": 0.049520845214525856,
            "maxmemory_policy": "noeviction"
        },
        {
            "used_memory_human": "152.15M",
            "role": "master",
            "connected_slaves": 1,
            "slave0": "ip=10.189.7.217,port=6380,state=online,offset=9792844650,lag=1",
            "expire_key_count": 49099,
            "maxmemory": 3221225472,
            "maxmemory_human": "3.00G",
            "node_url": "@127.0.0.1:7003",
            "used_memory": 159540800,
            "key_count": 50229,
            "memory_used_rate_human": "4.95%",
            "avg_ttl": 98326773,
            "redis_version": "4.0.9",
            "hz": 10,
            "qps": 96,
            "memory_used_rate": 0.04952798287073771,
            "maxmemory_policy": "noeviction"
        }
    ],
    "otherInfo":
    [
        {
          "value": "8",
          "key": "cluster_known_nodes"
        },
        {
          "value": "4",
          "key": "cluster_size"
        },
        {
          "value": "0",
          "key": "cluster_slots_pfail"
        },
        {
          "value": "ok",
          "key": "cluster_state"
        },
        {
          "value": "16384",
          "key": "cluster_slots_assigned"
        },
        {
          "value": "16384",
          "key": "cluster_slots_ok"
        },
        {
          "value": "0",
          "key": "cluster_slots_fail"
        },
        {
          "value": "yes",
          "key": "cluster_safety"
        }
    ]
}
```

### 备注
当把proxy作为一个监控平台使用时，proxy启动时可能不知道要访问后端redis地址，为了避免proxy启动失败，可以配置启动时不进行预热（preheat参数设置为false），如下：
```properties
upstream.preheat.enable=false
```
```