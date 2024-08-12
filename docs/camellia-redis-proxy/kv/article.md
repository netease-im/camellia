# 通过hbase/tikv/obkv模拟redis协议来降本增效

## 一、背景

redis有丰富的数据结构，很多业务功能都使用了redis来实现相关功能，随着业务的发展，redis经历了多次扩容，扩容后的集群，内存占用很高，cpu却相对空闲，此外存储的数据存在比较明显的冷热分离的特性。  

因此考虑通过cpu换内存，以应对后续业务的增长，从而达到降本增效的目的。   


## 二、调研

既然要省内存，自然是要把冷数据落盘到磁盘上，业界有多款开源的兼容redis协议的磁盘型kv方案，包括：
* 基于leveldb的ssdb
* 基于rocksdb的pika、kvrocks、tendis
* 基于分布式kv进行协议封装的，如基于tikv的titea、tidis、titan等，也有基于obkv的陌陌的方案，不过没有开源。

其中ssdb已经不更新了，tendis看上去一年更新一次，pika则在停滞了几年后重新开始活跃，kvrocks作为apache的顶级项目，目前很活跃；    
至于基于分布式kv封装的方案，除了不开源的陌陌外，其余均已不再更新。  

那么留给我们选择的就只有pika和kvrocks了。

* pika的集群方案至少需要lvs、codis-proxy、codis-dashboad、pika-server四个组件  
* kvrocks则需要kvrocks-server、kvrocks-controller、etcd三个组件  

可以看到不管选择哪种，都会带来的一定的运维复杂性

考虑到线上目前已经有自研的redis-proxy组件，且有现成的hbase集群，并且dba对hbase运维经验丰富；  
进一步考虑到当前业务需求仅需要少量redis命令即可满足，则基于redis-proxy+hbase封装redis命令，看上去并不需要太多工作，但是却可以简化运维复杂性。  


## 三、方案可行性

使用hbase封装redis协议，方案类似于基于tikv/obkv到tidis、titan、titea以及陌陌的方案，但是不同于tikv提供了分布式事务，hbase仅提供单行事务，当然分布式事务也带来了tidis、titan、titea的性能瓶颈，特别是在大量写冲突时特别明显。  

hbase没有跨行事务，用kv模拟redis的复杂结构必然存在一个redis-key映射到多个hbase的key-value，因此会存在写入时部分成功的问题，但是结合业务需求，这样的弱一致性是可以接受的。  

此外，为了后续可能的存储层替换，我们把对底层kv的操作抽象为put/get/delete/scan等少量接口，hbase仅仅是其中一种实现方式，从而可以经过少量的代码开发就能以可插拔式的方式替换为其他分布式kv存储，如tikv/obkv等

在整个方案里，必须的组件仅有两个，一个是redis-proxy集群（现成的，仅需升级），一个是分布式kv集群（hbase，现成的，仅需创建一个表）。  

proxy集群会伪装为redis-cluster集群对外提供服务，这样做有2个好处：

* 不需要在proxy前面添加lvs等负载均衡器，只需要redis客户端支持redis-cluster协议即可，而且天然具有高可用切换的能力，并且可以方便的实现proxy层的扩缩容
* 因为相同slot的key同时只会被一个proxy节点处理，而且proxy内部的线程模型也会使用mpsc的无锁队列，把相同slot的key路由到同一个线程处理，从而不再需要进行复杂的锁同步机制，也简化了对kv层的数据结构的封装工作。当然这样的线程模型缺点是，在处理热点key时会无法发挥多线程的能力，这个我们后面再说。


## 四、数据结构
如何映射redis数据结构到通用的kv？我们借鉴了前文提到的pika/kvrocks的方案。

我们把kv区分为了meta-key和sub-key，通过不同的前缀来区分

### 1、meta-key
meta-key结构如下：

|               meta-key               |                          meta-value                           |
|:------------------------------------:|:-------------------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key | encode-version + key-type + key-version + expire-time + extra |

meta-key由四部分构成，如下：

* m#，这是一个固定前缀，表示这个key是一个meta-key，不同于pika/kvrocks，我们设计面向的是一个通用的kv存储，因此没有column-famliy的概念，因此使用前缀来区分
* namespace，用于划分命名空间，这样你就可以在一个kv存储上，构造出逻辑独立的多个redis集群了
* md5(key)[0-8]，通过增加8字节的md5的前缀，用于打散，避免key的热点
* key，就是redis-key本身


meta-value由5部分构造，如下：

* encode-version：1字节，sub-key的编码方式，默认0，通过不同的编码方式可以适用不同的特定场景
* key-type：1字节，redis-key的类型，如string、hash、zset等
* key-version：8字节，版本，用一个毫秒时间戳来表示，当一个key过期重新生成了，key-version就会发生变化
* expire-time：8字节，过期时间戳，也是一个毫秒级的时间戳，表示key什么时候过期，如果没有ttl，则为-1
* extra：n字节，这是一个可选的扩展字段，根据encode-version和key-type来确定格式


