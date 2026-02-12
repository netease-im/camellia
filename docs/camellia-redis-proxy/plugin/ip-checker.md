
## IPCheckProxyPlugin

### 说明
* 一个用于对访问proxy的客户端进行ip黑白名单限制的plugin
* 支持黑名单模式，也支持白名单模式，配置支持动态变更

### 配置
```properties
proxy.plugin.list=ipCheckerPlugin

#黑名单示例（支持ip，也支持网段，逗号分隔）：
#ip.check.mode=1
#ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#白名单示例（支持ip，也支持网段，逗号分隔）：
#ip.check.mode=2
#ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#根据bid/bgroup设置不同的策略：
#黑名单示例（表示bid=1,bgroup=default的黑名单配置）：
#1.default.ip.check.mode=1
#1.default.ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#白名单示例（表示bid=1,bgroup=default的白名单配置）：
#1.default.ip.check.mode=2
#1.default.ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```