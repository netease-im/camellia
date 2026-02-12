
## CommandDisableProxyPlugin

### 说明
* 一个用于屏蔽proxy上某些命令的plugin

### 动态配置开关（camellia-redis-proxy.properties）
```properties
proxy.plugin.list=commandDisablePlugin

#被屏蔽的命令列表（忽略大小写）
disabled.commands=GET,SET,EVAL
```