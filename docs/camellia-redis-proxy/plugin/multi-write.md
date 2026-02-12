
## MultiWriteProxyPlugin

### 说明
* 用于另外的进行自定义双写策略（key）的plugin，如：某些key需要双写，某些key不需要双写，某些key双写到redisA，某些key双写到redisB  
* 备注一：只有proxy完整支持的命令集合中的写命令支持本模式，对于那些限制性支持的命令，如阻塞型命令、发布订阅命令、一次操作多个key的命令（SMOVE、EVAL、ZDIFFSTORE、MSETNX）等，是不支持使用MultiWriteProxyPlugin来双写的，  
* 备注二：redis事务包裹的写命令使用MultiWriteProxyPlugin双写时可能主路由执行失败而双写成功


### 配置
```properties
proxy.plugin.list=multiWritePlugin

#一个用于判断双写策略的函数方法（实现MultiWriteFunc接口即可）
multi.write.func.class.name=com.xxx.xxx.CustomMultiWriteFunc
```

### 内置的KeyPrefixMultiWriteFunc

根据key的前缀选择双写地址

```properties
multi.write.func.class.name=com.netease.nim.camellia.redis.proxy.plugin.misc.KeyPrefixMultiWriteFunc
```
配置方法
```properties
## 默认配置
key.prefix.multi.write.func.config={"abc": ["redis://@127.0.0.1:6379"], "def": ["redis://@127.0.0.1:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"]}
## bid/bgroup级别的配置
1.default.key.prefix.multi.write.func.config={"abc": ["redis://@127.0.0.1:6379"], "def": ["redis://@127.0.0.1:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"]}
```