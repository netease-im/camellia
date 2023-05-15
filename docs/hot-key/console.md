
## console
* camellia-hot-key-server内置了一个简单的http服务器
* 可以提供优雅上下线和获取监控数据的功能


### 接口列表
* /status  
获取服务器状态  
* /online   
服务器上线   
* /offline    
服务器下线
* /check  
检查服务器   
* /monitor  
获取服务器监控数据（JSON格式）    
* /topN?namespace=xxx  
指定namespace获取topN数据  
* /prometheus
获取服务器监控数据（以prometheus的格式）  
* /reload
立即reload热key规则配置
* /custom
自定义接口，默认什么也不做  


#### /monitor的返回

* 具体字段参考HotKeyServerStats类

```json
{
    "hotKeyInfoStats":
    [
        {
            "namespace": "test",
            "count": 472,
            "action": "QUERY",
            "rule": "{\n\t\"name\":\"default\",\n\t\"checkMillis\":1000,\n\t\"type\":\"match_all\",\n\t\"checkThreshold\":100\n}",
            "key": "def"
        }
    ],
    "trafficTotalStats":
    [
        {
            "qps": 136.33333333333334,
            "count": 8180
        }
    ],
    "queueStats":
    [
        {
            "pendingSize": 0,
            "id": "0"
        },
        {
            "pendingSize": 0,
            "id": "1"
        }
    ],
    "trafficStats":
    [
        {
            "namespace": "test",
            "count": 7903,
            "type": "NORMAL"
        },
        {
            "namespace": "test",
            "count": 277,
            "type": "HOT"
        }
    ],
    "info":
    [
        {
            "count": 8180,
            "workThread": 2,
            "monitorIntervalSeconds": 60,
            "connectCount": 1,
            "applicationName": "camellia-hot-key-server"
        }
    ]
}
```


#### /topN?namespace=test的回包

* 具体字段参考TopNStatsResult类

```json
{
  "info":
  [
    {
      "namespace": "sql_hot_key",
      "statsTime": "2023-05-15#15:09:00"
    }
  ],
  "stats":
  [
    {
      "total": 141548,
      "action": "QUERY",
      "id": "0",
      "key": "abc",
      "maxQps": 1814
    },
    {
      "total": 90132,
      "action": "QUERY",
      "id": "1",
      "key": "def",
      "maxQps": 1180
    },
    {
      "total": 1,
      "action": "QUERY",
      "id": "2",
      "key": "feefc9d3-f6fa-4394-884a-1c95ca7e8693",
      "maxQps": 0
    }
  ]
}
```