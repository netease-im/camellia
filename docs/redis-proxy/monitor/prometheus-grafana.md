## Prometheus - Grafana Monitoring

### Prometheus
- Prometheus has been exposed at: [localhost:16379/prometheus](localhost:16379/prometheus)
- Enable monitor plugin
- **application.properties**
```yaml
...
camellia-redis-proxy:
  #port: 6380 #The priority is higher than server.port. If it is missing, server.port will be used. If it is set to -6379, a random available port will be used.
  #application-name: camellia-redis-proxy-server # has higher priority than spring.application.name, if missing, use spring.application.name
  console-port: 16379 #console port, the default is 16379, if set to -16379, there will be a random available port, if set to 0, the console will not be started
  #  password: ${PROXY_PASSWORD:pass123} #proxy password, if a custom client-auth-provider-class-name is set, the password parameter is invalid
  monitor-enable: true #Whether to enable monitoring
  monitor-interval-seconds: 60 #Monitor callback interval
  plugins: 
    - monitorPlugin
...
```

- Config **/prometheus/prometheus.yaml**
```yaml
global:
  scrape_interval: 5s

scrape_configs:
- job_name: 'Camellia Redis Proxy'
  metrics_path: '/prometheus'
  static_configs:
    - targets: ['host.docker.internal:16379'] #change to your host
      labels:
        application: 'Camellia Redis Proxy'
```

### Grafana
- **docker-compose.yaml**
```yaml
version: '2.1'

services:

  prometheus:
    image: prom/prometheus:v2.38.0
    # network_mode: host
    container_name: prometheus-container
    volumes:
      - ./prometheus/:/etc/prometheus/ #note: config to your prometheus folder
    command:
      - '--config.file=/etc/prometheus/prometheus.yaml'
    ports:
      - "9090:9090"
    restart: always
    networks:
      - default

  grafana:
    image: grafana/grafana-oss:9.1.4
    user: "$UID:$GID"
    # network_mode: host
    container_name: grafana-container
    depends_on:
      - prometheus
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_SERVER_DOMAIN=localhost
    networks:
      - default
```
- Folder structure
```
grafana
|   docker-compose.yaml
|___prometheus
    |___prometheus.yaml
```
- Start:
```shell 
docker-compose up            
```
- Monitor:
  - You can access prometheus via: [localhost:9090](localhost:9090)
  - You can access grafana via: [localhost:3000](localhost:3000) _(username: admin, password: admin)_
![img.png](grafana-sample.png)
- Visualize on Grafana:
  - Import [redis-proxy-grafana.json](redis-proxy-grafana.json)
  - Alternately, you can create a custom dashboard that uses Prometheus metrics.