### 2、string
redis的string其实就是一个简单的k-v结构，因此此时不需要额外的sub-key结构了，而string的value直接存储在meta-value的extra里即可


### 3、hash和set
redis里的hash是一个字典结构，我们需要单独的sub-key结构来存储hash里的field-value

sub-key结构如下：

|                               sub-key                                | sub-value |
|:--------------------------------------------------------------------:|:---------:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + field |   value   |

sub-key有7部分组成，分别如下：

* s#，这是一个固定的前缀，用于表示这是一个sub-key
* namespace，同meta-key中的namespace
* md5(key)[0-8]，同meta-key中的md5前缀
* key.len，redis-key的长度，方便解析sub-key中的key字段
* key，redis-key本身
* key-version，key的版本，同meta-value中的key-version
* field，哈希字典中的field
* sub-value，就是value本身，对于set，则value为null


此外，为了更快的执行hlen/scard命令，对于hash/set的meta-key，我们会在extra中记录size  
但是，有一些业务场景下，可能只有hset、hdel、hget、hgetall、sadd、srem，而且并不关心hset的是否是一个已存在的field，而且也不需要执行hlen，此时单独维护size反而降低了性能，因此对于这两种不同的业务场景，我们提供了不同的encode-version来区分：

* encode-version为0时，meta-key中的extra字段为4字节，用于记录hash的size，此外hset和hdel等命令会准确的返回影响的行数，内部逻辑是先read后write，此外hlen是O(1)的复杂度，这是默认的encode-version
* encode-version为1时，meta-key中的extra字段为空，此外hset、hdel、sadd、srem等命令会直接进行写入，返回的影响行数是固定的参数长度，此外hlen/scard命令需要对底层进行scan操作，是O(n)的复杂度


### 4、zset
redis里的zset是一个有序集合，并且可以根据score、rank、lex等多种排序方法进行正序和逆序的查询，因此我们需要两个sub-key结构来存储zset里的元素，如下：

|                                sub-key                                | sub-value |
|:---------------------------------------------------------------------:|:---------:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + member |   score   |

|                                    sub-key                                    | sub-value |
|:-----------------------------------------------------------------------------:|:---------:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + score + member |   null    |

其中第一个sub-key包含7个部分：

* 前6个部分同hash
* member，zset中的member
* sub-value就是score

第二个sub-key包含8个部分：

* 前6个部分同第一个sub-key
* score，zset中的score，占用8字节
* member，zset中的member
* sub-value为null


存在第二个sub-key的原因是为了根据score进行正序和逆序的scan，表现为zrangebyscore、zrevrangebyscore、zremrangebyscore等命令

特别的，对于zset，有这样一种业务场景：
* 每个zset的key会不断的写入，同时不断的弹出最早的数据，从而每个key只保留最近n个元素，元素的score就是写入的时间戳
* 每个元素的长度可能比较长，比如几百或者上千字节
* zset的查询是根据时间戳来增量查询的，也就意味着大部分情况下只需要查询最近的几个元素

针对这次场景，我们额外提供了一种编码模式（encode-version=1），这种编码模式下：
* zset的结构本身是维护在redis中的，不再落库到kv存储
* redis中针对zset的member，只保留一个较小的索引，索引指向的原值，会落库到kv存储
* 索引到原值，会保留一个较短ttl的redis-cache
* 因为大部分读请求，都只查询最近几个元素，因此可以极大的降低redis的内存开销


### 5、其他数据结构 
使用kv去模拟list数据结构，效率较低，此外目前我们也没有需求，因此未实现  



## 五、本地缓存

因为所有数据都存储在分布式kv中，因此每次请求都需要rpc调用，为了提升服务的性能，我们会在proxy层增加lru的缓存来提高查询效率。  
因为proxy是以redis-cluster协议对外提供服务的，相同key的请求都在同一个proxy节点上，因此做本地缓存就非常简单，而当proxy节点进行扩缩容的时候，清空本地缓存即可。  

本地缓存包括meta-key的缓存和sub-key的缓存：

* meta-key本身是一个简单kv，直接映射到本地lru的map即可
* sub-key的缓存是redis-key粒度的，以hash为例，所有的field-value对，会作为一个整体缓存在本地的lru的map中



有了缓存后，读写操作的流程发生了变化：

* 对于读请求，会先检查缓存是否存在，如果存在，则直接返回
* 对于写请求，会先检查缓存是否存在，如果存在，则更新缓存，随后再更新kv
* 特别的，缓存既能提升读请求的响应时间，也能提升写请求，以zadd k1 1.0 v1为例，zadd请求映射到kv后，对于第一个sub-key，直接覆盖写即可，而对于第二个sub-key，则需要先获取v1的旧的score，删除后再写入新的score；而如果命中了本地缓存，则可以省略读的流程


