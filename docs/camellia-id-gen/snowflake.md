
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

### id构成（二进制）
<img src="id-gen-snowflake.png" width="90%" height="90%">

### 快速部署一（使用安装包部署独立服务）
具体见：[quick-start-package](quick-start-package-snowflake.md)

### 快速部署二（使用spring-boot-starter部署独立服务）
具体见：[quick-start-spring-boot](quick-start-spring-boot-starter-snowflake.md)


### api接口

#### 返回一个id（GET请求）  
http://127.0.0.1:8081/camellia/id/gen/snowflake/genId  
返回示例：  
```json
{
  "code": 200,
  "data": 6393964107649080,
  "msg": "success"
}
```

#### 解析时间戳（GET请求）  
http://127.0.0.1:8081/camellia/id/gen/snowflake/decodeTs?id=6393964107649080  
返回示例：  
```json
{
  "code": 200,
  "data": 1632727639837,
  "msg": "success"
}
```  

#### 解析regionId（GET请求）  
http://127.0.0.1:8081/camellia/id/gen/snowflake/decodeRegionId?id=11111  
返回示例：
```json
{
    "code": 200,
    "data": 10,
    "msg": "success"
}
```

#### 解析workerId（GET请求）  
http://127.0.0.1:8081/camellia/id/gen/snowflake/decodeWorkerId?id=11111  
返回示例：
```json
{
    "code": 200,
    "data": 1,
    "msg": "success"
}
```

#### 解析sequence（GET请求）  
http://127.0.0.1:8081/camellia/id/gen/snowflake/decodeSequence?id=11111  
返回示例：
```json
{
    "code": 200,
    "data": 23,
    "msg": "success"
}
```

### 使用java-sdk访问api接口

先引入maven依赖：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-sdk</artifactId>
    <version>1.3.5</version>
</dependency>
```
示例代码如下：  
```java
public class CamelliaSnowflakeIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8081");
        config.setMaxRetry(5);//重试次数
        CamelliaSnowflakeIdGenSdk idGenSdk = new CamelliaSnowflakeIdGenSdk(config);
        long id = idGenSdk.genId();//生成id
        System.out.println(id);
        long ts = idGenSdk.decodeTs(id);//从id中解析出时间戳
        System.out.println(ts);
        System.out.println(new Date(ts));

        long target = 10*10000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGenSdk.genId();
            i++;
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            if (i >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //QPS=5052.801778586226
    }
}


```


### 如果你不想部署独立服务  
则可以直接本地发号，具体见：[quick-start-java](quick-start-java-snowflake.md)