- Sample `/prometheus` data:
```
# HELP redis_proxy_connect_count Redis Proxy Connect Count
# TYPE redis_proxy_connect_count gauge
redis_proxy_connect_count 271
# HELP redis_proxy_total Redis Proxy Total
# TYPE redis_proxy_total counter
redis_proxy_total{type="all",} 1664519
redis_proxy_total{type="read",} 200
redis_proxy_total{type="write",} 1664319
redis_proxy_total{type="max_qps",} 1664519
redis_proxy_total{type="max_read_qps",} 200
redis_proxy_total{type="max_write_qps",} 1664319
# HELP redis_proxy_total_command Redis Proxy Total Command
# TYPE redis_proxy_total_command counter
redis_proxy_total_command{command="incr",} 255117
redis_proxy_total_command{command="auth",} 200
redis_proxy_total_command{command="hset",} 409258
redis_proxy_total_command{command="spop",} 999944
# HELP redis_proxy_bid_bgroup Redis Proxy Bid Bgroup
# TYPE redis_proxy_bid_bgroup gauge
redis_proxy_bid_bgroup{bid="245",bgroup="platform",} 1409202
redis_proxy_bid_bgroup{bid="3",bgroup="tass",} 255117
redis_proxy_bid_bgroup{bid="default",bgroup="default",} 200
# HELP redis_proxy_detail Redis Proxy Detail
# TYPE redis_proxy_detail counter
redis_proxy_detail{bid="default",bgroup="default",command="auth",} 200
redis_proxy_detail{bid="245",bgroup="platform",command="spop",} 999944
redis_proxy_detail{bid="245",bgroup="platform",command="hset",} 409258
redis_proxy_detail{bid="3",bgroup="tass",command="incr",} 255117
# HELP redis_proxy_fail Redis Proxy Fail
# TYPE redis_proxy_fail counter
redis_proxy_fail{reason="Not Available",} 1409224
# HELP redis_proxy_spend_stats Redis Proxy Spend Stats
# TYPE redis_proxy_spend_stats summary
redis_proxy_spend_stats_sum{command="auth",} 1.744076
redis_proxy_spend_stats_count{command="auth",} 200
redis_proxy_spend_stats{command="auth",quantile="0.5"} 0.000000
redis_proxy_spend_stats{command="auth",quantile="0.75"} 0.000000
redis_proxy_spend_stats{command="auth",quantile="0.90"} 0.010000
redis_proxy_spend_stats{command="auth",quantile="0.95"} 0.010000
redis_proxy_spend_stats{command="auth",quantile="0.99"} 0.010000
redis_proxy_spend_stats{command="auth",quantile="0.999"} 0.020000
redis_proxy_spend_stats{command="auth",quantile="1"} 0.030000
redis_proxy_spend_stats_sum{command="incr",} 10917030.367337
redis_proxy_spend_stats_count{command="incr",} 255079
redis_proxy_spend_stats{command="incr",quantile="0.5"} 0.600000
redis_proxy_spend_stats{command="incr",quantile="0.75"} 0.910000
redis_proxy_spend_stats{command="incr",quantile="0.90"} 87.640000
redis_proxy_spend_stats{command="incr",quantile="0.95"} 88.900000
redis_proxy_spend_stats{command="incr",quantile="0.99"} 92.080000
redis_proxy_spend_stats{command="incr",quantile="0.999"} 17051.380000
redis_proxy_spend_stats{command="incr",quantile="1"} 41120.120000
redis_proxy_spend_stats_sum{command="spop",} 790149.952603
redis_proxy_spend_stats_count{command="spop",} 999933
redis_proxy_spend_stats{command="spop",quantile="0.5"} 0.010000
redis_proxy_spend_stats{command="spop",quantile="0.75"} 0.010000
redis_proxy_spend_stats{command="spop",quantile="0.90"} 0.020000
redis_proxy_spend_stats{command="spop",quantile="0.95"} 0.020000
redis_proxy_spend_stats{command="spop",quantile="0.99"} 0.030000
redis_proxy_spend_stats{command="spop",quantile="0.999"} 0.060000
redis_proxy_spend_stats{command="spop",quantile="1"} 23505.420000
redis_proxy_spend_stats_sum{command="hset",} 666347.040306
redis_proxy_spend_stats_count{command="hset",} 409281
redis_proxy_spend_stats{command="hset",quantile="0.5"} 0.010000
redis_proxy_spend_stats{command="hset",quantile="0.75"} 0.010000
redis_proxy_spend_stats{command="hset",quantile="0.90"} 0.020000
redis_proxy_spend_stats{command="hset",quantile="0.95"} 0.020000
redis_proxy_spend_stats{command="hset",quantile="0.99"} 0.030000
redis_proxy_spend_stats{command="hset",quantile="0.999"} 0.060000
redis_proxy_spend_stats{command="hset",quantile="1"} 23504.740000
# HELP redis_proxy_bid_bgroup_spend_stats Redis Proxy Bid Bgroup Spend Stats
# TYPE redis_proxy_bid_bgroup_spend_stats summary
redis_proxy_bid_bgroup_spend_stats_sum{bid="default",bgroup="default",command="auth",} 1.744076
redis_proxy_bid_bgroup_spend_stats_count{bid="default",bgroup="default",command="auth",} 200
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.5"} 0.000000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.75"} 0.000000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.90"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.95"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.99"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="0.999"} 0.020000
redis_proxy_bid_bgroup_spend_stats{bid="default",bgroup="default",command="auth",quantile="1"} 0.030000
redis_proxy_bid_bgroup_spend_stats_sum{bid="245",bgroup="platform",command="spop",} 790149.952603
redis_proxy_bid_bgroup_spend_stats_count{bid="245",bgroup="platform",command="spop",} 999933
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.5"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.75"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.90"} 0.020000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.95"} 0.020000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.99"} 0.030000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="0.999"} 0.060000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="spop",quantile="1"} 23505.420000
redis_proxy_bid_bgroup_spend_stats_sum{bid="245",bgroup="platform",command="hset",} 666347.040306
redis_proxy_bid_bgroup_spend_stats_count{bid="245",bgroup="platform",command="hset",} 409281
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.5"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.75"} 0.010000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.90"} 0.020000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.95"} 0.020000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.99"} 0.030000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="0.999"} 0.060000
redis_proxy_bid_bgroup_spend_stats{bid="245",bgroup="platform",command="hset",quantile="1"} 23504.740000
redis_proxy_bid_bgroup_spend_stats_sum{bid="3",bgroup="tass",command="incr",} 10917030.367337
redis_proxy_bid_bgroup_spend_stats_count{bid="3",bgroup="tass",command="incr",} 255079
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.5"} 0.600000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.75"} 0.910000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.90"} 87.640000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.95"} 88.900000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.99"} 92.080000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="0.999"} 17051.380000
redis_proxy_bid_bgroup_spend_stats{bid="3",bgroup="tass",command="incr",quantile="1"} 41120.120000
# HELP redis_proxy_resource_stats Redis Proxy Resource Stats
# TYPE redis_proxy_resource_stats counter
redis_proxy_resource_stats{resource="redis://@xx.xx.xx.xx:6379",} 255117
redis_proxy_resource_stats{resource="redis://***********@xx.xx.xx.xx:6379",} 1409162
# HELP redis_proxy_resource_command_stats Redis Proxy Resource Command Stats
# TYPE redis_proxy_resource_command_stats counter
redis_proxy_resource_command_stats{resource="redis://***********@xx.xx.xx.xx:6379",command="spop",} 999944
redis_proxy_resource_command_stats{resource="redis://***********@xx.xx.xx.xx:6379",command="hset",} 409218
redis_proxy_resource_command_stats{resource="redis://@xx.xx.xx.xx:6379",command="incr",} 255117
# HELP redis_proxy_bid_bgroup_resource_command_stats Redis Proxy Bid Bgroup Resource Command Stats
# TYPE redis_proxy_bid_bgroup_resource_command_stats counter
redis_proxy_bid_bgroup_resource_command_stats{bid="245",bgroup="platform",resource="redis://***********@xx.xx.xx.xx:6379",command="hset",} 409218
redis_proxy_bid_bgroup_resource_command_stats{bid="245",bgroup="platform",resource="redis://***********@xx.xx.xx.xx:6379",command="spop",} 999944
redis_proxy_bid_bgroup_resource_command_stats{bid="3",bgroup="tass",resource="redis://@xx.xx.xx.xx:6379",command="incr",} 255117
# HELP redis_proxy_route_conf Redis Proxy Route Conf
# TYPE redis_proxy_route_conf gauge
redis_proxy_route_conf{bid="245",bgroup="platform",routeConf="redis://***********@xx.xx.xx.xx:6379",updateTime="1673248471748",} 1
redis_proxy_route_conf{bid="3",bgroup="tass",routeConf="{type:simple,operation:{read:redis://@127.0.0.1:6379,type:rw_separate,write:redis://@xx.xx.xx.xx:6379}}",updateTime="1673248459424",} 1
# HELP redis_proxy_redis_connect_stats Redis Proxy Redis Connect Stats
# TYPE redis_proxy_redis_connect_stats gauge
redis_proxy_redis_connect_stats{redis_addr="@xx.xx.xx.xx:6379",} 32
redis_proxy_redis_connect_stats{redis_addr="***********@xx.xx.xx.xx:6379",} 13
# HELP redis_proxy_upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats
# TYPE redis_proxy_upstream_redis_spend_stats summary
redis_proxy_upstream_redis_spend_stats_sum{redis_addr="@xx.xx.xx.xx:6379",} 10930472.866854
redis_proxy_upstream_redis_spend_stats_count{redis_addr="@xx.xx.xx.xx:6379",} 255085
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.5"} 0.590000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.75"} 0.900000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.9"} 87.630000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.95"} 88.880000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.99"} 92.070000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="0.999"} 17051.380000
redis_proxy_upstream_redis_spend_stats{redis_addr="@xx.xx.xx.xx:6379",quantile="1"} 41120.110000
# HELP redis_proxy_big_key_stats Redis Proxy Big Key Stats
# TYPE redis_proxy_big_key_stats counter
redis_proxy_big_key_stats{bid="1",bgroup="default",command="zrange",} 100
# HELP redis_proxy_hot_key_stats Redis Proxy Hot Key Stats
# TYPE redis_proxy_hot_key_stats counter
redis_proxy_hot_key_stats{bid="1",bgroup="default",} 10
# HELP redis_proxy_hot_key_cache_stats Redis Proxy Hot Key Cache Stats
# TYPE redis_proxy_hot_key_cache_stats counter
redis_proxy_hot_key_cache_stats{bid="1",bgroup="default",} 10
# HELP redis_proxy_slow_command_stats Redis Proxy Slow Command Stats
# TYPE redis_proxy_slow_command_stats counter
redis_proxy_slow_command_stats{bid="245",bgroup="platform",command="hset",} 24
redis_proxy_slow_command_stats{bid="3",bgroup="tass",command="incr",} 76
# HELP redis_proxy_upstream_fail_stats Redis Proxy Upstream Fail Stats
# TYPE redis_proxy_upstream_fail_stats counter
redis_proxy_upstream_fail_stats{resource="redis://@127.0.0.1:6379",command="get",msg="ERR proxy upstream connection not available",} 2
```

