## DynamicIPCheckProxyPlugin

### Illustrate
* A plugin for ip black and white list restrictions on clients accessing proxy
* Supports blacklist mode, also supports whitelist mode, 
* Configuration supports dynamic change via `Camellia-dashboard`

### configuration
````properties
proxy.plugin.list=dynamicIpCheckerPlugin

#The interval of updating the plugin configuration, the default is 5 seconds
dynamic.ip.check.plugin.config.update.interval.seconds=5
#camellia-dashboard configuration
camellia.dashboard.url=http://127.0.0.1:8080
camellia.dashboard.headerMap={"api-key": "secretToken"}
````

### API management (Camellia-dashboard)
- The configuration of the plugin is stored in the database, and the configuration of the plugin can be modified through the API
- Check it out: [/permissions/ip-checkers APIs](http://localhost:8080/swagger-ui.html#!/permission-interface)
 ![img.png](ip-checker-api.png)
- APIs for managing plugin configurations:
  + [Find configurations](http://localhost:8080/swagger-ui.html#!/permission-interface/findIpCheckersUsingGET)
  + [Find a configuration by ID](http://localhost:8080/swagger-ui.html#!/permission-interface/findIpCheckerByIdUsingGET)
  + [Create a new configuration](http://localhost:8080/swagger-ui.html#!/permission-interface/CreateIpCheckerUsingPOST)
  + [Update a configuration](http://localhost:8080/swagger-ui.html#!/permission-interface/UpdateIpCheckerUsingPUT)
  + [Delete a configuration](http://localhost:8080/swagger-ui.html#!/permission-interface/DeleteIpCheckerUsingDELETE)
  + [Fetch all configurations](http://localhost:8080/swagger-ui.html#!/api%E6%8E%A5%E5%8F%A3/getIpCheckerListUsingGET)
- Sample response body:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "bid": 0,
    "bgroup": "0",
    "mode": "WHITE",
    "ipList": [
      "2.2.2.2",
      "5.5.5.5",
      "3.3.3.0/24",
      "6.6.0.0/16"
    ],
    "updateTime": 1668161492780,
    "createTime": 1668161492780
  }
}
```
**NOTE:** **bid = -1 , bgroup = "default"** means that the configuration is global, and the configuration is applied for all. 
If you want to configure a specific bid and bgroup, you need to create a new configuration, and it will override the global configuration.