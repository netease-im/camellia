# camellia-dashboard-samples


## Introduction
This is sample code of Camellia Dashboard with Api-key authentication is optional.

## Configurations
Sample **application.yml**
- database:
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC
    username: root
    password: root
    hikari:
      minimum-idle: 5
      idle-timeout: 180000
      maximum-pool-size: 10
      connection-timeout: 30000
      connection-test-query: SELECT 1
```
- camellia dashboard:
```yaml

camellia-dashboard:
  local-cache-expire-seconds: 5
  stats-expire-seconds: 180
  stats-key-expire-hours: 6
  security: # Config security
    enabled: false # Enable api-key authentication. Default is false
    secure-token: secretToken # If enabled is true, secret token for security -> header must contain "api-key: secretToken"
```
- redis
```yaml
camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379
  redis-conf:
    jedis:
      max-idle: 8
      min-idle: 0
      max-active: 8
      max-wait-millis: 2000
      timeout: 2000
```

### Note:
- If `camellia-dashboard.security.enabled` is true, you must add `api-key` in header of request. 
- And if `camellia-redis-proxy` set `camellia-redis-proxy.transpond.type` is remote, you must add `header-map` like this:
```yaml
camellia-redis-proxy:
  # ...
  transpond:
    type: remote
    remote:
      url: http://127.0.0.1:8080 #camellia-dashboard's address
      check-interval-millis: 5000 # Polling period to camellia-dashboard
      dynamic: true # indicates that multiple sets of configurations are supported, the default is true
      header-map: # custom header map for request to camellia-dashboard (optional)
        api-key: secretToken # header name: api-key, header value: secretToken 
```

## Maven dependencies
````
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-dashboard-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
````