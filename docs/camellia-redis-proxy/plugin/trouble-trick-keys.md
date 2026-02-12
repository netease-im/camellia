
## TroubleTrickKeysProxyPlugin

### 说明
* 用于临时屏蔽某些命令的指定方法的请求，会直接返回错误信息，而不是穿透到后端redis 

### 配置
```properties
proxy.plugin.list=troubleTrickKeysPlugin

#配置
#表示：针对key1和key2的ZREVRANGEBYSCORE方法，针对key3和key4的GET方法，会被拦截（直接返回错误信息）
trouble.trick.keys=ZREVRANGEBYSCORE:["key1","key2"];GET:["key3","key4"]

#配置（租户级别）
#表示：bid=2/bgroup=default路由配置下，针对key1和key2的ZRANGE方法，针对key3和key4的SMEMBERS方法，会被拦截（直接返回错误信息）
2.default.trouble.trick.keys=ZRANGE:["key1","key2"];SMEMBERS:["key3","key4"]
```
