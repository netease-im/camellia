
# camellia-id-gen
## 简介  
* 提供了多种id生成算法，开箱即用，包括雪花算法、严格递增的id生成算法、趋势递增的id生成算法等  
* 支持使用prometheus/grafana来监控id-gen-server集群，参考：[prometheus-grafana](prometheus-grafana.md)  

## 雪花算法
详见：[snowflake](snowflake.md)

## 趋势递增的id生成算法（基于数据库）  
详见：[segment](segment.md)

## 严格递增的id生成算法（基于数据库+redis）
详见：[strict](strict.md)

## 严格递增的id生成算法（基于redis+ntp时间戳）
详见：[strict2](strict2.md)
