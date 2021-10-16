## 监控数据的获取
* 你可以自己通过实现各项监控功能对外暴露的callback来获取到监控数据
* 此外你也可以请求proxy的console（一个简单的http-server）来获取，默认端口是16379，具体接口是http://127.0.0.1:16379/monitor   
* /monitor获取的监控数据的刷新周期取决于application.yml里的monitor-interval-seconds配置  
* /monitor获取到的监控数据是一个json，如[示例](monitor.json)
## monitor.json字段含义解析
```
{
  "connectStats": [
    {
      "connect": 53  //客户端连接数，虽然是一个数组，但是其实只有一个
    }
  ],
  "countStats": [
    {
      "count": 422214,  //总请求量
      "totalReadCount": 207582,  //读请求量
      "totalWriteCount": 214632  //写请求量
    }
  ],
  "qpsStats": [
    {
      "qps": 7036.9,   //总qps
      "writeQps": 3577.2,  //写qps
      "readQps": 3459.7  //读qps
    }
  ],
  "total": [ //各命令的请求量、qps
    {
      "qps": 2389.483333333333, //qps
      "count": 143369,  //请求量
      "command": "hgetall"  //命令
    },
    {
      "qps": 0.03333333333333333,
      "count": 2,
      "command": "auth"
    }
  ],
  "bidbgroup": [  //各业务的请求量、qps，按bid/bgroup来划分业务
    {
      "qps": 0.05,  //qps
      "bgroup": "default",  //bgroup
      "count": 3,  //请求量
      "bid": "35"  //bid
    },
    {
      "qps": 0.06666666666666667,
      "bgroup": "default",
      "count": 4,
      "bid": "default"
    }
  ],
  "detail": [ //各业务下各个命令的请求量、qps
    {
      "qps": 180.18333333333334, //qps
      "bgroup": "default", //bgroup
      "count": 10811,    //请求量
      "bid": "3",        //bid
      "command": "hget"  //命令
    },
    {
      "qps": 0.016666666666666666,
      "bgroup": "default",
      "count": 1,
      "bid": "35",
      "command": "zrangebyscore"
    }
  ],
  "spendStats": [ //耗时监控
    {
      "maxSpendMs": 16.464333,  //最大rt
      "count": 143565,  //请求数
      "avgSpendMs": 1.1236299562985408,  //平均rt
      "command": "hgetall"  //命令
    },
    {
      "maxSpendMs": 0.041476,
      "count": 206,
      "avgSpendMs": 0.0021440485436893205,
      "command": "ping"
    }
  ],
  "bidBgroupSpendStats": [//按照业务统计的耗时监控
    {
      "bgroup": "default",  
      "maxSpendMs": 7.929259,
      "count": 263,
      "bid": 9,
      "avgSpendMs": 0.7394532509505704,
      "command": "incrby"
    },
    {
      "bgroup": "default",
      "maxSpendMs": 1.668735,
      "count": 21,
      "bid": 9,
      "avgSpendMs": 0.7418927142857142,
      "command": "hget"
    }
  ],
  "failStats": [ //失败监控
    {
      "reason": "xxx",  //原因
      "count": 2  //数量
    }
  ],
  "slowCommandStats": [ //慢查询监控，
    {
      "bgroup": "default",  //bgroup
      "bid": "1",   //bid，如果没有使用camellia-dashboard，而是使用了local配置，则bid=default/bgroup=default
      "command": "mget",  //命令
      "keys": "k1,k2,k3",  //涉及的key列表，逗号分隔
      "spendMillis": 10200,  //耗时
      "thresholdMillis":1000  //慢查询的阈值
    },
    {
      "bgroup": "default",  
      "bid": "1",
      "command": "hgetall",
      "keys": "kkk",
      "spendMillis": 10200,
      "thresholdMillis":1000
    }],
  "hotKeyStats": [ //热key监控
    {
      "times": 1,  //超过阈值的次数
      "avg": 101.0,  //超过阈值的N个周期中，所有周期的平均请求量
      "max": 101,  //超过阈值的N个周期中，请求量最大的那个周期的请求数
      "bgroup": "default", //bgroup
      "count": 101, //超过阈值的N个周期的请求量之和
      "checkMillis": 1000, //检查周期
      "bid": "1", //bid
      "key": "dao_c|kfk_tpc_.23380.", //key
      "checkThreshold": 100  //检查阈值，在检查周期内请求数量超过该阈值，则认为是热key，times会+1
    }
  ],
  "bigKeyStats": [  //大key监控
    //特别的，你可以通过在camellia-redis-proxy.propertis里配置big.key.monitor.json.max.count=10000来限制该监控数据项的最大个数，默认不限制，一般来说，更好的方式是通过调整更合适的大key检测阈值来减少数据项
    {
      "commandType": "ZSET",  //key的类型，包括STRING/ZSET/SET/LIST/HASH等
      "size": 1903,  //大小
      "bgroup": "default",  //bgroup
      "threshold": 1000,  //阈值
      "bid": "16",  //bid，如果没有使用camellia-dashboard，而是使用了local配置，则bid=default/bgroup=default
      "command": "zrangebyscore", //命令
      "key": "saaaaa" //涉及的key
    },
    {
      "commandType": "SET",
      "size": 2912,
      "bgroup": "default",
      "threshold": 1000,
      "bid": "16",
      "command": "scard",
      "key": "ssasas"
    }
  ],
  "hotKeyCacheStats": [   //热key缓存监控
    {
      "bid": "1",  //bid
      "bgroup": "default",  //bgroup
      "key": "xxxx",  //涉及的key
      "hitCount": 49,  //缓存命中次数
      "checkMillis": 1000,  //检查周期
      "checkThreshold": 100  //检查阈值，在检查周期内，请求次数超过该阈值，则下一次请求会被缓存直接命中
    },
    {
      "hitCount": 459,
      "bgroup": "default",
      "checkMillis": 1000,
      "bid": "1",
      "key": "yyyy",
      "checkThreshold": 100
    }
  ],
  "routeConf": [ //当前路由表
    {
      "bgroup": "default",
      "updateTime": "2021-04-29 10:52:01",  //若重启proxy，updateTime会重置为当前时间
      "bid": 1,
      "resourceTable": "{\"type\":\"simple\",\"operation\":{\"read\":\"redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true\",\"type\":\"rw_separate\",\"write\":\"redis-sentinel://passwd123@127.0.0.1:26379/master\"}}"
    },
    {
      "bgroup": "default",
      "updateTime": "2021-04-29 10:52:32",
      "bid": 2,
      "resourceTable": "redis://passwd123@127.0.0.1:6379"
    }
  ],
  "resourceBidBgroupCommandStats": [  //根据业务统计的命令级别的路由统计
    {
      "resource": "redis://passwd123@127.0.0.1:6379",
      "qps": 0.4,
      "bgroup": "default",
      "count": 24,
      "bid": 2,
      "command": "smembers"
    },
    {
      "resource": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
      "qps": 0.35,
      "bgroup": "default",
      "count": 21,
      "bid": 1,
      "command": "hget"
    }
  ],
  "resourceCommandStats": [  //路由统计（命令）
    {
      "resource": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
      "qps": 238.61666666666667,
      "count": 14317,
      "command": "get"
    }, 
    {
      "resource": "redis://passwd123@127.0.0.1:6379",
      "qps": 63.9,
      "count": 3834,
      "command": "setex"
    }
  ],
  "resourceStats": [  //路由统计
    {
      "resource": "redis://passwd123@127.0.0.1:6379",
      "qps": 196.28333333333333,
      "count": 11777
    },
    {
      "resource": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
      "qps": 404.31666666666666,
      "count": 24259
    }
  ],
  "resourceBidBgroupStats": [ //业务路由统计
    {
      "resource": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
      "qps": 404.31666666666666,
      "bgroup": "default",
      "count": 24259,
      "bid": 9
    },
    {
      "resource": "redis://passwd123@127.0.0.1:6379",
      "qps": 196.28333333333333,
      "bgroup": "default",
      "count": 11777,
      "bid": 1
    }
  ],
  "redisConnectStats": [ //到后端redis的总连接数
    {
      "connect": 16
    }
  ],
  "redisConnectDetailStats": [//到后端各个redis的连接数
    {
      "addr": "@10.177.0.69:8803",
      "connect": 4
    },
    {
      "addr": "abc@10.201.48.171:6379",
      "connect": 4
    }
  ]
}
```