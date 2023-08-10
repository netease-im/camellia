
## 多读场景下自动摘除故障读节点

* proxy支持配置多个读的redis资源
* proxy会定期检查后端的read-resource是否可用（默认5s），如果不可用，则会临时摘除


### 配置多读的示例
```json
{
  "type": "simple",
  "operation": {
    "read": {
      "resources": [
        "redis://password1@127.0.0.1:6379",
        "redis://password2@127.0.0.1:6380"
      ],
      "type": "random"
    },
    "type": "rw_separate",
    "write": "redis://passwd1@127.0.0.1:6379"
  }
}
```

### 判断resource是否可用的判断方法

```
## 是否可连接、是否可正常登录
redis://passwd@127.0.0.1:6379

## 主节点是否可连接、是否可正常登录
redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName

## 是否所有主节点可连接、是否可正常登录
redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379

## 主节点/从节点，是否有任意一个节点可连接，可正常登录
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true

## 是否所主节点/从节点都不可连接，不可正常登录
redis-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true

## 是否所有节点都不可连接，不可正常登录
redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379

## 是否所有节点都不可连接，不可正常登录
redis-proxies-discovery://passwd@proxyName
```