什么时候新建缓存：

* 如果触发了全量读，如hgetall等，则读出来的结果可以直接写入本地缓存，没有额外成本
* 如果检测到是热key，如大量的zrange，虽然每次可能只是读一部分，但是超过了配置的阈值，如1秒10次，则会触发一次全量读，并写入到本地缓存，之后的读取可以直接走本地缓存
* 第一次写入，此时新建缓存不需要额外读取，但是为了避免大量写入产生的缓存新建影响到了读缓存，proxy内部会维护2套lru的缓存，即read-lru-cache和write-lru-cache，只有当产生实际的读请求，cache才会从write-lru-cache迁移到read-lru-cache


## 六、write-buffer

通过本地缓存和redis，底层kv的读请求会有很大一个比例被拦截，而对于写入，可能会因为抖动产生尖刺，因此proxy还支持开启write-buffer，让底层kv的写入异步化，大致逻辑如下：

* 如果本地缓存有值，则更新完本地缓存后即可返回，而不等底层kv写入完成
* 确保这样的本地缓存不会被lru给驱逐，从而保证写入的数据立即读取时可以读到
* 等kv写入完成，本地缓存应该允许驱逐，避免内存容量的风险
* 这样的本地缓存数量可控，避免内存容量的风险，如果超过配置阈值，回退为底层kv写入完成后再返回
* 开启write-buffer可以降低写入命令的响应时间，减少尖刺，当然缺点就是如果底层kv写入失败，会产生写命令返回成功，但是实际失败的不一致问题，因此按需开启


## 七、扩缩容

通过前文的描述，proxy是一个弱状态的服务，本身只保存可以驱逐的lru数据，但是又不是单纯的无状态服务，slot信息需要在proxy节点间达成一致，确保同一个key只会被同一个proxy节点处理。

proxy之前就支持伪redis-cluster模式，在之前的模式中，proxy节点会互相发送心跳，并且根据心跳信息各自判断出存活节点列表，并且按照固定的排序算法对slot进行均分，在没有网络分区的情况下，可以认为是一个最终一致的方案。

在proxy仅仅作为一个redis代理时，这样的模式是没有任何问题的，但是在本文描述的场景下，这种方案有以下缺点：

* 各个proxy节点slot信息在短时间内可能不一致，导致相同key可能同时被多个proxy节点处理
* 在proxy节点宕机或者下线，或者有新节点加入时，简单的对slot进行均分，会让proxy的本地缓存全量失效

针对上述问题，我们对proxy设计了一个对扩缩容更友好的伪redis-cluster模式：

* proxy需要选主，内置支持基于redis，未来也可以简单改造后即可支持zk、etcd等
* 所有slot变更都由master节点完成
* 因此当节点有变更时（扩缩容、宕机等），master在保证流量均衡的前提下，会尽可能减少slot在不同节点间的移动，从而提高节点本身的缓存命中率，提高扩缩容的平滑性
* 此外，因为slot变更由master节点统一发起，因此各节点的slot不一致状态的时间会更短，对应的kv存储的一致性也能更高

未来，我们还计划设计一个一致性更加强的伪redis-cluster模式，主要改动点如下：

* proxy需要选主，选主可以依赖外部组件（如zk、etcd、redis等），也可以自治（此时至少需要三个proxy节点）
* 所有的slot变更均需由主节点来发起变更，也都由主节点来负责对其他节点的探活，如果主节点挂了，其他节点会重新选主
* 变更过程分为prepare和commit两个阶段
* 主节点会把新的slot信息发送给所有节点，并标记阶段为prepare
* 在prepare阶段，proxy节点会把不再归属于自己的slot的命令重定向走，已经在执行的会等待执行完成，执行完成后，会给master响应prepare完成
* 在prepare阶段，proxy节点会把之前不属于，但是新的slot中属于自己的命令hold住，等待commit阶段后再执行（hold也是有时间要求的，如果超过配置时间，则返回错误告诉客户端正在slot迁移中）
* 当master接收到所有节点都prepare完成后，给所有节点下发commit指令
* master在节点上下线时，在流量均衡的前提下，尽可能让proxy节点的slot少变化
* proxy在收到commit后，会把不再归属于自己的slot的cache清空，而不是全量清空


## 八、过期key的清理（gc）

不同于pika/rocksdb，可以把过期的key的清理和compact绑定，我们这里需要使用定期扫描的方式，去删除过期的key，你可以配置在业务低峰期进行，并且可以配置扫描的速度。  
此外，你可以单独部署一个proxy节点，但是不加入集群中，由这个节点单独执行gc任务。


## 九、结论
目前实现了string、hash、zset的常用命令，会应用于云信相关的服务中，欢迎大家一起共建共享～