

## 关于使用redis-shake进行数据迁移的说明
* camellia-redis-proxy实现了标准的redis协议，包括scan命令，因此你可以从redis-standalone/redis-sentinel/redis-cluster或者twemproxy/codis迁移数据到camellia，当然也可以反向进行
* camellia-redis-proxy支持使用redis-shake的sync和rump两种模式进行数据的迁入，sync模式支持存量数据和增量数据的迁移，rump模式仅支持存量数据迁移
* 因为redis-shake在迁移之前会通过info命令校验redis版本，1.0.50以及之前的camellia-redis-proxy版本的info命令回包使用\n进行换行，之后的版本使用\r\n进行换行，而redis-shake默认使用\r\n来识别info命令回包，因此请使用1.0.51及之后的版本来对接redis-shake
* redis-shake的下载地址：https://github.com/alibaba/RedisShake

### 典型场景一
* 背景：从redis-cluster迁移到多套redis-sentinel
* 源集群：一套redis-cluster
* 目标集群：camellia-redis-proxy + redis-sentinel集群*N

### 典型场景二
* 背景：从1套redis-cluster迁移到多套redis-cluster组成自定义分片的逻辑大集群
* 源集群：一套redis-cluster
* 目标集群：camellia-redis-proxy + redis-cluster集群*N

### 典型场景三
* 背景：从twemproxy/codis迁移到redis-cluster，但是客户端不支持redis-cluster协议，因此加了camellia-redis-proxy作为前置代理
* 源集群：twemproxy/codis + 多个redis-server，对每个redis-server单独起一个redis-shake任务
* 目标集群：camellia-redis-proxy + redis-cluster集群