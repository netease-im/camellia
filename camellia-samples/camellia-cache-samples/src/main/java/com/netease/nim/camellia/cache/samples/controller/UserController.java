package com.netease.nim.camellia.cache.samples.controller;

import com.netease.nim.camellia.cache.samples.dao.UserDao;
import com.netease.nim.camellia.cache.samples.dao.UserDaoWrapper;
import com.netease.nim.camellia.cache.samples.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Created by caojiajun on 2019/9/19.
 */
@RestController
public class UserController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserDaoWrapper userDaoWrapper;

    @RequestMapping("/ping")
    public String ping(@RequestParam String ping) {
        return ping;
    }

    @RequestMapping("/getUser")
    public User get(@RequestParam long appid,
                    @RequestParam String accid,
                    @RequestParam(required = false, defaultValue = "false") boolean localCache) {
        if (localCache) {
            return userDaoWrapper.get(appid, accid);
        } else {
            return userDao.get(appid, accid);
        }
    }

    @RequestMapping("/getUserBatch")
    public List<User> getUserBatch(@RequestParam long appid,
                                   @RequestParam String accids) {
        return userDao.getBatch(appid, Arrays.asList(accids.split(",")));
    }

    @RequestMapping("/deleteUser")
    public int delete(@RequestParam long appid,
                      @RequestParam String accid) {
        return userDao.delete(appid, accid);
    }

    @RequestMapping("/deleteUserBatch")
    public int deleteBatch(@RequestParam long appid,
                           @RequestParam String accids) {
        return userDao.deleteBatch(appid, Arrays.asList(accids.split(",")));
    }

    @RequestMapping("/insertUser")
    public int insert(@RequestParam long appid,
                      @RequestParam String accid,
                      @RequestParam int age) {
        return userDao.insert(new User(appid, accid, age));
    }

    @RequestMapping("/insertUserBatch")
    public int insertBatch(List<User> userList) {
        return userDao.insertBatch(userList);
    }

    @RequestMapping("/insertUserBatch2")
    public int insertBatch2(@RequestParam long appid,
                            @RequestParam String accids,
                            @RequestParam int age) {
        List<User> userList = new ArrayList<>();
        for (String accid : Arrays.asList(accids.split(","))) {
            userList.add(new User(appid, accid, age));
        }
        return userDao.insertBatch(userList);
    }

    @RequestMapping("/updateUser")
    public int update(@RequestParam long appid,
                      @RequestParam String accid,
                      @RequestParam int age) {
        return userDao.update(new User(appid, accid, age));
    }

    @RequestMapping("/updateUserBatch")
    public int updateBatch(List<User> userList) {
        return userDao.updateBatch(userList);
    }

    @RequestMapping("/updateUserBatch2")
    public int updateBatch(@RequestParam long appid,
                           @RequestParam String accids,
                           @RequestParam int age) {
        List<User> userList = new ArrayList<>();
        for (String accid : Arrays.asList(accids.split(","))) {
            userList.add(new User(appid, accid, age));
        }
        return userDao.updateBatch(userList);
    }
}
