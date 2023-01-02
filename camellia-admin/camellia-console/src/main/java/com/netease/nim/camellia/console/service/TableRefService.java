package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.service.bo.TableRefBO;
import com.netease.nim.camellia.console.service.vo.CamelliaTableRefPage;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface TableRefService {

    TableRefBO bindBigBgroupWithTid(Long dashboardId, long bid, String bgroup, long tid, String info);

    void delete(Long dashboardId,long bid, String bgroup);

    CamelliaTableRefPage getTableRef(Long did, Long bid, String bgroup, Long tid, Integer validFlag, int pageNum, int pageSize,String info,String resourceInfo);
}
