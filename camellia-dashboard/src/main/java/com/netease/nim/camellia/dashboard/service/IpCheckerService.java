package com.netease.nim.camellia.dashboard.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.dashboard.daowrapper.IpCheckerDaoWrapper;
import com.netease.nim.camellia.dashboard.dto.CreateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.dto.UpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.dashboard.util.IpCheckerUtil;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.util.PageCriteriaPageableUtil;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Service
public class IpCheckerService implements IIpCheckerService {

    @Autowired
    private IpCheckerDaoWrapper ipCheckerDao;

    private final CamelliaLocalCache camelliaLocalCache = new CamelliaLocalCache(5);

    @Override
    public DataWithMd5Response<List<IpCheckerDto>> getList(String md5) {
        DataWithMd5Response<List<IpCheckerDto>> response = new DataWithMd5Response<>();
        List<IpCheckerDto> data = null;
        //add local cache
        String newMd5 = camelliaLocalCache.get("md5", "ip-checker", String.class);
        if (newMd5 == null) {
            data = getData();
            newMd5 = MD5Util.md5(JSONObject.toJSONString(data));
            camelliaLocalCache.put("md5", "ip-checker", newMd5, 3);
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

    private List<IpCheckerDto> getData() {
        List<IpChecker> list = ipCheckerDao.getList();
        List<IpCheckerDto> data = new ArrayList<>();
        for (IpChecker ipChecker : list) {
            data.add(IpCheckerUtil.convertToDto(ipChecker));
        }
        return data;
    }

    @Override
    public IpChecker findById(Long id) {
        IpChecker ipChecker = ipCheckerDao.findById(id);
        if (ipChecker == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "IpChecker is not existed");
        }
        return ipChecker;
    }

    @Override
    public Page<IpChecker> findIpCheckers(Long bid, String bgroup, IpCheckMode mode, String ip, PageCriteria pageCriteria) {
        Pageable pageable = PageCriteriaPageableUtil.toPageable(pageCriteria);
        return ipCheckerDao.findAllBy(bid, bgroup, mode, ip, pageable);
    }

    @Override
    public IpChecker create(CreateIpCheckerRequest request) {
        if (!IpCheckerUtil.isValidIpList(request.getIpList())) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "ipList is invalid");
        }

        boolean isExisted = ipCheckerDao.existByBidAndBgroup(request.getBid(), request.getBgroup());
        if (isExisted) {
            throw new AppException(CamelliaApiCode.CONFLICT.getCode(), "bid and bgroup is existed");
        }
        IpChecker ipChecker = IpCheckerUtil.convertToModel(request);
        ipCheckerDao.create(ipChecker);
        return ipChecker;
    }

    @Override
    public IpChecker update(Long id, UpdateIpCheckerRequest request) {
        if (!IpCheckerUtil.isValidIpList(request.getIpList())) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "ipList is invalid");
        }

        IpChecker ipChecker = findById(id);

        ipChecker.setMode(request.getMode());
        ipChecker.setIpList(request.getIpList());
        ipCheckerDao.update(ipChecker);
        return ipChecker;
    }

    @Override
    public void delete(Long id) {
        boolean isExisted = ipCheckerDao.existById(id);
        if (!isExisted) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "IpChecker is not existed");
        }
        ipCheckerDao.delete(id);
    }
}
