
# camellia-id-gen
## 简介  
提供了多种id生成算法，开箱即用，包括雪花算法、严格递增的id生成算法、趋势递增的id生成算法等  

## 雪花算法
### 特性
* 单节点递增，全局趋势递增，保证全局唯一
* 支持设置region标记，从而可以在单元化部署中保证不同单元之间id不冲突
* 默认提供了一种基于redis的workerId生成策略，避免手动设置workerId的繁琐  
* regionId、workerId、sequence的比特位数支持自定义配置
* 提供一个spring-boot-starter，快速搭建一个基于雪花算法的发号器集群

### 原理
* 生成的id是一个64位的数字：首位保留，41位表示时间戳，剩余22位可以灵活配置regionId、workerId、sequence的比特位分配比例（22位可以不用完，以便减少id的长度）  
* 每个region的每个发号器节点的workerId都不同，确保id不重复  
* id前缀是时间戳，确保趋势递增
* 每个ms内使用递增sequence确保唯一    
* 核心源码参见CamelliaSnowflakeIdGen  

### 用法
//todo


## 基于数据库的id生成算法 
### 特性
* 单节点递增，全局趋势递增，保证全局唯一
* 支持根据tag维护多条序列，彼此独立
* 支持设置region标记（比特位数可以自定义），从而可以在单元化部署中保证不同单元之间id不冲突
* 基于数据库保证id唯一，基于内存保证分配速度，基于预留buffer确保rt稳定
* 提供了一个spring-boot-starter，快速搭建一个基于数据库的segmentId发号器集群

### 原理
* 数据库表记录每个tag当前分配到的id，每个发号器节点每次从数据库取一段id保留在内存中作为buffer提升性能
* 内存buffer快用完时，提前从数据库load一批新id，避免内存buffer耗尽时引起rt周期性上升
* 如果设置region标记，则regionId会作为id的后几位，确保不同单元间的id整体是保持相同的趋势递增规律的
* 核心源码参见CamelliaSegmentIdGen

### 用法
//todo


## 基于数据库和redis的id生成算法
### 特性
* id全局严格递增
* 支持根据tag维护多条序列，彼此独立
* 支持peek操作（获取当前最新id，但是不使用）
* 提供了一个spring-boot-starter，快速搭建一个基于数据库和redis的严格递增的发号器集群

### 原理
* 数据库记录每个tag当前分配到的id
* 每个发号器节点会从数据库中取一段id后塞到redis的list中（不同节点会通过分布式锁保证id不会乱序）
* 每个发号器节点先从redis中取id，如果取不到则穿透到数据库进行load
* redis中的id即将耗尽时会提前从db中load最新一批的id
* 发号器节点会统计每个批次分配完毕消耗的时间来动态调整批次大小
* 核心源码参见CamelliaStrictIdGen

### 用法
//todo