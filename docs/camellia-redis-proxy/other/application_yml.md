
* application.yaml

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  plugins:
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  config:
    "k1": "v1"
    "k2": "v2"
```
