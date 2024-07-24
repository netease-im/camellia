
# CamelliaDynamicIsolationExecutor

## 简介
一个可以根据isolationKey自动选择不同线程池的执行器

### 设计目标：
* 在一个多租户的系统中，每个租户的表现可能是不一样的，有的租户执行任务快，有的执行任务慢，因此就会导致执行慢的租户影响到执行快的租户
* 因为系统资源是有限的，因此我们无法通过给每个租户设置一个线程池的方式来做完全的隔离
* 因此催生了本线程池工具
* CamelliaDynamicIsolationExecutor的基本原理是通过任务执行的统计数据和线程池的工作状态，动态分配线程资源
* 目标是执行快的租户不受执行慢的租户的影响，尽可能保证任务执行延迟保持在一个较短的水平

### 一个典型场景：
* 每个租户绑定一个http的请求地址，不同租户的http地址响应时间不一样，有的快，有的慢，不同租户的请求量也不一样
* 我们期望http响应慢的租户不要影响http响应快的租户

### 内部分为六个线程池：
* 1）fastExecutor，执行耗时较短的任务
* 2）fastBackUpExecutor，fastExecutor的backup
* 3）slowExecutor，执行耗时较长的任务
* 4）slowBackupExecutor，slowExecutor的backup
* 5）whiteListExecutor，白名单isolationKey在这里执行，不关心统计数据
* 6）isolationExecutor，隔离线程池，如果上述五个线程池都执行不了，则最终使用isolationExecutor，如果还是执行不了，则走fallback放弃执行任务

### 规则：
* 1）默认走fastExecutor
* 2）[选择阶段] 如果统计为快（默认阈值1000ms），则使用fastExecutor；如果统计为慢（默认阈值1000ms），则使用slowExecutor
* 3）[选择阶段] 如果fastExecutor/slowExecutor任务执行延迟超过阈值（默认300ms），且fastBackUpExecutor/slowBackupExecutor的延迟小于fastExecutor/slowExecutor，则使用fastBackUpExecutor/slowBackupExecutor
* 4）[提交阶段] 如果因为fastExecutor/slowExecutor繁忙而提交失败，则进入fastBackUpExecutor/slowBackupExecutor，如果仍然繁忙，则转交给isolationExecutor
* 5）[执行阶段] 如果某个isolationKey的最新统计数据和当前线程池不匹配，则转交给匹配的线程池
* 6）[执行阶段] 如果某个线程池执行任务延迟超过阈值（默认300ms），且其他线程池有空闲的（有空闲的线程），则转交给其他线程池（fast会找fastBackup+isolation，fastBackup会找fast+isolation，slow会找slowBackup+isolation，slowBackup会找slow+isolation）
* 7）[执行阶段] 如果某个isolationKey在fastExecutor/slowExecutor中占有线程数比例超过阈值（默认0.3），则转交给fastBackUpExecutor/slowBackupExecutor执行
* 8）[执行阶段] 如果某个isolationKey在fastBackUpExecutor/slowBackupExecutor占有线程数比例也超过阈值（默认0.3），则转交给isolationExecutor执行
* 9）[选择阶段] 在白名单列表里的isolationKey，直接在whiteListExecutor中执行；[提交阶段] 如果whiteListExecutor繁忙，则转交给isolationExecutor
* 10）最终所有任务都会把isolationExecutor作为兜底，如果isolationExecutor因为繁忙处理不了任务，则走fallback回调告诉任务提交者任务被放弃执行了
* 11）可以设置任务过期时间（默认不过期），任务如果过期而被放弃也会走fallback
* 12）fallback方法务必不要有阻塞，fallback方法会告知任务不执行的原因（当前定义了2个原因：任务已过期、任务被拒绝）

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.2.27</version>
</dependency>
```

## 示例
* 有8个执行较快的租户（执行耗时分别为：50ms、50ms、100ms、100ms、200ms、300ms、400ms、500ms）
* 有2个执行较慢的租户（执行耗时分别为：2000ms、3000ms）
* 一共执行100s
```java
public class CamelliaDynamicIsolationExecutorSamples {

    private static final AtomicBoolean stop = new AtomicBoolean(false);
    private static final long statTime = System.currentTimeMillis();
    private static final long maxRunTime = 100*1000L;

