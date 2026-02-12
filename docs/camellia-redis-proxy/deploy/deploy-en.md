## Deployment and Access
In production environments, you need to deploy at least 2 proxy instances to ensure high availability, and proxy can be horizontally expanded


## Table of Contents
### Deployment Modes
* Cluster based on LB (like LVS, or service in k8s, etc.), see: [standalone_mode](standalone_mode.md)
* Cluster based on registry, see: [register_mode](register_mode.md)
* redis-cluster mode, see: [cluster_mode](cluster_mode.md)
* redis-sentinel mode, see: [sentinel_mode](sentinel_mode.md)
* jvm-in-sidecar mode, see: [jvm-in-sidecar](jvm-in-sidecar.md)

### Others
* Graceful online/offline, see: [console](console.md)
* Using docker/k8s, see: [quick-start-docker](quick-start-docker.md)
