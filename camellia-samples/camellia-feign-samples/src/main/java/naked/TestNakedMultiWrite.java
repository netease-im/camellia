package naked;

import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.feign.CamelliaFeignEnv;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClient;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/8/2
 */
public class TestNakedMultiWrite {

    private static final ConcurrentHashMap<String, AtomicLong> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        String url1 = "feign#http://baidu.com";
        String url2 = "feign#http://163.com";

        test(MultiWriteType.SINGLE_THREAD, url1, url2);
        test(MultiWriteType.MULTI_THREAD_CONCURRENT, url1, url2);
        test(MultiWriteType.ASYNC_MULTI_THREAD, url1, url2);
        test(MultiWriteType.MISC_ASYNC_MULTI_THREAD, url1, url2);

        Thread.sleep(100);
        System.exit(-1);
    }

    private static void test(MultiWriteType multiWriteType, String url1, String url2) throws InterruptedException {
        map.put(url1, new AtomicLong());
        map.put(url2, new AtomicLong());

        ResourceTable resourceTable = ResourceTableUtil.simple2W1RTable(new Resource(url1), new Resource(url1), new Resource(url2));

        ProxyEnv proxyEnv = new ProxyEnv.Builder()
                .multiWriteType(multiWriteType)
                .build();
        CamelliaFeignEnv feignEnv = new CamelliaFeignEnv.Builder()
                .proxyEnv(proxyEnv)
                .build();

        CamelliaNakedClient<Long, String> client = new CamelliaNakedClient.Builder()
                .resourceTable(ReadableResourceTableUtil.readableResourceTable(resourceTable))
                .feignEnv(feignEnv)
                .build((feignResource, request) -> {
                    long result = map.get(feignResource.getUrl()).addAndGet(request);
                    System.out.println(Thread.currentThread().getName() + ",result=" + result);
                    return String.valueOf(result);
                });

        String s = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, 1L);
        assertEquals(s, "1");
        Thread.sleep(100);

        assertEquals(map.get(url1).get(), 1L);
        assertEquals(map.get(url2).get(), 1L);


        String s2 = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, 1L);
        assertEquals(s2, "2");
        Thread.sleep(100);

        assertEquals(map.get(url1).get(), 2L);
        assertEquals(map.get(url2).get(), 2L);
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result + "," +
                    " thread=" + Thread.currentThread().getName());
            throw new RuntimeException();
        }
    }
}
