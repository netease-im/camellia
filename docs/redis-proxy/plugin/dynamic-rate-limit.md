## DynamicRateLimitProxyPlugin

### Illustrate
* It is used to control the client's request tps, if it exceeds, it will return an error directly instead of penetrating to the backend redis
* The configuration related to rate control is hosted by `camellia-dashboard`, which supports dynamic configuration changes

### Enable method
````yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, the default is 16379, if set to -16379, there will be a random available port, if set to 0, the console will not be started
  password: pass123 #proxy password, if a custom client-auth-provider-class-name is set, the password parameter is invalid
  monitor-enable: true #Whether to enable monitoring
  monitor-interval-seconds: 60 #Monitor callback interval
  plugins: #Use yml to configure plugins, built-in plugins can be enabled directly using aliases, custom plugins need to configure the full class name
    - dynamicRateLimitPlugin
  transpond:
    type: remote
    remote:
      url: http://127.0.0.1:8080 #camellia-dashboard's address
      check-interval-millis: 5000 # Polling period to camellia-dashboard
      dynamic: true # indicates that multiple sets of configurations are supported, the default is true
      header-map:
        api-key: secretToken
````

### Dynamic configuration (camellia-redis-proxy.properties)
- The interval of updating the plugin configuration
````properties
#The interval of updating the plugin configuration, the default is 5 seconds
dynamic.plugin.config.update.interval.seconds=5
#camellia-dashboard configuration
camellia.dashboard.url=http://127.0.0.1:8080
camellia.dashboard.headerMap={"api-key": "secretToken"}
````

### API management (Camellia-dashboard)
- The configuration of the plugin is stored in the database, and the configuration of the plugin can be modified through the API
- Check it out: [/permissions/rate-limit APIs](http://localhost:8080/swagger-ui.html#!/permission-interface)
 ![img.png](rate-limit-api.png)
- APIs for managing plugin configurations:
  + [Search configurations]()
  + [Find a configuration by ID]()
  + [Create a new configuration]()
  + [Update a configuration]()
  + [Delete a configuration]()
  + [Fetch all configurations]()
- Sample response body:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 6,
    "bid": 3,
    "bgroup": "tass",
    "checkMillis": 100,
    "maxCount": 10000,
    "createTime": 1671699743917,
    "updateTime": 1671701005134
  }
}
```
**NOTE:** **bid = -1 , bgroup = "default"** means that the configuration is global, and the configuration is applied for all. 
If you want to configure a specific bid and bgroup, you need to create a new configuration, and it will override the global configuration.