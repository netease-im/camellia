# 支持的后端redis类型
我们通过url的方式来描述后端redis服务器，支持普通单点redis、redis-sentinel、redis-cluster三种类型，此外可以支持配置读请求指向redis-sentinel的从节点，具体的url格式如下：

* 普通单点redis
```
##有密码
redis://passwd@127.0.0.1:6379
##没有密码
redis://@127.0.0.1:6379
```

* redis-sentinel
```
##有密码
redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##没有密码
redis-sentinel://@127.0.0.1:16379,127.0.0.1:16379/masterName
```

* redis-cluster
```
##有密码
redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
redis-cluster://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
```

* redis-sentinel-slaves
```
##本类型的后端只能配置为读写分离模式下的读地址

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true

##redis-sentinel-slaves会自动感知：节点宕机、主从切换和节点扩容
```


# 支持配置方式

* 简单读写
```
redis://password@127.0.0.1:6379
```

* 读写分离
```
{
  "type": "simple",
  "operation": {
    "read": "redis://read@127.0.0.1:6379",
    "type": "rw_separate",
    "write": "redis://write@127.0.0.1:6380"
  }
}
```

* 读写分离
```
{
  "type": "simple",
  "operation": {
    "read": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd123@127.0.0.1:26379/master"
  }
}
```

* 双写单读
```
{
  "type": "simple",
  "operation": {
    "read": "redis://read@127.0.0.1:6379",
    "type": "rw_separate",
    "write": {
      "resources": [
        "redis://read@127.0.0.1:6379",
        "redis://write@127.0.0.1:6380"
      ],
      "type": "multi"
    }
  }
}
```

* 分片
```
{
  "type": "shading",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```

* 分片+双写
```
{
  "type": "shading",
  "operation": {
    "operationMap": {
      "4": {
        "read": "redis://password1@127.0.0.1:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```