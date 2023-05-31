## camellia-config
一个简单的k-v配置平台，支持按namespace进行归类

## 关键词
namespace：camellia-config以namespace作为隔离和管理k-v配置的界限，基于namespace增删改查

## 接口文档（服务）
### 获取配置
GET /camellia/config/api/getConfig HTTP/1.1

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|md5|string|否|配置的md5|

响应
```json
{
  "code": 200,
  "md5": "xa1sa1xxza",
  "conf": 
  {
    "k1": "v1",
    "k2": "v2"
  }
}
```


## 接口文档（管理）
### 创建或者更新一个config的命名空间
POST /camellia/config/admin/createOrUpdateConfigNamespace HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|alias|string|否|namespace的别名，用于ui展示，最长32字符<br>创建时必填，更新时非必填|
|info|string|否|namespace的说明，最长16384字符<br>创建时必填|
|version|number|否|版本号，用于并发控制<br>创建时不填，更新时必填|
|validFlag|number|否|是否生效，0或者1，其他参数错误<br>创建时默认0|
|operatorInfo|string|是|操作说明，最长256字符|

响应  
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "id": 1,
    "namespace": "callback",
    "alias": "回调",
    "info": "callback config center",
    "version": 1,
    "validFlag": 1,
    "creator": "xxx@xx.com",
    "operator": "xxx@xx.com",
    "createTime": 1111,
    "updateTime": 222
  }
}
```

### 删除一个config的命名空间
POST /camellia/config/admin/deleteConfigNamespace HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|id|number|是|namespace的id|
|version|number|是|版本号，用于并发控制|
|operatorInfo|string|是|操作说明，最长256字符|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data": 1
}
```

### 分页查询config的命名空间
POST /camellia/config/admin/getConfigNamespaceList HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|pageIndex|number|否|偏移量，默认0，用于分页|
|pageSize|number|否|pageSize，默认100用于分页|
|validFlag|number|否|是否有效，0或者1，如果不填，则返回所有|
|keyword|string|否|关键词|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "count": 111,
    "list":
    [
      {
        "id": 1,
        "namespace": "callback",
        "alias": "回调",
        "info": "callback config center",
        "version": 1,
        "validFlag": 1,
        "creator": "xxx@xx.com",
        "operator": "xxx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 1111,
        "updateTime": 222
      },
      {
        "id": 2,
        "namespace": "callback",
        "alias": "回调2",
        "info": "callback config center",
        "version": 1,
        "validFlag": 1,
        "creator": "xxx@xx.com",
        "operator": "xxx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 1111,
        "updateTime": 222
      }
    ]
  }
}
```

### 创建或者更新一份配置
POST /camellia/config/admin/createOrUpdateConfig HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|key|string|是|配置key，最长256字符|
|value|string|否|配置value，最长4096字符<br>创建时必填|
|type|number|否|配置类型，1表示字符串，2表示整数，3表示浮点数，4表示布尔类型，5表示json<br>创建时必填|
|info|string|否|配置说明，最长4096字符<br>创建时必填|
|version|number|否|版本号，用于并发控制<br>创建时不填，更新时必填|
|validFlag|number|否|是否生效，0或者1，其他参数错误<br>创建时默认0|
|operatorInfo|string|是|操作说明，最长256字符|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "id": 1,
    "key": "k1",
    "namespace": "xxx",
    "namespaceId": "xxx",
    "value": "123",
    "type": 2,
    "info": "xx",
    "validFlag": 1,
    "version": 1,
    "creator": "xx@xx.com",
    "operator": "xx@xx.com",
    "createTime": 123,
    "updateTime": 234
  }
}
```

### 删除一份配置
POST /camellia/config/admin/deleteConfig HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|key|string|是|配置key，最长256字符|
|id|number|是|config的id|
|version|number|是|版本号，用于并发控制|
|operatorInfo|string|是|操作说明，最长256字符|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data": 1
}
```

### 查询一份配置
POST /camellia/config/admin/getConfigByKey HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|key|string|是|配置key，最长256字符|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "id": 1,
    "namespace": "xxx",
    "namespaceId": "xxx",
    "key": "k1",
    "value": "123",
    "type": 2,
    "info": "xx",
    "validFlag": 1,
    "version": 1,
    "creator": "xx@xx.com",
    "operator": "xx@xx.com",
    "createTime": 123,
    "updateTime": 234
  }
}
```

### 分页查询所有配置
POST /camellia/config/admin/getConfigList HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|pageIndex|number|否|偏移量，默认0，用于分页|
|pageSize|number|否|pageSize，默认100用于分页|
|validFlag|number|否|是否有效，0或者1，如果不填，则返回所有|
|keyword|string|否|关键词|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "count": 11,
    "list":
    [
      {
        "id": 1,
        "namespace": "xxx",
        "namespaceId": "xxx",
        "key": "k1",
        "value": "123",
        "type": 2,
        "info": "xx",
        "validFlag": 1,
        "version": 1,
        "creator": "xx@xx.com",
        "operator": "xx@xx.com",
        "createTime": 123,
        "updateTime": 234
      },
      {
        "id": 2,
        "namespace": "xxx",
        "namespaceId": "xxx",
        "key": "k2",
        "value": "123",
        "type": 2,
        "info": "xx",
        "validFlag": 1,
        "version": 1,
        "creator": "xx@xx.com",
        "operator": "xx@xx.com",
        "createTime": 123,
        "updateTime": 234
      }
    ]
  }
}
```


### 分页查询某个key的配置历史
POST /camellia/config/admin/getConfigHistoryListByConfigKey HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|id|number|否|配置key，最长256字符，id和key二选一，优先id|
|key|string|否|配置key，最长256字符，id和key二选一，优先id|
|pageIndex|number|否|偏移量，默认0，用于分页|
|pageSize|number|否|pageSize，默认100用于分页|
|keyword|string|否|关键词|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "count": 12,
    "list":
    [
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      },
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      }
    ]
  }
}
```

### 分页查询namespace的变更历史（增删改查）
POST /camellia/config/admin/getConfigNamespaceHistoryList HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|pageIndex|number|否|偏移量，默认0，用于分页|
|pageSize|number|否|pageSize，默认100用于分页|
|keyword|string|否|关键词|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "count": 12,
    "list":
    [
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      },
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      }
    ]
  }
}
```

### 分页查询某个namespace的变更历史（namespace自身+namespace下的key）
POST /camellia/config/admin/getConfigHistoryListByNamespace HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|namespace，最长128字符|
|pageIndex|number|否|偏移量，默认0，用于分页|
|pageSize|number|否|pageSize，默认100用于分页|
|keyword|string|否|关键词|

响应
```json
{
  "code": 200,
  "msg": "success",
  "data":
  {
    "count": 12,
    "list":
    [
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      },
      {
        "id": 1,
        "type": 2,
        "namespace": "callback",
        "configId": 1,
        "oldConfig": "{xxx}",
        "newConfig": "{xxx}",
        "operatorType": "UPDATE",
        "operator": "xx@xx.com",
        "operatorInfo": "xxx",
        "createTime": 123
      }
    ]
  }
}
```


