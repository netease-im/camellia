# Camellia Redis Proxy Helm Chart

[helm仓库页面](https://48n6e.github.io/camellia/) 

## 架构说明

本 Chart 部署以下组件：

- **camellia-redis-proxy**：Redis 代理服务（StatefulSet），对外暴露 Redis 协议
- **redis-single**：单节点 Redis 实例（可选），作为代理的后端存储

默认配置下，camellia 代理以读写分离模式连接后端 Redis：读请求随机分发，写请求统一写入主节点。

## 前置要求

- Kubernetes 1.19+
- Helm 3.x
- PV 供应（若启用 Redis 持久化）

## 快速部署

### 1. 添加 Helm 仓库（如适用）

```bash
helm repo add camellia-redis-proxy https://48n6e.github.io/camellia/
```

### 2. 安装 Chart

使用默认配置安装到命名空间 `camellia`：

```bash
helm install camellia-proxy ./camellia-redis-proxy -n camellia --create-namespace
```

### 3. 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -n camellia

# 查看 Service
kubectl get svc -n camellia
```

## 配置说明

### 主要参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `camellia.replicas` | Camellia 代理副本数 | `2` |
| `camellia.image.name` | Camellia 镜像 | `48n6e/camellia-redis-proxy:1.4.0-jdk-21-0.0.1` |
| `camellia.image.pullPolicy` | 镜像拉取策略 | `IfNotPresent` |
| `camellia.resources` | 代理资源限制与请求 | 见 values.yaml |
| `redis.image.name` | Redis 镜像 | `redis:5.0` |
| `redis.persistence.enabled` | 是否启用 Redis 持久化 | `false` |
| `redis.persistence.size` | Redis 存储大小 | `1Gi` |

### 自定义 values 部署

创建 `my-values.yaml`：

```yaml
camellia:
  replicas: 3
  image:
    name: "your-registry/camellia:1.4.0-jdk-21"
    pullPolicy: "IfNotPresent"
  resources:
    requests:
      cpu: 100m
      memory: 512Mi
    limits:
      cpu: 2000m
      memory: 2Gi

redis:
  persistence:
    enabled: true
    size: "5Gi"
    storageClass: "standard"
```

执行安装：

```bash
helm repo add camellia-redis-proxy https://48n6e.github.io/camellia/
helm install camellia-proxy ./camellia-redis-proxy -n camellia -f my-values.yaml
```

## 端口说明

| 组件 | 端口 | 说明 |
|------|------|------|
| camellia-redis-proxy | 6380 | Redis 协议端口，应用直连 |
| camellia-redis-proxy | 16379 | 控制台 / Prometheus metrics |
| redis-single | 6379 | 后端 Redis 服务（ClusterIP） |

## 访问代理服务

集群内应用通过 Service 访问：

- **Redis 协议**：`<release-name>-replicas:6380`
- **控制台**：`<release-name>-replicas:16379`
- **Metrics**：`http://<release-name>-replicas:16379/metrics`

例如，release 名为 `camellia-proxy` 时：

```bash
# 集群内测试 Redis 连接
kubectl run -it --rm redis-client --image=redis:alpine -n camellia -- redis-cli -h camellia-proxy-camellia-redis-proxy-replicas -p 6380 ping
```

## 连接外部 Redis

若要使用已有 Redis 而非 Chart 内置的 redis-single，需要修改 ConfigMap 中的路由配置（`resource-sample.json`）。可在安装后编辑 ConfigMap，或通过 values 传入自定义配置（需自行扩展 Chart 模板）。

## 持久化

- **camellia-redis-proxy**：无状态，不持久化
- **redis-single**：可通过 `redis.persistence.enabled: true` 启用，支持 `storageClass`、`size`、`accessMode` 等配置

## 升级与卸载

```bash
# 升级
helm upgrade camellia-proxy ./camellia-redis-proxy -n camellia -f my-values.yaml

# 卸载
helm uninstall camellia-proxy -n camellia
```

## 参考

- [Camellia 官方仓库](https://github.com/netease-im/camellia)
- [Camellia Redis Proxy 文档](https://github.com/netease-im/camellia/tree/master/docs/redis-proxy)
