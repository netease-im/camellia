
## ReadOnlyPlugin

### 说明
* 用于拦截客户端的写请求

### 配置
```properties
proxy.plugin.list=readOnlyPlugin

#全局，默认true
read.only.plugin.enable=true

#租户级别（优先级高于全局），默认true
{bid}.{bgroup}.read.only.plugin.enable=true
```
