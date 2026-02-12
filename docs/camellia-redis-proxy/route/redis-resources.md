
## 支持的redis后端类型

* 我们通过url的方式来描述后端redis服务器
* 支持普通单点redis、redis-sentinel、redis-cluster三种类型，也支持代理到多个无状态的proxy节点（如codis、twemproxy等）
* 此外，还支持将读命令透明的代理到redis-sentinel/redis-cluster的从节点
* 具体的url格式如下：

### 普通单点redis
不带tls
```
##有密码
redis://passwd@127.0.0.1:6379
##没有密码
redis://@127.0.0.1:6379
##有账号也有密码
redis://username:passwd@127.0.0.1:6379
##有账号也有密码且设置了db
redis://username:passwd@127.0.0.1:6379?db=1
```
带tls
```
##有密码
rediss://passwd@127.0.0.1:6379
##没有密码
rediss://@127.0.0.1:6379
##有账号也有密码
rediss://username:passwd@127.0.0.1:6379
##有账号也有密码且设置了db
rediss://username:passwd@127.0.0.1:6379?db=1
```

### redis-sentinel
不带tls
```
##有密码
redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##没有密码
redis-sentinel://@127.0.0.1:16379,127.0.0.1:16379/masterName
##有账号也有密码
redis-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##有账号也有密码，且设置了db
redis-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
redis-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?db=1&sentinelUserName=xxx&sentinelPassword=xxx
##sentinel带tls
redis-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?db=1&sentinelUserName=xxx&sentinelPassword=xxx&sentinelSSL=true
```
带tls
```
##有密码
rediss-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##没有密码
rediss-sentinel://@127.0.0.1:16379,127.0.0.1:16379/masterName
##有账号也有密码
rediss-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##有账号也有密码，且设置了db
rediss-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
rediss-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?db=1&sentinelUserName=xxx&sentinelPassword=xxx
```

### redis-cluster
不带tls
```
##有密码
redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
redis-cluster://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
redis-cluster://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
```
带tls
```
##有密码
rediss-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
rediss-cluster://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
rediss-cluster://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
```

### redis-sentinel-slaves
不带tls
```
##本类型的后端只能配置为读写分离模式下的读地址

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##有账号也有密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##有账号也有密码，且设置了db
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false&db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##有账号也有密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##有账号也有密码，且设置了db
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true&db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true&db=1&sentinelUserName=xxx&sentinelPassword=xxx
##sentinel带tls
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true&db=1&sentinelUserName=xxx&sentinelPassword=xxx&sentinelSSL=true

##redis-sentinel-slaves会自动感知：节点宕机、主从切换和节点扩容
```
带tls
```
##本类型的后端只能配置为读写分离模式下的读地址

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
rediss-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##没有密码
rediss-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##有账号也有密码
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##有账号也有密码，且设置了db
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false&db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
rediss-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##没有密码
rediss-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##有账号也有密码
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##有账号也有密码，且设置了db
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true&db=1
##有账号也有密码，且设置了db，且设置了sentinel的账号和密码
rediss-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true&db=1&sentinelUserName=xxx&sentinelPassword=xxx

##redis-sentinel-slaves会自动感知：节点宕机、主从切换和节点扩容
```

### redis-cluster-slaves
不带tls
```
##本类型的后端只能配置为读写分离模式下的读地址，如果配置为写地址，proxy不会报错，但是每次写请求都会产生一次重定向，性能会大大受影响

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##没有密码
redis-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##有账号也有密码
redis-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##没有密码
redis-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##有账号也有密码
redis-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true

##redis-cluster-slaves会自动感知：节点宕机、主从切换和节点扩容
```
带tls
```
##本类型的后端只能配置为读写分离模式下的读地址，如果配置为写地址，proxy不会报错，但是每次写请求都会产生一次重定向，性能会大大受影响

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
rediss-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##没有密码
rediss-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##有账号也有密码
rediss-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
rediss-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##没有密码
rediss-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##有账号也有密码
rediss-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true

##redis-cluster-slaves会自动感知：节点宕机、主从切换和节点扩容
```

### redis-proxies
不带tls
```
##本类型主要是为了代理到多个无状态的proxy节点，如codis-proxy、twemproxy等，camellia-redis-proxy会从配置的多个node中随机选择一个进行转发
##当后端的proxy node有宕机时，camellia-redis-proxy会动态剔除相关节点，如果节点恢复了则会动态加回

##有密码
redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
redis-proxies://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
redis-proxies://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码，且设置了db
redis-proxies://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379?db=1
```
带tls
```
##本类型主要是为了代理到多个无状态的proxy节点，如codis-proxy、twemproxy等，camellia-redis-proxy会从配置的多个node中随机选择一个进行转发
##当后端的proxy node有宕机时，camellia-redis-proxy会动态剔除相关节点，如果节点恢复了则会动态加回

##有密码
rediss-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
rediss-proxies://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
rediss-proxies://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码，且设置了db
rediss-proxies://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379?db=1
```

### redis-proxies-discovery
不带tls
```
##本类型主要是为了代理到多个无状态的proxy节点，如codis-proxy、twemproxy等
##与redis-proxies的区别在于，本类型支持从注册中心获取无状态proxy节点的列表，并动态增减相关节点
##为了实现和注册中心进行交互，你需要先实现ProxyDiscoveryFactory接口（支持全类名配置，也支持spring直接注入相关实现类）
##本类型通过proxyName来标识proxy节点列表，并通过proxyName去ProxyDiscoveryFactory获取实际的proxy节点列表

##有密码
redis-proxies-discovery://passwd@proxyName
##没有密码
redis-proxies-discovery://@proxyName
##有密码且有账号
redis-proxies-discovery://username:passwd@proxyName
##有密码且有账号，且设置了db
redis-proxies-discovery://username:passwd@proxyName?db=1
```
带tls
```
##本类型主要是为了代理到多个无状态的proxy节点，如codis-proxy、twemproxy等
##与redis-proxies的区别在于，本类型支持从注册中心获取无状态proxy节点的列表，并动态增减相关节点
##为了实现和注册中心进行交互，你需要先实现ProxyDiscoveryFactory接口（支持全类名配置，也支持spring直接注入相关实现类）
##本类型通过proxyName来标识proxy节点列表，并通过proxyName去ProxyDiscoveryFactory获取实际的proxy节点列表

##有密码
rediss-proxies-discovery://passwd@proxyName
##没有密码
rediss-proxies-discovery://@proxyName
##有密码且有账号
rediss-proxies-discovery://username:passwd@proxyName
##有密码且有账号，且设置了db
rediss-proxies-discovery://username:passwd@proxyName?db=1
```

### redis-uds(unix-domain-socket)
```
##没有密码
redis-uds://@/tmp/redis.sock
##有密码
redis-uds://password@/tmp/redis.sock
##有密码且有账号
redis-uds://user:password@/tmp/redis.sock?db=0
```