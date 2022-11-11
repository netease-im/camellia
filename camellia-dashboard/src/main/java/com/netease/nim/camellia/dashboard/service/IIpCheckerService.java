package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.dashboard.constant.IpCheckMode;
import com.netease.nim.camellia.dashboard.dto.CreateOrUpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
public interface IIpCheckerService {
    DataWithMd5Response<List<IpChecker>> getList(String md5);

    IpChecker findById(Long id);

    Page<IpChecker> findIpCheckers(Long bid, String bgroup, IpCheckMode mode, String ip, PageCriteria pageCriteria);

    IpChecker create(CreateOrUpdateIpCheckerRequest ipChecker);

    IpChecker update(Long id, CreateOrUpdateIpCheckerRequest request);
}
