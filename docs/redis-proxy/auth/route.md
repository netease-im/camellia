## 路由配置
路由配置表示了camellia-redis-proxy在收到客户端的redis命令之后的转发规则

## 大纲
* 最简单的示例
* 支持的后端redis类型
* 动态配置和复杂配置（读写分离、分片等）
* 多租户支持
* 使用camellia-dashboard管理多租户动态路由
* 集成ProxyRouteConfUpdater自定义管理多租户动态路由

### 最简单的示例
在application.yml里配置如下信息：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```
上面的配置表示proxy的端口=6380，proxy的密码=pass123，代理到后端redis-cluster集群，地址串=127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377

### 支持的后端redis类型
具体见：[redis-resources](redis-resources.md)

### 动态配置(单租户)
具体见：[dynamic-conf](dynamic-conf.md)

### 如何定义一个复杂配置（读写分离、分片等）
具体见：[complex](complex.md)

### 多租户支持
具体见：[tenancy](tenancy.md)

### 使用camellia-dashboard管理多租户动态路由
具体见：[dashboard](dashboard.md)

### 使用ProxyRouteConfUpdater自定义管理多租户动态路由
具体见：[route-conf-updater](route-conf-updater.md)

