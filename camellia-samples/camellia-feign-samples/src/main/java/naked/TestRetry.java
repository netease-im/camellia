package naked;

import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.feign.CamelliaFeignEnv;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClient;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClientFailureContext;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClientFailureListener;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientException;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/8/20
 */
public class TestRetry {

    private static final ConcurrentHashMap<String, AtomicLong> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        String url1 = "feign#http://baidu.com";
        String url2 = "feign#http://163.com";

        test(MultiWriteType.SINGLE_THREAD, url1, url2);
        test(MultiWriteType.MULTI_THREAD_CONCURRENT, url1, url2);
        test(MultiWriteType.ASYNC_MULTI_THREAD, url1, url2);
        test(MultiWriteType.MISC_ASYNC_MULTI_THREAD, url1, url2);

        System.out.println("SUCCESS");
        Thread.sleep(100);
        System.exit(-1);
    }

    private static void test(MultiWriteType multiWriteType, String url1, String url2) {
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
                    if (feignResource.getUrl().equals(url1) && result == 300) {
                        throw new RuntimeException("error1");
                    }
                    if (feignResource.getUrl().equals(url2) && result == 600) {
                        throw new RuntimeException("error2");
                    }
                    System.out.println(Thread.currentThread().getName() + ",result=" + result);
                    return String.valueOf(result);
                }, context -> {
                    Long request = context.getRequest();
                    Resource resource = context.getResource();
                    System.out.println("url=" + resource.getUrl() + ",fail=" + request);
                    if (resource.getUrl().equals(url1)) {
                        assertEquals(request, 200L);
                        String message = context.getException().getMessage();
                        assertEquals(message, "error1");
                        return;
                    }
                    if (resource.getUrl().equals(url2)) {
                        assertEquals(request, 300L);
                        String message = context.getException().getMessage();
                        assertEquals(message, "error2");
                        return;
                    }
                    System.out.println("FAIL");
                    System.exit(-1);
                });

        try {
            String s = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, 100L);
            assertEquals(s, "100");
        } catch (Exception e) {
            System.out.println("FAIL");
            System.exit(-1);
        }
        try {
            String s = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, 200L);
            assertEquals(s, "300");
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "error1");
        }
        try {
            String s = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, 300L);
            assertEquals(s, "600");
        } catch (Exception e) {
            assertEquals(e.getCause().getMessage(), "error2");
        }
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
