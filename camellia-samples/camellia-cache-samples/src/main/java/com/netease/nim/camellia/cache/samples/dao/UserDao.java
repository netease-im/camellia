package com.netease.nim.camellia.cache.samples.dao;

import com.netease.nim.camellia.cache.core.CamelliaCacheName;
import com.netease.nim.camellia.cache.samples.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用分布式缓存
 * Created by caojiajun on 2019/9/19.
 */
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL})
public class UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private static final Map<Long, Map<String, User>> map = new HashMap<>();

    public UserDao() {
        long appid1 = 100;
        Map<String, User> map1 = new HashMap<>();
        map1.put("acc1", new User(appid1, "acc1", 10));
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<2000; i++) {
            builder.append("a");
        }
        map1.get("acc1").setExt(builder.toString());
        map1.put("acc2", new User(appid1, "acc2", 12));
        map1.put("acc3", new User(appid1, "acc3", 15));
        map.put(appid1, map1);
        long appid2 = 200;
        Map<String, User> map2 = new HashMap<>();
        map2.put("acc1", new User(appid2, "acc1", 110));
        map2.put("acc2", new User(appid2, "acc2", 112));
        map2.put("acc3", new User(appid2, "acc3", 115));
        map.put(appid2, map2);
    }

    /**
     * sync=true表示缓存穿透的情况下，只允许一个请求去底层捞数据
     * 其他的会等待获取到锁的请求操作完成缓存回填完成后，直接从缓存里取；若等待一段时间之后缓存中依然没有数据，此时会穿透到底层去捞数据
     * @param appid appid
     * @param accid accid
     * @return result
     */
    @Cacheable(key = "'user_'.concat(#appid).concat('_').concat(#accid)", sync = true)
    public User get(long appid, String accid) {
        Map<String, User> subMap = map.get(appid);
        User ret = null;
        if (subMap != null) {
            ret = subMap.get(accid);
        }
        logger.info("get user, appid = {}, accid = {}, ret = {}", appid, accid, ret);
        return ret;
    }

    /**
     * mget操作
     * 1表示第2个参数是一个List类型的参数（0开始算）
     * (user_'.concat(#appid).concat('_)(#.*) 表示了mget时的key的拼装规则，括号分隔，#是参数里的List<String> accidList
     * (user_)(#.appid)(_)(#.accid) 表示了缓存回填时的key的拼装规则，括号分隔，#是方法返回的List<User>
     *
     * 备注：#.*表示List中的元素，#.appid表示List中的元素的appid字段
     *
     * @param appid appid
     * @param accidList accidList
     * @return result
     */
    @Cacheable(key = "'mget|1|(user_'.concat(#appid).concat('_)(#.*)|(user_)(#.appid)(_)(#.accid)')")
    public List<User> getBatch(long appid, List<String> accidList) {
        List<User> list = new ArrayList<>();
        Map<String, User> subMap = map.get(appid);
        if (subMap != null) {
            for (String accid : accidList) {
                User user = subMap.get(accid);
                if (user != null) {
                    list.add(user);
                }
            }
        }
        logger.info("get user, appid = {}, accidList = {}", appid, accidList);
        return list;
    }

    @CacheEvict(key = "'user_'.concat(#appid).concat('_').concat(#accid)")
    public int delete(long appid, String accid) {
        Map<String, User> subMap = map.get(appid);
        int ret = 0;
        if (subMap != null) {
            ret = subMap.remove(accid) != null ? 1 : 0;
        }
        logger.info("delete user, appid = {}, accid = {}, ret = {}", appid, accid, ret);
        return ret;
    }


    @CacheEvict(key = "'mevict|1|(user_)('.concat(#appid).concat('_)(#.*)')")
    public int deleteBatch(long appid, List<String> accidList) {
        int ret = 0;
        Map<String, User> subMap = map.get(appid);
        if (subMap != null) {
            for (String accid : accidList) {
                ret += subMap.remove(accid) != null ? 1 : 0;
            }
        }
        logger.info("delete user, appid = {}, accidList = {}, ret = {}", appid, accidList, ret);
        return ret;
    }

    @CacheEvict(key = "'user_'.concat(#user.appid).concat('_').concat(#user.accid)")
    public int update(User user) {
        Map<String, User> subMap = map.get(user.getAppid());
        int ret = 0;
        if (subMap != null) {
            User oldUser = subMap.get(user.getAccid());
            if (oldUser != null) {
                subMap.put(user.getAccid(), user);
                ret = 1;
            }
        }
        logger.info("update user, user = {}, ret = {}", user, ret);
        return ret;
    }

    /**
     * mevict操作
     * 0表示第1个参数是List类型的参数（0开始算）
     * (user_)(#.appid)(_)(#.accid) 表示了mevict的key的拼装规则，#是参数里的List<User> userList
     *
     * 备注：#.*表示List中的元素，#.appid表示List中的元素的appid字段

     * @param userList userList
     * @return result
     */
    @CacheEvict(key = "'mevict|0|(user_)(#.appid)(_)(#.accid)'")
    public int updateBatch(List<User> userList) {
        int ret = 0;
        for (User user : userList) {
            Map<String, User> subMap = map.get(user.getAppid());
            if (subMap != null) {
                User oldUser = subMap.get(user.getAccid());
                if (oldUser != null) {
                    subMap.put(user.getAccid(), user);
                    ret += 1;
                }
            }
        }
        logger.info("update user, userList = {}, ret = {}", userList, ret);
        return ret;
    }

    @CacheEvict(key = "'user_'.concat(#user.appid).concat('_').concat(#user.accid)")
    public int insert(User user) {
        Map<String, User> subMap = map.computeIfAbsent(user.getAppid(), k -> new HashMap<>());
        subMap.put(user.getAccid(), user);
        logger.info("insert user, user = {}, ret = {}", user, 1);
        return 1;
    }

    @CacheEvict(key = "'mevict|0|(user_)(#.appid)(_)(#.accid)'")
    public int insertBatch(List<User> userList) {
        for (User user : userList) {
            Map<String, User> subMap = map.computeIfAbsent(user.getAppid(), k -> new HashMap<>());
            subMap.put(user.getAccid(), user);
        }
        int ret = userList.size();
        logger.info("insert user, userList = {}, ret = {}", userList, ret);
        return ret;
    }
}
