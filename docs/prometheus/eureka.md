
## how to use eureka to discovery targets

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

  - job_name: "redis-proxy-1"
    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    metrics_path: "/metrics"
    eureka_sd_configs:
      - server: 'http://10.221.145.235:8761/eureka'
    relabel_configs:
    - source_labels: [__meta_eureka_app_instance_vip_address]
      regex: redis-proxy-1
      action: keep
    - source_labels: [__address__, __meta_eureka_app_instance_metadata_prometheus_port]
      action: replace
      regex: ([^:]+)(?::\d+)?;(\d+)
      replacement: $1:$2
      target_label: __address__

```

```yaml

server:
  port: 6380
spring:
  application:
    name: redis-proxy-1

eureka:
  client:
    serviceUrl:
      defaultZone: http://10.221.145.235:8761/eureka
    registryFetchIntervalSeconds: 5
  instance:
    leaseExpirationDurationInSeconds: 15
    leaseRenewalIntervalInSeconds: 5
    prefer-ip-address: true
    metadataMap.prometheus.port: 16379

camellia-redis-proxy:
  console-port: 16379
  password: pass123
  monitor-enable: true 
  monitor-interval-seconds: 60 
  plugins:
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```

