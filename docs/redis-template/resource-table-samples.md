
### json-file配置示例

#### 单个地址
```
redis://passwd123@127.0.0.1:6379
```
CamelliaRedisTemplate会自动识别是否是json，如果发现不是json，则会认为是一个普通redis地址，没有分片/读写分离等逻辑

#### 配置读写分离一
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd123@127.0.0.1:6379",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"
  }
}
```
上面的配置表示：  
* 写命令会指向redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master  
* 读命令会指向redis://passwd123@127.0.0.1:6379

可以看到json里可以混用redis、redis-sentinel、redis-cluster  

#### 配置读写分离二
```json
{
  "type": "simple",
  "operation": {
    "read": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd123@127.0.0.1:26379/master"
  }
}
```
上面的配置表示：  
* 写命令会指向redis-sentinel://passwd123@127.0.0.1:26379/master  
* 读命令会指向redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true，也就是redis-sentinel://passwd123@127.0.0.1:26379/master的主节点和所有从节点

#### 配置分片（之前有命名错误，1.0.45及以前，请使用shading代替sharding，1.0.46及之后兼容sharding和shading）
```json
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key划分为6个分片，其中： 
* 分片[0,2,4]指向redis://password1@127.0.0.1:6379
* 分片[1,3,5]指向redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381

#### 配置双（多）写
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd1@127.0.0.1:6379",
    "type": "rw_separate",
    "write": {
      "resources": [
        "redis://passwd1@127.0.0.1:6379",
        "redis://passwd2@127.0.0.1:6380"
      ],
      "type": "multi"
    }
  }
}
```
上面的配置表示：  
* 所有的写命令（如setex/zadd/hset）指向redis://passwd1@127.0.0.1:6379和redis://passwd2@127.0.0.1:6380（即双写），特别的，客户端的回包是看的配置的第一个写地址  
* 所有的读命令（如get/zrange/mget）指向redis://passwd1@127.0.0.1:6379  

#### 配置多读
```json
{
  "type": "simple",
  "operation": {
    "read": {
      "resources": [
        "redis://password1@127.0.0.1:6379",
        "redis://password2@127.0.0.1:6380"
      ],
      "type": "random"
    },
    "type": "rw_separate",
    "write": "redis://passwd1@127.0.0.1:6379"
  }
}
```
上面的配置表示：  
* 所有的写命令（如setex/zadd/hset）指向redis://passwd1@127.0.0.1:6379  
* 所有的读命令（如get/zrange/mget）随机指向redis://passwd1@127.0.0.1:6379或者redis://password2@127.0.0.1:6380

#### 混合各种分片、双写逻辑（之前有命名错误，1.0.45及以前，请使用shading代替sharding，1.0.46及之后兼容sharding和shading）
```json
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "4": {
        "read": "redis://password1@127.0.0.1:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "5": {
        "read": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "random"
        },
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis://password1@127.0.0.1:6379",
      "1-3": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key被划分为6个分片，其中分片4配置了读写分离和双写的逻辑，分片5设置了读写分离和双写多读的逻辑