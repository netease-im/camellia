
## 异常测试

|机器|规格|参数
|:---:|:---:|:---:|
|camellia-redis-proxy-sync|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|camellia-redis-proxy-async|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部，3主3从|
|redis|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单节点|

## 场景一  
### 描述  
后端是redis cluster，kill -15/kill -9掉其中一个master，等集群自动选主，观察proxy是否自动恢复  
kill -15和kill -9表现没有区别  

### 配置  

```
proxy-sync配置：

camellia-redis-proxy:
  password: pass123
  type: sync
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005

proxy-async配置：

camellia-redis-proxy:
  password: pass123
  type: async
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005

```      

### 测试结果  
|测试命令|机器|表现
|:---:|:---:|:---:|
|mget，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=4.4k；<br>宕机后失败率100%，qps=14；<br>选主成功后，自动恢复（立即），qps=4.4k|
||camellia-redis-proxy-async|宕机前qps=4k；<br>宕机后失败率100%，qps=1.5k；<br>选主成功后，自动恢复（立即），qps=4k|
|pipeline_get，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=2.3k；<br>宕机后失败率100%，qps=44；<br>选主成功后，自动恢复（立即），qps=2.3k|
||camellia-redis-proxy-async|宕机前qps=3.4k；<br>宕机后失败率100%，qps=1.2k；<br>选主成功后，自动恢复（立即），qps=3.4k|
|get，10并发|camellia-redis-proxy-sync|宕机前qps=7.8w；<br>宕机后失败率33%，成功qps=2.6k，失败qps=1.3k；<br>选主成功后，自动恢复（立即），qps=7.8w|
||camellia-redis-proxy-async|宕机前qps=6.1w；<br>宕机后失败率33%，成功qps=1.9w，失败qps=0.95w；<br>选主成功后，自动恢复（立即），qps=6.1w|
|mset，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=1.3k；<br>宕机后失败率100%，qps=13；<br>选主成功后，自动恢复（立即），qps=1.3k|
||camellia-redis-proxy-async|宕机前qps=1.3k；<br>宕机后失败率100%，qps=1.3k；<br>选主成功后，自动恢复（立即），qps=1.3k|
|setex，10并发|camellia-redis-proxy-sync|宕机前qps=6.6w；<br>宕机后失败率33%，成功qps=2.7k，失败qps=1.3k；<br>选主成功后，自动恢复（立即），qps=6.6w|
||camellia-redis-proxy-async|宕机前qps=5.4w；<br>宕机后失败率33%，成功qps=2.3w，失败qps=1.1w；<br>选主成功后，自动恢复（立即），qps=5.4w|
|pipeline_setex，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=983；<br>宕机后失败率100%，qps=50；<br>选主成功后，自动恢复（立即），qps=983|
||camellia-redis-proxy-async|宕机前qps=1.3k；<br>宕机后失败率100%，qps=1.1k；<br>选主成功后，自动恢复（立即），qps=1.3k|

## 场景二  
### 描述  
后端是redis cluster，kill -19掉其中一个master，造成进程假死，等集群自动选主，观察proxy是否自动恢复  
kill -19会造成进程假死，TCP连接能建立成功，但是不会响应  

### 配置  

```
同场景一

```      

### 测试结果  
|测试命令|机器|表现
|:---:|:---:|:---:|
|mget，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=4.7k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（71s），qps=4.7k|
||camellia-redis-proxy-async|宕机前qps=3.7k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（46s），qps=3.7k|
|pipeline_get，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=2.2k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（11s），qps=2.2k|
||camellia-redis-proxy-async|宕机前qps=2.3k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（30s），qps=2.3k|
|get，10并发|camellia-redis-proxy-sync|宕机前qps=7.7w；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（4s），qps=7.7w|
||camellia-redis-proxy-async|宕机前qps=6w；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（2s），qps=6w|
|mset，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=1.3k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（100s），qps=1.3k|
||camellia-redis-proxy-async|宕机前qps=1.3k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（50s），qps=1.3k|
|setex，10并发|camellia-redis-proxy-sync|宕机前qps=6.7w；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（2s），qps=6.7w|
||camellia-redis-proxy-async|宕机前qps=5.2w；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（5s），qps=5.2w|
|pipeline_setex，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=1.0k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（6s），qps=1.0k|
||camellia-redis-proxy-async|宕机前qps=1.2k；<br>宕机后失败率100%，qps=10；<br>选主成功后，自动恢复（40s），qps=1.2k|

## 场景三
### 描述  
后端是多个redis，proxy层进行分片（含双写），kill -15/kill -9掉6379这个实例（读写实例），kill掉6382实例（双写实例，没有读）  
kill -15和kill -9表现没有区别  

### 配置  

```
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "0-1-6-7": {
        "read": "redis://test123@nim-pri-02.jd.163.org:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://test123@nim-pri-02.jd.163.org:6379",
            "redis://test123@nim-pri-02.jd.163.org:6382"
          ],
          "type": "multi"
        }
      },
      "2-3-8-9": {
        "read": "redis://test123@nim-pri-02.jd.163.org:6380",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://test123@nim-pri-02.jd.163.org:6380",
            "redis://test123@nim-pri-02.jd.163.org:6383"
          ],
          "type": "multi"
        }
      },
      "4-5-10-11": {
        "read": "redis://test123@nim-pri-02.jd.163.org:6381",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://test123@nim-pri-02.jd.163.org:6381",
            "redis://test123@nim-pri-02.jd.163.org:6384"
          ],
          "type": "multi"
        }
      },
    },
    "bucketSize": 12
  }
}

```      

