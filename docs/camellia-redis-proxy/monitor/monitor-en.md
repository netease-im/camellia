## Monitoring
camellia-redis-proxy provides rich monitoring functions, including:
* Provided monitoring items
* Monitoring data acquisition methods
* Get server related information through info command
* Use proxy as a platform to monitor redis cluster status (exposed via HTTP interface)
* Use prometheus and grafana to monitor proxy clusters

## Monitoring Items
### Large Key Monitoring
Implemented using BigKeyProxyPlugin, see: [big-key](../plugin/big-key.md)

### Hot Key Monitoring
Implemented using HotKeyProxyPlugin, see: [hot-key](../plugin/hot-key.md)

### Hot Key Cache Monitoring
Mainly monitors the hit situation of hot key cache, see: [hot-key-cache](../plugin/hot-key-cache.md)

### Request Count/RT/Slow Queries
Implemented using MonitorProxyPlugin, see: [monitor-plugin](../plugin/monitor-plugin.md)

### Other Monitoring Data
* Client connection count
* Backend redis connection count
* Backend redis response time
* Routing information
* ....

## Monitoring Data Acquisition
### Custom Callback
Configure custom callback. The default callback implementation is to print logs, as follows:
```properties
proxy.plugin.list=hotKeyPlugin,monitorPlugin,bigKeyPlugin,hotKeyCachePlugin
```
The callback class can obtain all monitoring data, refer to the definition of com.netease.nim.camellia.redis.proxy.monitor.model.Stats class

## Get Monitoring Data via HTTP API
Besides getting monitoring data through callbacks, you can also get it directly via HTTP API (JSON format), see: [Monitoring Data](monitor-data.md)

### Get Server Related Information via Info Command
proxy implements the info command, supporting returning the following information: Server/Clients/Route/Upstream/Memory/GC/Stats/Upstream-Info
See [info command](info.md) for details

### Use Proxy as a Platform to Monitor Redis Cluster Status (Exposed via HTTP Interface)
You can use HTTP interface to request proxy and pass the redis address that needs to be detected to proxy. Proxy will return the information of the target redis cluster in JSON format
See [detect](detect.md) for details

### Use Prometheus and Grafana to Monitor Proxy Clusters
See [prometheus-grafana](.././prometheus/prometheus-grafana.md) for details
