
### 复杂配置（json-file配置示例）

#### 配置单个地址
使用单独配置文件方式进行配置时，文件一般来说是一个json文件，但是如果你的配置文件里只写一个地址，也是允许的，proxy会识别这种情况，如下：
```
redis://passwd@127.0.0.1:6379
```
配置文件里只有一行数据，就是一个后端redis地址，表示proxy的路由转发规则是最简单的形式，也就是直接转发给该redis实例  
此时的配置效果和在application.yml里直接配置resource地址url效果是一样的，但是区别在于使用独立配置文件时，该地址是支持动态更新的

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
* 写命令会代理到redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master
* 读命令会代理到redis://passwd123@127.0.0.1:6379

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
* 写命令会代理到redis-sentinel://passwd123@127.0.0.1:26379/master
* 读命令会代理到redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true，也就是redis-sentinel://passwd123@127.0.0.1:26379/master的主节点和所有从节点

#### 配置分片
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
* 分片[0,2,4]代理到redis://password1@127.0.0.1:6379
* 分片[1,3,5]代理到redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381

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
* 所有的写命令（如setex/zadd/hset）代理到redis://passwd1@127.0.0.1:6379和redis://passwd2@127.0.0.1:6380（即双写），特别的，客户端的回包是看的配置的第一个写地址
* 所有的读命令（如get/zrange/mget）代理到redis://passwd1@127.0.0.1:6379

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
* 所有的写命令（如setex/zadd/hset）代理到redis://passwd1@127.0.0.1:6379
* 所有的读命令（如get/zrange/mget）随机代理到redis://passwd1@127.0.0.1:6379或者redis://password2@127.0.0.1:6380

#### 混合各种分片、双写逻辑
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