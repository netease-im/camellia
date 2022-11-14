package com.netease.nim.camellia.dashboard.daowrapper;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.dashboard.dao.IpCheckerDao;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Service
public class IpCheckerDaoWrapper {

    private static final String TAG = "camellia_ip_checker";
    private static final String IP_CHECKER_ALL_CACHE_KEY = TAG + "_all";

    @Autowired
    private IpCheckerDao ipCheckerDao;

    @Autowired
    private CamelliaRedisTemplate template;


    public List<IpChecker> getList() {
        String value = template.get(IP_CHECKER_ALL_CACHE_KEY);
        // The 1st time, fetch data from the database and update the redis cache
        if (value == null) {
            return updateCache();
        }
        if (value.length() > 0) {
            return JSONArray.parseArray(value, IpChecker.class);
        }
        return Collections.emptyList();
    }

    public IpChecker findById(Long id) {
        Optional<IpChecker> optional = ipCheckerDao.findById(id);
        return optional.orElse(null);
    }

    public Page<IpChecker> findAllBy(Long bid, String bgroup, IpCheckMode mode, String ip, Pageable pageable) {
        Integer modeValue = mode != null ? mode.getValue() : null;
        return ipCheckerDao.findAllBy(bid, bgroup, modeValue, ip, pageable);
    }

    public boolean existByBidAndBgroup(Long bid, String bgroup) {
        return ipCheckerDao.existsByBidAndBgroup(bid, bgroup);
    }

    public int create(IpChecker ipChecker) {
        long now = System.currentTimeMillis();
        ipChecker.setCreateTime(now);
        ipChecker.setUpdateTime(now);
        IpChecker save = ipCheckerDao.save(ipChecker);
        ipChecker.setId(save.getId());
        ipChecker.setCreateTime(save.getCreateTime());
        ipChecker.setUpdateTime(save.getUpdateTime());
        updateCache();
        return 1;
    }

    public int update(IpChecker ipChecker) {
        long now = System.currentTimeMillis();
        ipChecker.setUpdateTime(now);
        IpChecker save = ipCheckerDao.save(ipChecker);
        ipChecker.setUpdateTime(save.getUpdateTime());
        updateCache();
        return 1;
    }

    public boolean existById(Long id) {
        return ipCheckerDao.existsById(id);
    }

    public void delete(Long id) {
        ipCheckerDao.deleteById(id);
        updateCache();
    }

    /**
     * Update redis cache anytime the data in the database changes,
     * so that the data in the cache is always consistent with the data in the database
     * Key: camellia_ip_checker_all
     * Value: JSONArray
     *
     * @return List of IpChecker
     */
    private List<IpChecker> updateCache() {
        List<IpChecker> list = ipCheckerDao.findAll();
        // Convert list to JSONArray
        JSONArray json = new JSONArray();
        json.addAll(list);
        String value = json.toJSONString();
        template.set(IP_CHECKER_ALL_CACHE_KEY, value);
        return list;
    }

}