### 测试结果  
|测试命令|机器|表现
|:---:|:---:|:---:|
|mget，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=6.5k；<br>6379宕机后失败率100%，失败tps=6.1k；<br>拉起后，自动恢复（立即），qps=6.5k；<br>6382宕机后，失败率0%，qps=6.5k|
||camellia-redis-proxy-async|宕机前qps=6.8k；<br>6379宕机后失败率100%，失败tps=10.2k；<br>拉起后，自动恢复（立即），qps=6.8k；<br>6382宕机后，失败率0%，qps=6.8k|
|get，10并发|camellia-redis-proxy-sync|宕机前qps=7.7w；<br>6379宕机后失败率33%，成功qps=1.7w，失败qps=0.85w；<br>拉起后，自动恢复（立即），qps=7.7w；<br>6382宕机后，失败率0%，qps=7.7w；|
||camellia-redis-proxy-async|宕机前qps=6.1w；<br>6379宕机后失败率33%，成功qps=2.1w，失败qps=1.05w；<br>拉起后，自动恢复（立即），qps=6.1w；<br>6382宕机后，失败率0%，qps=6.1w；|
|setex，10并发|camellia-redis-proxy-sync|宕机前qps=6.1w；<br>6379宕机后失败率33%，成功qps=1.7w，失败qps=0.85w；<br>拉起后，自动恢复（立即），qps=6.1w；<br>6382宕机后失败率33%，成功qps=1.7w，失败qps=0.85w；<br>拉起后，自动恢复（立即）qps=6.1w；|
||camellia-redis-proxy-async|宕机前qps=5.6w；<br>6379宕机后失败率33%，成功qps=2.2w，失败qps=1.1w；<br>拉起后，自动恢复（立即），qps=5.6w；<br>6382宕机后，失败率0%，qps=5.6w；|
|pipeline_get，500g个key，10并发|camellia-redis-proxy-sync|宕机前qps=2.4k；<br>6379宕机后失败率100%，失败qps=260；<br>拉起后，自动恢复（立即），qps=2.3k；<br>6382宕机后失败率0%，qps=2.3k；|
||camellia-redis-proxy-async|宕机前qps=2.3k；<br>6379宕机后失败率100%，失败qps=2.3k；<br>拉起后，自动恢复（立即），qps=2.3k；<br>6382宕机后失败率0%，qps=2.3k；|
|pipeline_setex，500g个key，10并发|camellia-redis-proxy-sync|宕机前qps=1.4k；<br>6379宕机后失败率100%，失败qps=200；<br>拉起后，自动恢复（立即），qps=1.4k；<br>6382宕机后失败率100%，qps=200；<br>拉起后，自动恢复（立即）qps=1.4k；|
||camellia-redis-proxy-async|宕机前qps=1.8k；<br>6379宕机后失败率100%，失败qps=1.8k；<br>拉起后，自动恢复（立即），qps=1.8k；<br>6382宕机后失败率0%，qps=1.8k；|

## 场景四
### 描述  
后端是多个redis，proxy层进行分片（含双写），kill -19掉6379这个实例（读写实例），kill -19掉6382实例（双写实例，没有读）  
kill -19会造成进程假死，TCP连接能建立成功，但是不会响应  

### 配置  

```
配置同场景三

```      

### 测试结果  
|测试命令|机器|表现
|:---:|:---:|:---:|
|mget，500个key，10并发|camellia-redis-proxy-sync|宕机前qps=6.6k；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=6.6k；<br>6382宕机后，失败率0%，qps=6.6k；|
||camellia-redis-proxy-async|宕机前qps=6.9k；<br>6379宕机后失败率100%，tps=10；<br>拉起后，自动恢复（立即），qps=6.9k；<br>6382宕机后，失败率0%，qps=6.9k；|
|get，10并发|camellia-redis-proxy-sync|宕机前qps=7.5w；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=7.5w；<br>6382宕机后，失败率0%，qps=7.5w；|
||camellia-redis-proxy-async|宕机前qps=6w；<br>6379宕机后失败率33%，tps=20；<br>拉起后，自动恢复（立即），qps=6w；<br>6382宕机后，失败率0%，qps=6w；|
|setex，10并发|camellia-redis-proxy-sync|宕机前qps=6.3w；<br>6379宕机后失败率100%，失败tps=25；<br>拉起后，自动恢复（立即），qps=6.3w；<br>6382宕机后，失败率100%，qps=25；<br>拉起后，自动恢复（立即），qps=6.3w；|
||camellia-redis-proxy-async|宕机前qps=5.5w；<br>6379宕机后失败率33%，失败tps=20；<br>拉起后，自动恢复（立即），qps=5.5w；<br>6382宕机后，失败率0%，qps=5.5w；|
|pipeline_get，500g个key，10并发|camellia-redis-proxy-sync|宕机前qps=2.3k；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=2.3k；<br>6382宕机后，失败率0%，qps=2.3k；|
||camellia-redis-proxy-async|宕机前qps=2.4k；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=2.4k；<br>6382宕机后，失败率0%，qps=2.4k；|
|pipeline_setex，500g个key，10并发|camellia-redis-proxy-sync|宕机前qps=1.3k；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=1.3k；<br>6382宕机后，失败率100%，qps=10；<br>拉起后，自动恢复（立即），qps=1.3k；|
||camellia-redis-proxy-async|宕机前qps=2.4k；<br>6379宕机后失败率100%，失败tps=10；<br>拉起后，自动恢复（立即），qps=2.4k；<br>6382宕机后，失败率0%，qps=2.4k；|