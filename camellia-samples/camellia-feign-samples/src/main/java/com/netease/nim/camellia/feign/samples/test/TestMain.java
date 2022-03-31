package com.netease.nim.camellia.feign.samples.test;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.feign.CamelliaFeignClientFactory;
import com.netease.nim.camellia.feign.samples.ITestFeignService;
import com.netease.nim.camellia.feign.samples.User;
import com.netease.nim.camellia.feign.samples.UserResponse;

/**
 * Created by caojiajun on 2022/3/30
 */
public class TestMain {

    public static void main(String[] args) {
        CamelliaFeignClientFactory factory = new CamelliaFeignClientFactory();
        ITestFeignService service = factory.getService(ITestFeignService.class);
        User user = new User();
        user.setUid(123);
        UserResponse response = service.getUser(user);
        System.out.println(JSONObject.toJSON(response));
    }
}
