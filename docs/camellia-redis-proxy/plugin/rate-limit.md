
## RateLimitProxyPlugin

### 说明
* 用于控制客户端的请求tps，超过了会直接返回错误，而不是穿透到后端redis


### 配置
```properties
proxy.plugin.list=rateLimitPlugin

##检查周期
rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
rate.limit.max.count=100000

#bid/bgroup级别的速率控制（下面的例子表示bid=1，bgroup=default的请求，最多允许1000ms内10w次请求，超过会返回错误）
##检查周期
1.default.rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
1.default.rate.limit.max.count=100000

#bid/bgroup级别的默认速率控制
##检查周期
default.default.rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
default.default.rate.limit.max.count=100000
```
