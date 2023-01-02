package com.netease.nim.camellia.console.controller;

import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.constant.ActionRole;
import com.netease.nim.camellia.console.constant.ActionType;
import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.RefHistoryService;
import com.netease.nim.camellia.console.service.TableHistoryService;
import com.netease.nim.camellia.console.service.vo.CamelliaRefHistoryListWithNum;
import com.netease.nim.camellia.console.service.vo.CamelliaTableHistoryListWithNum;
import com.netease.nim.camellia.console.util.LogBean;
import io.netty.util.internal.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Api(value = "查看用户历史操作的接口", tags = {"UserHistoryController"})
@RestController
@ConditionalOnClass(ConsoleProperties.class)
@RequestMapping(value = "/camellia/console/user/history")
public class UserHistoryController {

    @Autowired
    TableHistoryService tableHistoryService;

    @Autowired
    RefHistoryService refHistoryService;



    @ApiOperation(value = "根据tid查询全量的table操作历史", notes = "需要did，tid(可不传递)，当前页和页大小")
    @GetMapping("/tableByTid")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getTableByBidAndBgroup(@RequestParam Long did,
                                 @RequestParam(value = "tid",required = false) Long tid,
                                 @RequestParam Integer pageNum,
                                 @RequestParam Integer pageSize){
        LogBean.get().addProps("did", did);
        if(tid!=null){
            LogBean.get().addProps("tid",tid);

        }
        CamelliaTableHistoryListWithNum tableHistoryByBidAndBgroup = tableHistoryService.getTableHistoryByBidAndBgroup(did, tid, pageNum, pageSize);
        LogBean.get().addProps("ret",tableHistoryByBidAndBgroup);
        return WebResult.success(tableHistoryByBidAndBgroup);
    }




    @ApiOperation(value = "根据id查询具体的table操作历史,", notes = "需要did，table历史记录的id")
    @GetMapping("/tableById")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getTableById(@RequestParam Long did,
                                  @RequestParam Long id) {
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("Id", id);
        CamelliaTableHistory tableHistory=tableHistoryService.getById(did,id);
        if(tableHistory!=null){
            LogBean.get().addProps("ret",tableHistory);
            return WebResult.success(tableHistory.toJson());
        }
        return WebResult.success();
    }




    @ApiOperation(value = "根据bid、group查询全量的ref操作历史", notes = "需要did，bid,bgroup，当前页和页大小")
    @GetMapping("/refByBidAndBgroup")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getAllTableRefByBidAndBgroup(@RequestParam Long did,
                                    @RequestParam(value = "bid" ,required = false) Long bid,
                                    @RequestParam(value = "bgroup",required = false) String bgroup,
                                    @RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize) {
        LogBean.get().addProps("did", did);

        if(bid==null){
            LogBean.get().addProps("bid",bid);
        }
        if(StringUtil.isNullOrEmpty(bgroup)){
            LogBean.get().addProps("bgroup",bgroup);
        }
        LogBean.get().addProps("pageNum", pageNum);
        LogBean.get().addProps("pageSize", pageSize);
        CamelliaRefHistoryListWithNum allByBidAndBgroup = refHistoryService.getAllByBidAndBgroup(did, bid, bgroup, pageNum, pageSize);
        LogBean.get().addProps("ret",allByBidAndBgroup);

        return WebResult.success(allByBidAndBgroup);
    }


    @ApiOperation(value = "根据id查询具体的ref操作历史,", notes = "需要did，ref历史记录的id")
    @GetMapping("/refById")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getById(@RequestParam Long did,
                             @RequestParam Long id){
        LogBean.get().addProps("did",did);
        LogBean.get().addProps("id",id);
        CamelliaRefHistory camelliaRefHistory=refHistoryService.getById(did,id);
        if(camelliaRefHistory!=null) {
            return WebResult.success(camelliaRefHistory.toJson());
        }else {
            return WebResult.success();
        }
    }
}
