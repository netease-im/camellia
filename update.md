
# 1.0.5（2020/04/27）
### 新增
* 新增camellia-redis-eureka-spring-boot-starter，方便spring boot工程以直连camellia-redis-proxy的模式接入（通过eureka自动发现proxy集群），从而不需要LVS/VIP这样的组件  
### 更新
* 优化CamelliaRedisLock的实现  
* 优化camellia-redis-proxy-hbase的实现  
* 更新了camellia-redis-proxy-hbase监控指标（见RedisHBaseMonitor类和RedisHBaseStats类）  
### fix
* 修复camellia-dashboard的swagger-ui的中文乱码问题  

# 1.0.4 (2020/04/20)
第一次发布  