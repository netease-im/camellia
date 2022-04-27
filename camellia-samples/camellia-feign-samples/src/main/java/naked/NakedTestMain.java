package naked;

import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClient;

/**
 * Created by caojiajun on 2022/4/25
 */
public class NakedTestMain {
    public static void main(String[] args) {
        CamelliaNakedClient<Long, String> client = new CamelliaNakedClient.Builder()
                .bid(1L)
                .bgroup("default")
                .resourceTable("feign#http://baidu.com")
                .dynamicOptionGetter(new CamelliaFeignDynamicOptionGetter.DefaultCamelliaFeignDynamicOptionGetter(1000, true))
                .build((feignResource, request) -> {
                    System.out.println("feignResource=" + feignResource.getFeignUrl());
                    if (request == 100L) {
                        throw new IllegalArgumentException("");
                    }
                    return "success,request=" + request;
                }, t -> "fail,FALLBACK");
        //fallback
        String response1 = client.sendRequest(CamelliaNakedClient.OperationType.READ, 100L);
        System.out.println(response1);

        //success
        String response2 = client.sendRequest(CamelliaNakedClient.OperationType.READ, 200L);
        System.out.println(response2);
    }
}
