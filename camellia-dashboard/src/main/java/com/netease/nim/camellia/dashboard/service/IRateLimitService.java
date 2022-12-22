package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.model.RateLimitDto;
import com.netease.nim.camellia.dashboard.dto.CreateRateLimitRequest;
import com.netease.nim.camellia.dashboard.dto.UpdateRateLimitRequest;
import com.netease.nim.camellia.dashboard.model.RateLimit;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
public interface IRateLimitService {
    DataWithMd5Response<List<RateLimitDto>> getList(String md5);

    RateLimit findById(long id);

    Page<RateLimit> searchRateLimitConfigurations(Long bid, String bgroup, PageCriteria pageCriteria);

    RateLimit create(CreateRateLimitRequest request);

    /**
     * Optional update Rate Limit configuration
     *
     * @param id
     * @param request
     */
    void update(long id, UpdateRateLimitRequest request);

    void delete(long id);
}
