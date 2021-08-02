### 支持的后端redis类型
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

##不读master，此时camellia会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false

##读master，此时camellia会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true

##redis-sentinel-slaves会自动感知：节点宕机、主从切换和节点扩容
```
