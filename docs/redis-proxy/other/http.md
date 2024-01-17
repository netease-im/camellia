
## http

* 支持使用http协议访问proxy，并执行命令
* 支持一次性提交多个命令
* proxy的插件和监控功能都是生效的

### 不支持的命令
```
PROXY,SELECT,HELLO,AUTH,SENTINEL,CLUSTER,ASKING,QUIT,
```
```
阻塞型的命令
发布订阅命令（仅支持PUBLISH、SPUBLISH、PUBSUB）
事务命令
```

### 快速开始

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  monitor-enable: false  #monitor enable/disable configure
  monitor-interval-seconds: 60 #monitor data refresh interval seconds
  http-port: 8080
  plugins: #plugin list
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379
```


```shell
curl -XPOST -d '{"requestId":"218hsqs9nsxaq","userName":"default","password":"pass123","commands":["set k1 v1","set k2 v2","get k1","mget k1 k2 k3"]}' http://127.0.0.1:8080/commands
```

请求体
```json
{
  "requestId": "218hsqs9nsxaq",
  "userName": "default",
  "password": "pass123",
  "db": -1,
  "commands":
  [
    "set k1 v1",
    "set k2 v2",
    "get k1",
    "mget k1 k2 k3"
  ]
}
```

返回
```json
{
    "code": 200,
    "commands":
    [
        "set k1 v1",
        "set k2 v2",
        "get k1",
        "mget k1 k2 k3"
    ],
    "replies":
    [
        {
            "error": false,
            "value": "OK"
        },
        {
            "error": false,
            "value": "OK"
        },
        {
            "error": false,
            "value": "v1"
        },
        {
            "error": false,
            "value":
            [
                {
                    "error": false,
                    "value": "v1"
                },
                {
                    "error": false,
                    "value": "v2"
                },
                {
                    "error": false
                }
            ]
        }
    ],
    "requestId": "218hsqs9nsxaq"
}
```

### 请求参数

|      参数       |   类型    | 是否必填 |                                 说明                                 |
|:-------------:|:-------:|:----:|:------------------------------------------------------------------:|
|   requestId   | string  |  否   |                               请求唯一标识                               |
|   userName    | string  |  否   |                           取决于proxy的鉴权配置                            |
|   password    | string  |  否   |                           取决于proxy的鉴权配置                            |
|      db       | number  |  否   |                           默认-1，表示不显式设置db                           |
| requestBase64 | boolean |  否   | 默认false，如果设置为true，则除了第一个参数是直接的字符串，其他参数服务器都会做base64解码，方便客户端传递二进制的内容 |
|  replyBase64  | boolean |  否   |             默认false，如果设置为true，则回包中的bulk类型，也回改成base64返回             |
|      bid      | number  |  否   |        默认null，如果proxy使用了password到bid/bgroup的多租户映射，则不能设置本参数         |
|    bgroup     | string  |  否   |        默认null，如果proxy使用了password到bid/bgroup的多租户映射，则不能设置本参数         |
|   commands    |  array  |  是   |                             每一行表示一个命令                              |

### 响应参数

|    参数     |     类型     | 是否必填 |                    说明                    |
|:---------:|:----------:|:----:|:----------------------------------------:|
|   code    |   number   |  是   | 业务错误码，200表示成功，其他表示失败，如果失败，则可能没有replies字段 |
|    msg    |   string   |  否   |                   错误信息                   |
| requestId |   string   |  否   |                  请求唯一标识                  |
| commands  |   array    |  是   |                每一行表示一个命令                 |
|  replies  | json array |  否   |         和commands一一对应，内部是一个嵌套结构          |

`StatusReply` 示例：
```json
{
  "error": false,
  "value": "OK"
}
```

`ErrorReply` 示例：
```json
{
  "error": true,
  "value": "ERR wrong number of arguments for 'get' command"
}
```

`IntegerReply` 示例：
```json
{
  "error": false,
  "value": 10
}
```

`BulkReply` 示例：
```json
{
  "error": false,
  "base64": false,
  "value": "abc"
}
```
```json
{
  "error": false,
  "base64": true,
  "value": "Y2FtZWxsaWE="
}
```
如果value是null，则没有 `value` 字段

`MultiBulkReply` 示例：
```json
{
    "error": false,
    "value":
    [
        {
            "error": false,
            "base64": false,
            "value": "v1"
        },
        {
            "error": false,
            "base64": false,
            "value": "v2"
        },
        {
            "error": false
        }
    ]
}
```
`value` 是一个数组，每一项是一个 `reply`  