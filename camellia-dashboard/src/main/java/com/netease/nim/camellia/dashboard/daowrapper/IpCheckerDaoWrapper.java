package com.netease.nim.camellia.dashboard.daowrapper;

import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.dashboard.dao.IpCheckerDao;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Service
public class IpCheckerDaoWrapper {

    private static final String tag = "camellia_ip_checker";

    @Autowired
    private IpCheckerDao ipCheckerDao;

    @Autowired
    private CamelliaRedisTemplate template;


    public List<IpChecker> getList() {
        //TODO: Read from cache
        return ipCheckerDao.findAll();
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
        // TODO: Update cache
        return 1;
    }

    public int update(IpChecker ipChecker) {
        long now = System.currentTimeMillis();
        ipChecker.setUpdateTime(now);
        IpChecker save = ipCheckerDao.save(ipChecker);
        ipChecker.setUpdateTime(save.getUpdateTime());
        // TODO: Update Cache
        return 1;
    }

    public boolean existById(Long id) {
        return ipCheckerDao.existsById(id);
    }

    public void delete(Long id) {
        ipCheckerDao.deleteById(id);
    }
}
