
### 动态配置
如果你希望你的proxy的路由配置可以动态变更，比如本来路由到redisA，然后动态的切换成redisB，那么你需要一个额外的配置文件，并且在application.yml中引用，如下：
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
      type: complex
      dynamic: true
      check-interval-millis: 3000
      json-file: resource-table.json
```
上面的配置表示：
* proxy的路由转发规则来自于一个配置文件（因为在文件里可以自定以配置双写、分片以及各种组合等，所以叫复杂的complex），叫resource-table.json
* dynamic=true表示配置是动态更新的，此时proxy会定时检查resource-table.json文件是否有变更（默认5s间隔，上图配置了3s），如果有变更，则会重新reload
* proxy默认会优先去classpath下寻找名称叫resource-table.json的配置文件
* 此外，你也可以直接配置一个绝对路径，proxy会自动识别这种情况，如下：
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
      type: complex
      dynamic: true
      check-interval-millis: 3000
      json-file: /home/xxx/resource-table.json
```
