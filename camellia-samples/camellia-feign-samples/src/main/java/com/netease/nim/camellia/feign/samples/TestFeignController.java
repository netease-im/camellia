package com.netease.nim.camellia.feign.samples;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by caojiajun on 2022/3/30
 */
@RestController
public class TestFeignController {

    @Autowired
    private ITestFeignService testFeignService;
    private static final AtomicLong id = new AtomicLong(0);

    @PostMapping("/getUser")
    public UserResponse getUser(@RequestBody User user) {
        if (user.getTimeout() > 0) {
            try {
                Thread.sleep(user.getTimeout());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!user.isError()) {
            user.setName("zhangsan");
            user.setExt("ext123");
            UserResponse response = new UserResponse();
            response.setUser(user);
            System.out.println("getUser=" + JSONObject.toJSONString(response));
            return response;
        }
        if (id.incrementAndGet() % 3 != 0) {//1次成功2次失败
            throw new RuntimeException();
        }
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
    public UserResponse getUser2(@RequestParam(defaultValue = "0") int type,
                                 @RequestParam(defaultValue = "-1") int timeout,
                                 @RequestParam(defaultValue = "false") boolean error) {
        User user = new User();
        user.setUid(111);
        user.setTimeout(timeout);
        user.setError(error);
        UserResponse response;
        if (type == 0) {
            System.out.println("getUser");
            response = testFeignService.getUser(user);
        } else if (type == 1) {
            System.out.println("getUserNoRetry");
            response = testFeignService.getUserNoRetry(user);
        } else if (type == 2) {
            System.out.println("getUserByServiceRetry");
            response = testFeignService.getUserByServiceRetry(user);
        } else {
            throw new RuntimeException();
        }
        System.out.println("getUser2=" + JSONObject.toJSONString(response));
        return response;
    }
}
