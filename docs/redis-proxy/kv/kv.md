
## camellia-redis-proxy-kv

基于camellia-redis-proxy的可插拔架构设计，支持对接外部kv存储，模拟redis协议

## 基本架构

![img_12.png](img_12.png)

* proxy基于redis-cluster模式运行，因此相同key会路由到同一个proxy节点（proxy节点扩缩容时需要精细化处理，todo）
* proxy内部多work-thread运行，每个命令根据key哈希到同一个work-thread运行
* proxy本身弱状态
* proxy依赖的存储包括三种类型：key-meta-server、cache-server、kv-storage
* key-meta-server，用于维护key的meta信息，包括key的类型、版本、ttl等，可以基于redis实现，也可以基于hbase/tikv/oceanbase实现
* cache-server，可选组件，基于redis，特点是key的ttl很短，并且允许换出
* kv-storage，持久化层，抽象简单的put/get/delete/scan等接口，可以基于hbase/tikv/oceanbase实现

## key-meta结构

![img.png](img.png)

## string数据结构

todo

## hash数据结构

![img_1.png](img_1.png)

![img_2.png](img_2.png)

![img_3.png](img_3.png)

hash数据有两种编码模式，区别在于key-meta中是否记录size字段，也就是哈希中元素个数

### hset命令

#### version-0

![img_4.png](img_4.png)

#### version-1

![img_5.png](img_5.png)

### hget命令

#### version-0 and version-1

![img_6.png](img_6.png)

### hdel命令

#### version-0

![img_7.png](img_7.png)

#### version-1

![img_8.png](img_8.png)

### hgetall命令

#### version-0 and version-1

![img_9.png](img_9.png)

### hlen命令

#### version-0

![img_10.png](img_10.png)

#### version-1

![img_11.png](img_11.png)

## zset数据结构

todo

## list数据结构

todo

## set数据结构

todo