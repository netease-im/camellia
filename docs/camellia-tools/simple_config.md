
## SimpleConfigFetcher

### 简介
* 一个简单的通过http接口获取配置的工具类
* 定时轮询的方式获取配置

### 接口定义

* GET方法
* 请求参数包括：biz和md5，均为字符串，其中md5为非必填
* 响应为json，包括：code、md5、nextPullTime、config四个字段
* code：数字类型，包括200（正常响应）、304（配置未修改）、404（配置不存在）
* md5：字符串类型，为config的md5值，如果请求的md5和响应的md5相等，则返回304错误码
* nextPullTime：下一次拉取的毫秒时间戳，可以动态控制SimpleConfigFetcher的拉取频率
* config：为配置内容，如果返回304，则本字段为空

### 示例代码

```java
String url = "http://xxx/xxx";
String biz = "redis1";
SimpleConfigFetcher fetcher = new SimpleConfigFetcher(url, biz);

String config = fetcher.getConfig();

```