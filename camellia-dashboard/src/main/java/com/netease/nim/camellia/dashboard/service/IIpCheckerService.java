package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.dashboard.dto.CreateOrUpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
public interface IIpCheckerService {
    DataWithMd5Response<List<IpCheckerDto>> getList(String md5);

    IpChecker findById(Long id);

    Page<IpChecker> findIpCheckers(Long bid, String bgroup, IpCheckMode mode, String ip, PageCriteria pageCriteria);

    IpChecker create(CreateOrUpdateIpCheckerRequest ipChecker);

    IpChecker update(Long id, CreateOrUpdateIpCheckerRequest request);

    void delete(Long id);
}
