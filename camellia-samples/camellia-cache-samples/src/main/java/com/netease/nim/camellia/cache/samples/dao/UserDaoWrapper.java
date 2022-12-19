package com.netease.nim.camellia.cache.samples.dao;

import com.netease.nim.camellia.cache.core.CamelliaCacheName;
import com.netease.nim.camellia.cache.samples.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 使用本地缓存
 * Created by caojiajun on 2019/9/19.
 */
@Service
@CacheConfig(cacheNames = CamelliaCacheName.LOCAL_SECOND_5_CACHE_NULL)
public class UserDaoWrapper {

    @Autowired
    private UserDao userDao;

    @Cacheable(key = "'local_user_'.concat(#appid).concat('_').concat(#accid)")
    public User get(long appid, String accid) {
        return userDao.get(appid, accid);
    }
}
