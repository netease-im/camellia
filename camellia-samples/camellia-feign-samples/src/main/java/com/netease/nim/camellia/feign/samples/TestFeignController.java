package com.netease.nim.camellia.feign.samples;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by caojiajun on 2022/3/30
 */
@RestController
public class TestFeignController {

    @Autowired
    private ITestFeignService testFeignService;

    @PostMapping("/getUser")
    public UserResponse getUser(@RequestBody User user) {
        if (user.getUid() == 222) {
            throw new RuntimeException();
        }
        user.setName("zhangsan");
        user.setExt("ext123");
        UserResponse response = new UserResponse();
        response.setUser(user);
        System.out.println("getUser=" + JSONObject.toJSONString(response));
        return response;
    }

    @GetMapping("/getUser2")
    public UserResponse getUser2() {
        User user = new User();
        user.setUid(111);
        UserResponse response = testFeignService.getUser(user);
        System.out.println("getUser2=" + JSONObject.toJSONString(response));
        return response;
    }

    @GetMapping("/getUserFallback")
    public UserResponse getUserFallback() {
        User user = new User();
        user.setUid(222);
        UserResponse response = testFeignService.getUser(user);
        System.out.println("getUser2=" + JSONObject.toJSONString(response));
        return response;
    }
}
