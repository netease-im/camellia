package com.netease.nim.camellia.dashboard.service;

import com.alibaba.fastjson.JSON;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.constant.RateLimitConstant;
import com.netease.nim.camellia.core.constant.TenantConstant;
import com.netease.nim.camellia.core.model.RateLimitDto;
import com.netease.nim.camellia.tools.utils.MD5Util;
import com.netease.nim.camellia.dashboard.daowrapper.RateLimitDaoWrapper;
import com.netease.nim.camellia.dashboard.dto.CreateRateLimitRequest;
import com.netease.nim.camellia.dashboard.dto.UpdateRateLimitRequest;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.RateLimit;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.util.PageCriteriaPageableUtil;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author anhdt9
 * @since 19/12/2022
 */
@Service
public class RateLimitService implements IRateLimitService {
    @Autowired
    private RateLimitDaoWrapper rateLimitDao;

    private final CamelliaLocalCache camelliaLocalCache = new CamelliaLocalCache(5);


    @Override
    public DataWithMd5Response<List<RateLimitDto>> getList(String md5) {
        DataWithMd5Response<List<RateLimitDto>> response = new DataWithMd5Response<>();
        List<RateLimitDto> data = null;
        //add local cache
        String newMd5 = camelliaLocalCache.get("md5", "rate-limit", String.class);
        if (newMd5 == null) {
            data = getData();
            newMd5 = MD5Util.md5(JSON.toJSONString(data));
            camelliaLocalCache.put("md5", "rate-limit", newMd5, 3);
        }
        if (newMd5.equals(md5)) {
            LogBean.get().addProps("not.modify", true);
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        if (data == null) {
            data = getData();
        }
        response.setCode(CamelliaApiCode.SUCCESS.getCode());
        response.setMd5(newMd5);
        response.setData(data);
        return response;
    }

    private List<RateLimitDto> getData() {
        List<RateLimit> list = rateLimitDao.getList();
        List<RateLimitDto> data = new ArrayList<>();
        for (RateLimit entity : list) {
            RateLimitDto dto = new RateLimitDto();
            dto.setBid(entity.getBid());
            dto.setBgroup(entity.getBgroup());
            dto.setCheckMillis(entity.getCheckMillis());
            dto.setMaxCount(entity.getMaxCount());
            data.add(dto);
        }
        return data;
    }

    @Override
    public RateLimit findById(long id) {
        RateLimit rateLimit = rateLimitDao.findById(id);
        throwIfNotExist(rateLimit);
        return rateLimit;
    }

    @Override
    public Page<RateLimit> searchRateLimitConfigurations(Long bid, String bgroup, PageCriteria pageCriteria) {
        Pageable pageable = PageCriteriaPageableUtil.toPageable(pageCriteria);
        return rateLimitDao.findAllBy(bid, bgroup, pageable);
    }

    @Override
    public RateLimit create(CreateRateLimitRequest request) {
        long bid = request.getBid();
        String bgroup = request.getBgroup();
        long checkMillis = request.getCheckMillis();
        long maxCount = request.getMaxCount();

        throwIfInvalidTenant(bid, bgroup);
        throwIfTenantAlreadyHasRateLimitConf(bid, bgroup);

        if (checkMillis <= 0) {
            checkMillis = RateLimitConstant.DEFAULT_CHECK_MILLIS;
        }
        if (maxCount < 0) {
            maxCount = RateLimitConstant.DEFAULT_MAX_COUNT;
        }

        RateLimit rateLimit = new RateLimit();
        rateLimit.setBid(bid);
        rateLimit.setBgroup(bgroup);
        rateLimit.setCheckMillis(checkMillis);
        rateLimit.setMaxCount(maxCount);

        rateLimitDao.create(rateLimit);

        return rateLimit;
    }

    private void throwIfTenantAlreadyHasRateLimitConf(long bid, String bgroup) {
        boolean isExisted = rateLimitDao.existsByBidAndAndBgroup(bid, bgroup);
        if (isExisted) {
            throw new AppException(CamelliaApiCode.CONFLICT.getCode(), "Tenant already has Rate Limit configuration");
        }
    }


    @Override
    public void update(long id, UpdateRateLimitRequest request) {
        RateLimit rateLimit = findById(id);

        Long checkMillis = request.getCheckMillis();
        Long maxCount = request.getMaxCount();

        if (checkMillis != null) {
            if (checkMillis <= 0) {
                checkMillis = RateLimitConstant.DEFAULT_CHECK_MILLIS;
            }
            rateLimit.setCheckMillis(checkMillis);
        }
        if (maxCount != null) {
            if (maxCount < 0) {
                maxCount = RateLimitConstant.DEFAULT_MAX_COUNT;
            }
            rateLimit.setMaxCount(maxCount);
        }

        rateLimitDao.update(rateLimit);
    }

    @Override
    public void delete(long id) {
        throwIfNotExist(id);
        rateLimitDao.delete(id);
    }

    private void throwIfNotExist(long id) {
        boolean isExisted = rateLimitDao.existsById(id);
        if (!isExisted) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "Rate Limit configuration is not existed");
        }
    }

    private void throwIfInvalidTenant(long bid, String bgroup) {
        if (bid <= 0 && bid != TenantConstant.DEFAULT_BID && bid != TenantConstant.GLOBAL_BID) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "bid is required");
        }
        if (StringUtils.isEmpty(bgroup)) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "bgroup is required");
        }
    }

    private void throwIfNotExist(RateLimit rateLimit) {
        if (rateLimit == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "Rate Limit configuration is not existed");
        }
    }
}
