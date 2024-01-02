
## use prometheus and grafana to monitor camellia-id-gen-server

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
  - job_name: "id_gen_1"
    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    metrics_path: "/metrics"
    static_configs:
      - targets: ["10.221.145.235:8080","10.221.145.234:8080"]%
```

config job_name, e.g. `id_gen_1`  
config targets, e.g. `["10.221.145.235:8080","10.221.145.234:8080"]`  

`10.221.145.235/10.221.145.234` is id gen server ip, `8080` is id gen server port  

### step3 install grafana
add datasource prometheus

### step4 import grafana-config.json to grafana

[grafana-conf.json](grafana-conf.json)

you should:  
* replace `job` name in json, e.g. `test4` to `id_gen_1`
* replace `prometheus.uid` in json, e.g. `ca547f68-c185-4008-9fe7-0ffa290eb12c`

### step5 dashboard

#### metrics
```
# HELP info Id Gen Server Info
# TYPE info gauge
info{camellia_version="1.2.23",arch="x86_64",os_name="Mac OS X",os_version="10.16",system_load_average="2.96533203125",vm_vendor="Oracle Corporation",vm_name="Java HotSpot(TM) 64-Bit Server VM",vm_version="25.311-b11",jvm_info="mixed mode",java_version="1.8.0_311",} 1
# HELP uptime Id Gen Server Uptime
# TYPE uptime gauge
uptime 277345051
# HELP start_time Id Gen Server StartTime
# TYPE start_time gauge
start_time 1703289756453
# HELP memory_info Id Gen Server Memory
# TYPE memory_info gauge
memory_info{type="free_memory"} 1734005212
memory_info{type="total_memory"} 5368709120
memory_info{type="max_memory"} 5368709120
memory_info{type="heap_memory_usage"} 2809304035
memory_info{type="no_heap_memory_usage"} 356787908
memory_info{type="netty_direct_memory"} 1019225915
# HELP cpu Id Gen Server Cpu
# TYPE cpu gauge
cpu{type="cpu_num"} 12
cpu{type="usage"} 212
# HELP gc Id Gen Server gc
# TYPE gc gauge
gc{name="PS Scavenge", type="count"} 4
gc{name="PS Scavenge", type="time"} 49
gc{name="PS MarkSweep", type="count"} 2
gc{name="PS MarkSweep", type="time"} 67
# HELP request Id Gen Server request
# TYPE request gauge
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="count"} 50
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="spendAvg"} 3.893157
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="spendMax"} 11.044797
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="spendP50"} 2.408705
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="spendP90"} 4.036505
request{uri="/camellia/id/gen/segment/genIds?tag=test1", code="200", type="spendP99"} 8.513686
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="count"} 67
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="spendAvg"} 2.535551
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="spendMax"} 10.323363
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="spendP50"} 2.809416
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="spendP90"} 4.003654
request{uri="/camellia/id/gen/segment/genIds?tag=test2", code="200", type="spendP99"} 8.573133
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="count"} 66
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="spendAvg"} 3.878357
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="spendMax"} 10.363235
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="spendP50"} 2.823454
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="spendP90"} 4.096675
request{uri="/camellia/id/gen/segment/genId?tag=test2", code="200", type="spendP99"} 8.265777
```

#### category  

![img.png](img.png)

####  system 

![img_1.png](img_1.png)

#### request

![img_2.png](img_2.png)