    public static void main(String[] args) throws InterruptedException {
        CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
        int thread = 10;
        CamelliaDynamicIsolationExecutorConfig config = new CamelliaDynamicIsolationExecutorConfig("test", () -> thread);
        config.setIsolationThresholdPercentage(() -> 0.3);
        CamelliaDynamicIsolationExecutor executor1 = new CamelliaDynamicIsolationExecutor(config);

        ThreadPoolExecutor executor2 = new ThreadPoolExecutor(thread * 5, thread * 5, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //test CamelliaDynamicIsolationExecutor
        test(manager, executor1, null);

        //test CamelliaDynamicIsolationExecutor
        //test(manager, null, executor2);

        Thread.sleep(1000);
        System.exit(-1);
    }

    private static void test(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        Set<String> isolationKeys = new HashSet<>();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast1", 10000, 50, 50)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast2", 10000, 50, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast3", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast4", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast5", 10000, 200, 200)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast6", 10000, 300, 300)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast7", 10000, 400, 400)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast8", 10000, 500, 500)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow1", 1000,2000, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow2", 800,3000, 100)).start();

        latch.await();

        System.out.println("======end,spend=" + (System.currentTimeMillis() - statTime) + "======");
        System.out.println("======total======");
        Map<String, CamelliaStatsData> statsDataAndReset = manager.getStatsDataAndReset();
        for (String isolationKey : isolationKeys) {
            CamelliaStatsData camelliaStatsData = statsDataAndReset.get(isolationKey);
            System.out.println(isolationKey + ",stats=" + JSONObject.toJSON(camelliaStatsData));
            statsDataAndReset.remove(isolationKey);
        }
        System.out.println("======detail======");
        for (String isolationKey : isolationKeys) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().startsWith(isolationKey)) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
        System.out.println("======type======");
        for (CamelliaDynamicIsolationExecutor.Type type : CamelliaDynamicIsolationExecutor.Type.values()) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().equals(type.name())) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
    }

    private static void doTask(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2, Set<String> isolationKeys,
                               CountDownLatch latch, String isolationKey, int taskCount, long taskSpendMs, int taskIntervalMs) {
        isolationKeys.add(isolationKey);
        CountDownLatch latch1 = new CountDownLatch(taskCount);
        boolean isBreak = false;
        for (int i=0; i<taskCount; i++) {
            if (isStop()) {
                isBreak = true;
                break;
            }
            final long id = i;
            final long start = System.currentTimeMillis();
            if (executor1 != null) {
                executor1.submit(isolationKey, () -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
                continue;
            }
            if (executor2 != null) {
                executor2.submit(() -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
            }
        }
        try {
            if (!isBreak) {
                latch1.await();
            }
            latch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void doTask(long id, long start, String isolationKey, CamelliaStatisticsManager manager, long taskSpendMs, CountDownLatch latch1) {
        if (isStop()) {
            latch1.countDown();
            return;
        }
        long latency = System.currentTimeMillis() - start;
        CamelliaDynamicIsolationExecutor.Type type = CamelliaDynamicIsolationExecutor.getCurrentExecutorType();
        System.out.println("key=" + isolationKey + ", start, latency = " + latency + ", id = " + id
                + ", thread=" + Thread.currentThread().getName() + ",type=" + type + ",time=" + (System.currentTimeMillis() - statTime));
        manager.update(isolationKey + "|" + type, latency);
        manager.update(isolationKey, latency);
        manager.update(String.valueOf(type), latency);
        sleep(taskSpendMs);
        latch1.countDown();
    }

    private static boolean isStop() {
        if (stop.get()) return true;
        if (System.currentTimeMillis() - statTime > maxRunTime) {
            stop.set(true);
        }
        return stop.get();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

执行结果（CamelliaDynamicIsolationExecutor）：
```
======end,spend=101849======
======total======
fast7,stats={"p99":520,"avg":34.318548387096776,"max":588,"p90":17,"count":248,"p50":0,"p999":633,"sum":8511,"p95":350,"p75":0}
fast6,stats={"p99":510,"avg":31.48181818181818,"max":583,"p90":28,"count":330,"p50":0,"p999":520,"sum":10389,"p95":287,"p75":0}
fast5,stats={"p99":520,"avg":33.78093306288032,"max":613,"p90":21,"count":493,"p50":0,"p999":650,"sum":16654,"p95":300,"p75":0}
fast4,stats={"p99":518,"avg":36.5041067761807,"max":617,"p90":100,"count":974,"p50":0,"p999":675,"sum":35555,"p95":370,"p75":0}
slow2,stats={"p99":69442,"avg":19216.124338624337,"max":70826,"p90":57678,"count":378,"p50":5200,"p999":70480,"sum":7263695,"p95":64252,"p75":37957}
fast8,stats={"p99":520,"avg":34.96482412060301,"max":583,"p90":13,"count":199,"p50":0,"p999":625,"sum":6958,"p95":370,"p75":0}
slow1,stats={"p99":69355,"avg":13639.958592132505,"max":71212,"p90":53019,"count":483,"p50":1,"p999":70840,"sum":6588100,"p95":61930,"p75":19000}
fast3,stats={"p99":516,"avg":36.239465570400824,"max":656,"p90":77,"count":973,"p50":0,"p999":700,"sum":35261,"p95":362,"p75":0}
fast2,stats={"p99":520,"avg":39.5374358974359,"max":616,"p90":152,"count":975,"p50":0,"p999":683,"sum":38549,"p95":412,"p75":0}
fast1,stats={"p99":519,"avg":34.40853979968371,"max":657,"p90":39,"count":1897,"p50":0,"p999":675,"sum":65273,"p95":350,"p75":0}
======detail======
fast7|ISOLATION,stats={"p99":0,"avg":320.0,"max":320,"p90":0,"count":1,"p50":0,"p999":0,"sum":320,"p95":0,"p75":0}
fast7|FAST_BACKUP,stats={"p99":24,"avg":50.0,"max":370,"p90":24,"count":9,"p50":6,"p999":24,"sum":450,"p95":24,"p75":16}
fast7|FAST,stats={"p99":520,"avg":32.52521008403362,"max":588,"p90":1,"count":238,"p50":0,"p999":633,"sum":7741,"p95":350,"p75":0}
fast6|FAST_BACKUP,stats={"p99":160,"avg":51.6,"max":450,"p90":84,"count":15,"p50":0,"p999":160,"sum":774,"p95":160,"p75":28}
fast6|FAST,stats={"p99":510,"avg":30.523809523809526,"max":583,"p90":1,"count":315,"p50":0,"p999":520,"sum":9615,"p95":287,"p75":0}
fast5|FAST,stats={"p99":518,"avg":30.625531914893617,"max":613,"p90":1,"count":470,"p50":0,"p999":650,"sum":14394,"p95":290,"p75":0}
fast5|FAST_BACKUP,stats={"p99":520,"avg":88.27272727272727,"max":572,"p90":180,"count":22,"p50":1,"p999":520,"sum":1942,"p95":510,"p75":22}
fast5|ISOLATION,stats={"p99":0,"avg":318.0,"max":318,"p90":0,"count":1,"p50":0,"p999":0,"sum":318,"p95":0,"p75":0}
fast4|FAST_BACKUP,stats={"p99":520,"avg":106.23076923076923,"max":603,"p90":400,"count":39,"p50":47,"p999":520,"sum":4143,"p95":460,"p75":115}
fast4|FAST,stats={"p99":517,"avg":32.961414790996784,"max":617,"p90":1,"count":933,"p50":0,"p999":650,"sum":30753,"p95":320,"p75":0}
fast4|ISOLATION,stats={"p99":320,"avg":329.5,"max":353,"p90":320,"count":2,"p50":320,"p999":320,"sum":659,"p95":320,"p75":320}
slow2|ISOLATION,stats={"p99":69788,"avg":35016.85024154589,"max":70826,"p90":63560,"count":207,"p50":34843,"p999":70480,"sum":7248488,"p95":67020,"p75":52834}
slow2|SLOW_BACKUP,stats={"p99":1900,"avg":91.70666666666666,"max":2173,"p90":1,"count":75,"p50":0,"p999":1900,"sum":6878,"p95":625,"p75":0}
slow2|SLOW,stats={"p99":370,"avg":19.80821917808219,"max":1076,"p90":1,"count":73,"p50":0,"p999":370,"sum":1446,"p95":1,"p75":0}
slow2|FAST_BACKUP,stats={"p99":480,"avg":196.22222222222223,"max":481,"p90":480,"count":9,"p50":120,"p999":480,"sum":1766,"p95":480,"p75":165}
slow2|FAST,stats={"p99":625,"avg":365.5,"max":563,"p90":520,"count":14,"p50":420,"p999":625,"sum":5117,"p95":625,"p75":505}
fast8|FAST,stats={"p99":520,"avg":34.00523560209424,"max":583,"p90":1,"count":191,"p50":0,"p999":625,"sum":6495,"p95":320,"p75":0}
fast8|FAST_BACKUP,stats={"p99":42,"avg":57.875,"max":384,"p90":42,"count":8,"p50":12,"p999":42,"sum":463,"p95":42,"p75":13}
slow1|ISOLATION,stats={"p99":70469,"avg":35318.1129032258,"max":71212,"p90":64157,"count":186,"p50":36683,"p999":70840,"sum":6569169,"p95":67499,"p75":53761}
slow1|SLOW_BACKUP,stats={"p99":1650,"avg":53.857142857142854,"max":2095,"p90":1,"count":133,"p50":0,"p999":1750,"sum":7163,"p95":1,"p75":0}
slow1|FAST,stats={"p99":650,"avg":403.94117647058823,"max":616,"p90":625,"count":17,"p50":440,"p999":650,"sum":6867,"p95":650,"p75":513}
slow1|FAST_BACKUP,stats={"p99":470,"avg":205.33333333333334,"max":605,"p90":430,"count":15,"p50":90,"p999":470,"sum":3080,"p95":470,"p75":380}
slow1|SLOW,stats={"p99":1,"avg":13.795454545454545,"max":1813,"p90":0,"count":132,"p50":0,"p999":1,"sum":1821,"p95":1,"p75":0}
fast3|FAST,stats={"p99":515,"avg":33.554126473740624,"max":656,"p90":1,"count":933,"p50":0,"p999":700,"sum":31306,"p95":360,"p75":0}
fast3|FAST_BACKUP,stats={"p99":400,"avg":74.47222222222223,"max":581,"p90":313,"count":36,"p50":17,"p999":400,"sum":2681,"p95":320,"p75":51}
fast3|ISOLATION,stats={"p99":330,"avg":318.5,"max":353,"p90":330,"count":4,"p50":320,"p999":330,"sum":1274,"p95":330,"p75":330}
fast2|ISOLATION,stats={"p99":370,"avg":333.75,"max":361,"p90":370,"count":4,"p50":330,"p999":370,"sum":1335,"p95":370,"p75":370}
fast2|FAST,stats={"p99":520,"avg":35.79424307036248,"max":616,"p90":1,"count":938,"p50":0,"p999":683,"sum":33575,"p95":390,"p75":0}
fast2|FAST_BACKUP,stats={"p99":460,"avg":110.27272727272727,"max":495,"p90":410,"count":33,"p50":23,"p999":460,"sum":3639,"p95":455,"p75":152}
fast1|FAST,stats={"p99":518,"avg":31.86685082872928,"max":657,"p90":1,"count":1810,"p50":0,"p999":675,"sum":57679,"p95":296,"p75":0}
fast1|ISOLATION,stats={"p99":340,"avg":334.0,"max":343,"p90":340,"count":2,"p50":340,"p999":340,"sum":668,"p95":340,"p75":340}
fast1|FAST_BACKUP,stats={"p99":625,"avg":81.48235294117647,"max":587,"p90":386,"count":85,"p50":4,"p999":625,"sum":6926,"p95":490,"p75":55}
======type======
FAST,stats={"p99":519,"avg":34.74005803038061,"max":657,"p90":6,"count":5859,"p50":0,"p999":677,"sum":203542,"p95":368,"p75":0}
FAST_BACKUP,stats={"p99":637,"avg":95.43911439114392,"max":605,"p90":397,"count":271,"p50":17,"p999":675,"sum":25864,"p95":490,"p75":90}
SLOW,stats={"p99":1,"avg":15.93658536585366,"max":1813,"p90":1,"count":205,"p50":0,"p999":1150,"sum":3267,"p95":1,"p75":0}
SLOW_BACKUP,stats={"p99":1750,"avg":67.5048076923077,"max":2173,"p90":1,"count":208,"p50":0,"p999":2150,"sum":14041,"p95":520,"p75":0}
ISOLATION,stats={"p99":70312,"avg":33961.255528255526,"max":71212,"p90":63833,"count":407,"p50":34499,"p999":71032,"sum":13822231,"p95":67432,"p75":52855}
```
执行结果（ThreadPoolExecutor）：
```
======end,spend=100228======
======total======
fast7,stats={"p99":10350,"avg":5478.679324894515,"max":10377,"p90":9720,"count":237,"p50":5885,"p999":10383,"sum":1298447,"p95":10200,"p75":8240}
fast6,stats={"p99":10346,"avg":5482.53164556962,"max":10391,"p90":9733,"count":316,"p50":5866,"p999":10386,"sum":1732480,"p95":10171,"p75":8300}
fast5,stats={"p99":10356,"avg":5486.577494692145,"max":10396,"p90":9711,"count":471,"p50":5880,"p999":10391,"sum":2584178,"p95":10180,"p75":8283}
fast4,stats={"p99":10354,"avg":5491.987110633727,"max":10393,"p90":9726,"count":931,"p50":5884,"p999":10395,"sum":5113040,"p95":10171,"p75":8290}
slow2,stats={"p99":10350,"avg":5131.4225,"max":10389,"p90":9533,"count":800,"p50":5360,"p999":10393,"sum":4105138,"p95":10085,"p75":7975}
fast8,stats={"p99":10355,"avg":5475.642105263158,"max":10390,"p90":9700,"count":190,"p50":5866,"p999":10377,"sum":1040372,"p95":10150,"p75":8250}
slow1,stats={"p99":10355,"avg":5491.793991416309,"max":10427,"p90":9723,"count":932,"p50":5904,"p999":10395,"sum":5118352,"p95":10180,"p75":8285}
fast3,stats={"p99":10353,"avg":5488.774436090225,"max":10389,"p90":9730,"count":931,"p50":5890,"p999":10395,"sum":5110049,"p95":10163,"p75":8290}
fast2,stats={"p99":10356,"avg":5495.666309012876,"max":10429,"p90":9736,"count":932,"p50":5895,"p999":10395,"sum":5121961,"p95":10190,"p75":8300}
fast1,stats={"p99":10355,"avg":5495.532089961602,"max":10402,"p90":9738,"count":1823,"p50":5897,"p999":10395,"sum":10018355,"p95":10169,"p75":8294}
======detail======
fast7|null,stats={"p99":10350,"avg":5478.679324894515,"max":10377,"p90":9720,"count":237,"p50":5885,"p999":10383,"sum":1298447,"p95":10200,"p75":8240}
fast6|null,stats={"p99":10346,"avg":5482.53164556962,"max":10391,"p90":9733,"count":316,"p50":5866,"p999":10386,"sum":1732480,"p95":10171,"p75":8300}
fast5|null,stats={"p99":10356,"avg":5486.577494692145,"max":10396,"p90":9711,"count":471,"p50":5880,"p999":10391,"sum":2584178,"p95":10180,"p75":8283}
fast4|null,stats={"p99":10354,"avg":5491.987110633727,"max":10393,"p90":9726,"count":931,"p50":5884,"p999":10395,"sum":5113040,"p95":10171,"p75":8290}
slow2|null,stats={"p99":10350,"avg":5131.4225,"max":10389,"p90":9533,"count":800,"p50":5360,"p999":10393,"sum":4105138,"p95":10085,"p75":7975}
fast8|null,stats={"p99":10355,"avg":5475.642105263158,"max":10390,"p90":9700,"count":190,"p50":5866,"p999":10377,"sum":1040372,"p95":10150,"p75":8250}
slow1|null,stats={"p99":10355,"avg":5491.793991416309,"max":10427,"p90":9723,"count":932,"p50":5904,"p999":10395,"sum":5118352,"p95":10180,"p75":8285}
fast3|null,stats={"p99":10353,"avg":5488.774436090225,"max":10389,"p90":9730,"count":931,"p50":5890,"p999":10395,"sum":5110049,"p95":10163,"p75":8290}
fast2|null,stats={"p99":10356,"avg":5495.666309012876,"max":10429,"p90":9736,"count":932,"p50":5895,"p999":10395,"sum":5121961,"p95":10190,"p75":8300}
fast1|null,stats={"p99":10355,"avg":5495.532089961602,"max":10402,"p90":9738,"count":1823,"p50":5897,"p999":10395,"sum":10018355,"p95":10169,"p75":8294}
======type======
```
结果分析：
* 使用普通线程池ThreadPoolExecutor的场景下，响应慢的租户影响到了响应快的租户，所有租户一视同仁，因此所有任务都有较高的延迟
* 使用CamelliaDynamicIsolationExecutor的场景下，响应慢的租户被自动隔离了，因此响应快的租户可以保持任务执行延迟保持在较低水平而不受响应慢的租户的影响  
