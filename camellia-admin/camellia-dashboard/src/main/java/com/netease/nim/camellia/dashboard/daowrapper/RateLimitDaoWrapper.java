package com.netease.nim.camellia.dashboard.daowrapper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.dashboard.dao.RateLimitDao;
import com.netease.nim.camellia.dashboard.model.RateLimit;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Service
public class RateLimitDaoWrapper {

    private static final String TAG = "camellia_rate_limit";
    private static final String RATE_LIMIT_ALL_CACHE_KEY = TAG + "_all";

    @Autowired
    private RateLimitDao rateLimitDao;

    @Autowired
    private CamelliaRedisTemplate template;


    public RateLimit findById(long id) {
        return rateLimitDao.findById(id).orElse(null);
    }

    public int create(RateLimit rateLimit) {
        long currentTimeMillis = System.currentTimeMillis();
        rateLimit.setCreateTime(currentTimeMillis);
        rateLimit.setUpdateTime(currentTimeMillis);
        RateLimit save = rateLimitDao.save(rateLimit);
        rateLimit.setId(save.getId());
        updateCache();
        return 1;
    }

    public int update(RateLimit rateLimit) {
        long currentTimeMillis = System.currentTimeMillis();
        rateLimit.setUpdateTime(currentTimeMillis);
        rateLimitDao.save(rateLimit);
        updateCache();
        return 1;
    }

    public boolean existsById(long id) {
        return rateLimitDao.existsById(id);
    }

    public void delete(long id) {
        rateLimitDao.deleteById(id);
        updateCache();
    }

    public Page<RateLimit> findAllBy(Long bid, String bgroup, Pageable pageable) {
        return rateLimitDao.findAllByConditions(bid, bgroup, pageable);
    }

    public boolean existsByBidAndAndBgroup(long bid, String bgroup) {
        return rateLimitDao.existsByBidAndAndBgroup(bid, bgroup);
    }

    public List<RateLimit> getList() {
        String value = template.get(RATE_LIMIT_ALL_CACHE_KEY);
        // The 1st time, fetch data from the database and update the redis cache
        if (value == null) {
            return updateCache();
        }
        if (!value.isEmpty()) {
            return JSON.parseArray(value, RateLimit.class);
        }
        return Collections.emptyList();
    }

    /**
     * Update redis cache anytime the data in the database changes,
     * so that the data in the cache is always consistent with the data in the database
     * Key: camellia_rate_limit_all
     * Value: JSONArray
     *
     * @return List of RateLimit
     */
    private List<RateLimit> updateCache() {
        List<RateLimit> list = rateLimitDao.findAll();
        // Convert list to JSONArray
        JSONArray json = new JSONArray();
        json.addAll(list);
        String value = json.toJSONString();
        template.set(RATE_LIMIT_ALL_CACHE_KEY, value);
        return list;
    }
}
