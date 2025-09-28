
## use prometheus and grafana to monitor camellia-redis-proxy

### step1 install prometheus

### step2 config prometheus
prometheus.yml
```yaml
# my global config
global:
  scrape_interval: 60s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 60s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093

# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: "prometheus"

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
      - targets: ["localhost:9090"]
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: "redis_proxy_1"
    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    metrics_path: "/metrics"
    static_configs:
      - targets: ["10.221.145.235:16379","10.221.145.234:16379"]%
```

config job_name, e.g. `redis_proxy_1`  
config targets, e.g. `["10.221.145.235:16379","10.221.145.234:16379"]`  

`10.221.145.235/10.221.145.234` is proxy ip, `16379` is proxy console port  

### step3 install grafana
add datasource prometheus

### step4 import grafana-config.json to grafana

[grafana-conf.json](grafana-conf.json)

you should:  
* replace `job` name in json, e.g. `test1` to `redis_proxy_1`
* replace `prometheus.uid` in json, e.g. `ca547f68-c185-4008-9fe7-0ffa290eb12c`

### step5 dashboard

#### metrics
```
# HELP proxy_info Redis Proxy Info
# TYPE proxy_info gauge
proxy_info{camellia_version="1.3.7",arch="x86_64",os_name="Mac OS X",os_version="14.1.2",system_load_average="4.59716796875",vm_vendor="Oracle Corporation",vm_name="Java HotSpot(TM) 64-Bit Server VM",vm_version="17.0.1+12-LTS-39",jvm_info="mixed mode, sharing",java_version="17.0.1",} 1
# HELP uptime Redis Proxy Uptime
# TYPE uptime gauge
uptime 494556717
# HELP start_time Redis Proxy Uptime
# TYPE start_time gauge
start_time 1702972469910
# HELP memory_info Redis Proxy Memory
# TYPE memory_info gauge
memory_info{type="free_memory"} 1884673280
memory_info{type="total_memory"} 5368709120
memory_info{type="max_memory"} 5368709120
memory_info{type="heap_memory_usage"} 1943755264
memory_info{type="no_heap_memory_usage"} 271743648
memory_info{type="netty_direct_memory"} 1168062208
# HELP cpu Redis Proxy Cpu
# TYPE cpu gauge
cpu{type="cpu_num"} 12
cpu{type="usage"} 213
# HELP thread Redis Proxy Thread
# TYPE thread gauge
thread{type="boss_thread"} 1
thread{type="work_thread"} 12
# HELP gc Redis Proxy gc
# TYPE gc gauge
gc{name="G1 Young Generation", type="count"} 8
gc{name="G1 Young Generation", type="time"} 33
gc{name="G1 Old Generation", type="count"} 0
gc{name="G1 Old Generation", type="time"} 0
# HELP client_connect Redis Proxy Connect Count
# TYPE client_connect gauge
client_connect 71
# HELP client_connect_detail Redis Proxy Connect Count Detail
# TYPE client_connect_detail gauge
client_connect_detail{tenant="1_default"} 46
client_connect_detail{tenant="2_default"} 22
# HELP QPS Redis Proxy QPS
# TYPE qps gauge
qps{type="qps"} 403
qps{type="write_qps"} 87
qps{type="read_qps"} 316
qps{type="max_qps"} 441
qps{type="max_write_qps"} 110
qps{type="max_read_qps"} 331
# HELP command_qps Redis Proxy Total Command QPS
# TYPE command_qps gauge
command_qps{command="get"} 41
command_qps{command="set"} 192
command_qps{command="expire"} 47
command_qps{command="zadd"} 12
command_qps{command="lpush"} 59
command_qps{command="zrange"} 76
command_qps{command="hset"} 48
command_qps{command="hgetall"} 66
# HELP command_spend_stats Redis Proxy Spend Stats
# TYPE command_spend_stats gauge
command_spend_stats{command="get",type="count"} 16
command_spend_stats{command="get",type="avg"} 0.226256
command_spend_stats{command="get",type="max"} 1.159709
command_spend_stats{command="get",type="p50"} 0.190956
command_spend_stats{command="get",type="p90"} 0.272747
command_spend_stats{command="get",type="p99"} 0.273412
command_spend_stats{command="set",type="count"} 52
command_spend_stats{command="set",type="avg"} 0.049023
command_spend_stats{command="set",type="max"} 6.548364
command_spend_stats{command="set",type="p50"} 0.021007
command_spend_stats{command="set",type="p90"} 0.036917
command_spend_stats{command="set",type="p99"} 0.330172
command_spend_stats{command="expire",type="count"} 7
command_spend_stats{command="expire",type="avg"} 0.179277
command_spend_stats{command="expire",type="max"} 4.229859
command_spend_stats{command="expire",type="p50"} 0.215459
command_spend_stats{command="expire",type="p90"} 0.248055
command_spend_stats{command="expire",type="p99"} 0.273080
command_spend_stats{command="zadd",type="count"} 88
command_spend_stats{command="zadd",type="avg"} 0.000109
command_spend_stats{command="zadd",type="max"} 4.299529
command_spend_stats{command="zadd",type="p50"} 0.140926
command_spend_stats{command="zadd",type="p90"} 0.145901
command_spend_stats{command="zadd",type="p99"} 0.410011
# HELP tenant_qps Redis Proxy Tenant QPS
# TYPE tenant_qps gauge
tenant_qps{tenant="1_default"} 37
tenant_qps{tenant="2_default"} 273
# HELP tenant_command_qps Redis Proxy Tenant Command QPS
# TYPE tenant_command_qps gauge
tenant_command_qps{tenant="1_default",command="get"} 228
tenant_command_qps{tenant="1_default",command="set"} 20
tenant_command_qps{tenant="2_default",command="get"} 168
tenant_command_qps{tenant="2_default",command="expire"} 53
# HELP tenant_command_spend_stats Redis Proxy Spend Stats
# TYPE tenant_command_spend_stats gauge
tenant_command_spend_stats{tenant="1_default",command="get",type="count"} 24
tenant_command_spend_stats{tenant="1_default",command="get",type="avg"} 0.173488
tenant_command_spend_stats{tenant="1_default",command="get",type="max"} 9.224230
tenant_command_spend_stats{tenant="1_default",command="get",type="p50"} 0.145736
tenant_command_spend_stats{tenant="1_default",command="get",type="p90"} 0.197311
tenant_command_spend_stats{tenant="1_default",command="get",type="p99"} 0.442240
tenant_command_spend_stats{tenant="1_default",command="set",type="count"} 85
tenant_command_spend_stats{tenant="1_default",command="set",type="avg"} 0.051730
tenant_command_spend_stats{tenant="1_default",command="set",type="max"} 4.195734
tenant_command_spend_stats{tenant="1_default",command="set",type="p50"} 0.077406
tenant_command_spend_stats{tenant="1_default",command="set",type="p90"} 0.089549
tenant_command_spend_stats{tenant="1_default",command="set",type="p99"} 0.347995
tenant_command_spend_stats{tenant="1_default",command="expire",type="count"} 83
tenant_command_spend_stats{tenant="1_default",command="expire",type="avg"} 0.212612
tenant_command_spend_stats{tenant="1_default",command="expire",type="max"} 8.771509
tenant_command_spend_stats{tenant="1_default",command="expire",type="p50"} 0.210019
tenant_command_spend_stats{tenant="1_default",command="expire",type="p90"} 0.227267
tenant_command_spend_stats{tenant="1_default",command="expire",type="p99"} 0.334999
tenant_command_spend_stats{tenant="2_default",command="get",type="count"} 50
tenant_command_spend_stats{tenant="2_default",command="get",type="avg"} 0.080800
tenant_command_spend_stats{tenant="2_default",command="get",type="max"} 3.169546
tenant_command_spend_stats{tenant="2_default",command="get",type="p50"} 0.199689
tenant_command_spend_stats{tenant="2_default",command="get",type="p90"} 0.268958
tenant_command_spend_stats{tenant="2_default",command="get",type="p99"} 0.470687
tenant_command_spend_stats{tenant="2_default",command="set",type="count"} 58
tenant_command_spend_stats{tenant="2_default",command="set",type="avg"} 0.110331
tenant_command_spend_stats{tenant="2_default",command="set",type="max"} 3.958564
tenant_command_spend_stats{tenant="2_default",command="set",type="p50"} 0.239233
tenant_command_spend_stats{tenant="2_default",command="set",type="p90"} 0.328563
tenant_command_spend_stats{tenant="2_default",command="set",type="p99"} 0.602665
tenant_command_spend_stats{tenant="2_default",command="expire",type="count"} 71
tenant_command_spend_stats{tenant="2_default",command="expire",type="avg"} 0.290812
tenant_command_spend_stats{tenant="2_default",command="expire",type="max"} 9.110151
tenant_command_spend_stats{tenant="2_default",command="expire",type="p50"} 0.116325
tenant_command_spend_stats{tenant="2_default",command="expire",type="p90"} 0.191287
tenant_command_spend_stats{tenant="2_default",command="expire",type="p99"} 0.240845
# HELP upstream_redis_connect Redis Proxy Upstream Redis Connect Count
# TYPE upstream_redis_connect gauge
upstream_redis_connect 140
# HELP upstream_redis_connect_detail Redis Proxy Upstream Redis Connect Count Detail
# TYPE upstream_redis_connect_detail gauge
upstream_redis_connect_detail{upstream="***@10.2.2.1:6379"} 10
upstream_redis_connect_detail{upstream="***@10.2.2.2:6379"} 10
upstream_redis_connect_detail{upstream="***@10.2.2.3:6379"} 10
# HELP upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats
# TYPE upstream_redis_spend_stats gauge
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="count"} 232
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="avg"} 0.149057
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="max"} 5.296120
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="p50"} 0.192256
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="p90"} 0.263175
upstream_redis_spend_stats{upstream="***@10.2.2.1:6379", type="p99"} 0.364994
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="count"} 337
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="avg"} 0.195434
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="max"} 4.705129
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="p50"} 0.261979
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="p90"} 0.344372
upstream_redis_spend_stats{upstream="***@10.2.2.2:6379", type="p99"} 0.375184
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="count"} 293
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="avg"} 0.152702
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="max"} 6.588670
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="p50"} 0.144971
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="p90"} 0.237163
upstream_redis_spend_stats{upstream="***@10.2.2.3:6379", type="p99"} 0.265288
# HELP proxy_route_conf Redis Proxy Upstream Redis Route Conf
# TYPE proxy_route_conf gauge
proxy_route_conf{tenant="1_default", route="redis://@127.0.0.1:6379"} 1
proxy_route_conf{tenant="2_default", route="redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"} 1
proxy_route_conf{tenant="3_default", route="{'type':'simple','operation':{'read':'redis://passwd123@127.0.0.1:6379','type':'rw_separate','write':'redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master'}}"} 1
# HELP upstream_redis_qps Redis Proxy Upstream Upstream Redis QPS
# TYPE upstream_redis_qps gauge
upstream_redis_qps{upstream="redis://@127.0.0.1:6379"} 227
upstream_redis_qps{upstream="redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"} 311
# HELP client_fail Redis Proxy Fail
# TYPE client_fail gauge
client_fail{reason="ChannelNotActive",} 6
client_fail{reason="ERR param wrong",} 0
# HELP upstream_fail Redis Proxy Upstream Fail
# TYPE upstream_fail gauge
upstream_fail{upstream="redis://@127.0.0.1:6379",command="get",msg="ERR param wrong"} 2
upstream_fail{upstream="redis://@127.0.0.1:6379",command="set",msg="ERR internal error"} 1
upstream_fail{upstream="redis-cluster://@127.0.0.1:6379,127.0.0.2:6379",command="get",msg="ERR param wrong"} 0
# HELP slow_command Redis Proxy Slow Command
# TYPE slow_command gauge
slow_command{tenant="1_default",command="get",keys="key9"} 2078.222726
# HELP string_big_key Redis Proxy Big Key
# TYPE string_big_key gauge
string_big_key{tenant="1_default",commandType="string",command="get",key="key8"} 1050817
# HELP collection_big_key Redis Proxy Big Key
# TYPE collection_big_key gauge
collection_big_key{tenant="2_default",commandType="zset",command="zrange",key="zset_key1"} 10187
# HELP hot-key Redis Proxy Hot Key
# TYPE key gauge
hot_key{tenant="1_default",key="hkey3",type="qps"} 1407
hot_key{tenant="1_default",key="hkey3",type="count"} 4221
# HELP hot_key_cache_hit Redis Proxy Hot Key Cache Hit
# TYPE hot_key_cache_hit gauge
hot_key_cache_hit{tenant="1_default",key="hkeyhit0"} 1241
```

#### category  

![img.png](img.png)

####  proxy nodes

![img_1.png](img_1.png)

####  system 

* single node

![img_2.png](img_2.png)

* multi nodes

![img_3.png](img_3.png)

#### proxy frontend

* single node

![img_5.png](img_5.png)

* multi node

![img_6.png](img_6.png)

#### proxy frontend(tenant)

* single node

![img_7.png](img_7.png)

* multi node

![img_8.png](img_8.png)

#### proxy upstream

* single node

![img_9.png](img_9.png)

* multi node

![img_10.png](img_10.png)

#### proxy fail

![img_11.png](img_11.png)

#### proxy slow command

![img_12.png](img_12.png)

#### proxy big key

![img_13.png](img_13.png)

#### proxy hot key

![img_14.png](img_14.png)

#### proxy hot key cache hit

![img_15.png](img_15.png)