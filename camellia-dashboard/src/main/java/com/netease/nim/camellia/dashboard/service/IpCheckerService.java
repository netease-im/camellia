package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.dashboard.constant.IpCheckMode;
import com.netease.nim.camellia.dashboard.daowrapper.IpCheckerDaoWrapper;
import com.netease.nim.camellia.dashboard.dto.CreateOrUpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.dashboard.util.IpCheckerUtil;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.dashboard.util.PageCriteriaPageableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Service
public class IpCheckerService implements IIpCheckerService {

    @Autowired
    private IpCheckerDaoWrapper ipCheckerDao;

    @Override
    public DataWithMd5Response<List<IpChecker>> getList(String md5) {
        DataWithMd5Response<List<IpChecker>> response = new DataWithMd5Response<>();

        List<IpChecker> list;
        try {
            list = ipCheckerDao.getList();
        } catch (AppException e) {
            response.setCode(e.getCode());
            return response;
        }
        String dataString = list.toString();
        String newMd5 = MD5Util.md5(dataString);
        if (newMd5.equals(md5)) {
            LogBean.get().addProps("not.modify", true);
            response.setCode(CamelliaApiCode.NOT_MODIFY.getCode());
            return response;
        }
        response.setCode(CamelliaApiCode.SUCCESS.getCode());
        response.setMd5(newMd5);
        response.setData(list);
        return response;
    }

    @Override
    public IpChecker findById(Long id) {
        return ipCheckerDao.findById(id);
    }

    @Override
    public Page<IpChecker> findIpCheckers(Long bid, String bgroup, IpCheckMode mode, String ip, PageCriteria pageCriteria) {
        Pageable pageable = PageCriteriaPageableUtil.toPageable(pageCriteria);
        return ipCheckerDao.findAllBy(bid, bgroup, mode, ip, pageable);
    }

    @Override
    public IpChecker create(CreateOrUpdateIpCheckerRequest request) {
        if (!IpCheckerUtil.checkValidIpList(request.getIpList())) {
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
    public IpChecker update(Long id, CreateOrUpdateIpCheckerRequest request) {
        if (!IpCheckerUtil.checkValidIpList(request.getIpList())) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "ipList is invalid");
        }

        IpChecker ipChecker = ipCheckerDao.findById(id);
        if (ipChecker == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "IpChecker is not existed");
        }
        ipChecker.setBid(request.getBid());
        ipChecker.setBgroup(request.getBgroup());
        ipChecker.setMode(request.getMode());
        ipChecker.setIpList(request.getIpList());
        ipCheckerDao.update(ipChecker);
        return ipChecker;
    }
}
