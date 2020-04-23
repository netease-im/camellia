package com.netease.nim.camellia.dashboard.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.client.env.DefaultShadingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.exception.AppException;
import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import com.netease.nim.camellia.dashboard.model.Table;
import com.netease.nim.camellia.dashboard.model.TableRef;
import com.netease.nim.camellia.dashboard.service.ResourceInfoService;
import com.netease.nim.camellia.dashboard.service.StatsService;
import com.netease.nim.camellia.dashboard.service.TableRefService;
import com.netease.nim.camellia.dashboard.service.TableService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.dashboard.model.RwStats;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Api(value = "管理接口", tags = {"AdminController"})
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/camellia/admin")
public class AdminController {

    @Autowired
    private TableService tableService;

    @Autowired
    private TableRefService tableRefService;

    @Autowired
    private ResourceInfoService resourceInfoService;

    @Autowired
    private StatsService statsService;

    @ApiOperation(value = "创建资源表", notes = "创建接口")
    @PostMapping("/createResourceTable")
    public WebResult createResourceTable(@RequestParam("detail") String detail,
                                         @RequestParam("info") String info) {
        LogBean.get().addProps("detail", detail);
        LogBean.get().addProps("info", info);
        ResourceTable resourceTable;
        try {
            resourceTable = JSONObject.parseObject(detail, ResourceTable.class);
            boolean check = CheckUtil.checkResourceTable(resourceTable);
            if (!check) {
                resourceTable = ReadableResourceTableUtil.parseTable(detail);
            }
        } catch (Exception e) {
            try {
                resourceTable = ReadableResourceTableUtil.parseTable(detail);
            } catch (Exception e1) {
                throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "resourceTable parse error");
            }
        }

        Table ret = tableService.create(resourceTable, info);
        JSONObject retJson = ret.toJson();
        LogBean.get().addProps("ret", retJson);
        return WebResult.success(retJson);
    }

    @ApiOperation(value = "查询单个资源表", notes = "需要指定tid")
    @GetMapping("/resourceTable/{tid}")
    public WebResult getResourceTable(@PathVariable("tid") long tid) {
        LogBean.get().addProps("tid", tid);
        Table table = tableService.get(tid);
        if (table == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not exists");
        }
        JSONObject ret = table.toJson();
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "删除单个资源表", notes = "需要指定tid")
    @DeleteMapping("/resourceTable/{tid}")
    public WebResult deleteResourceTable(@PathVariable long tid) {
        LogBean.get().addProps("tid", tid);
        tableService.delete(tid);
        return WebResult.success();
    }

    @ApiOperation(value = "获取资源表列表", notes = "全量列表，可以指定是否只返回valid的资源表")
    @GetMapping("/resourceTables")
    public WebResult getResourceTableList(@RequestParam(value = "onlyValid", required = false, defaultValue = "true") boolean onlyValid) {
        LogBean.get().addProps("onlyValid", onlyValid);
        List<Table> list = tableService.getList(onlyValid);
        JSONArray ret = new JSONArray();
        for (Table table : list) {
            ret.add(table.toJson());
        }
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "创建或者更新资源表引用关系", notes = "资源表引用关系")
    @PostMapping("/createOrUpdateTableRef")
    public WebResult createOrUpdateTableRef(@RequestParam("bid") long bid,
                                            @RequestParam("bgroup") String bgroup,
                                            @RequestParam("tid") long tid,
                                            @RequestParam("info") String info) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("tid", tid);
        LogBean.get().addProps("info", info);
        TableRef tableRef = tableRefService.createOrUpdate(bid, bgroup, tid, info);
        JSONObject ret = tableRef.toJson();
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "查询单个资源表引用关系", notes = "需要指定bid和bgroup")
    @GetMapping("/getTableRefByBidGroup")
    public WebResult getTableRef(@RequestParam("tid") long bid,
                                 @RequestParam("bgroup") String bgroup) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        TableRef tableRef = tableRefService.getByBidBGroup(bid, bgroup);
        if (tableRef == null) {
            LogBean.get().addProps("bid/group.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "bid/group not exists");
        }
        JSONObject ret = tableRef.toJson();
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "查询某个bid下的资源表引用关系列表", notes = "需要指定bid，并可以选择是否只返回valid的")
    @GetMapping("/getTableRefsByBid")
    public WebResult getTableRefByBid(@RequestParam("bid") long bid,
                                      @RequestParam(value = "onlyValid", required = false, defaultValue = "true") boolean onlyValid) {
        LogBean.get().addProps("onlyValid", onlyValid);
        LogBean.get().addProps("bid", bid);
        List<TableRef> list = tableRefService.getByBid(bid, onlyValid);
        JSONArray ret = new JSONArray();
        for (TableRef tableRef : list) {
            ret.add(tableRef.toJson());
        }
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "查询某个tid的资源表引用关系列表", notes = "需要指定tid，并可以选择是否只返回valid的")
    @GetMapping("/getTableRefsByTid")
    public WebResult getTableRefsByTid(@RequestParam("tid") long tid,
                                       @RequestParam(value = "onlyValid", required = false, defaultValue = "true") boolean onlyValid) {
        LogBean.get().addProps("onlyValid", onlyValid);
        LogBean.get().addProps("tid", tid);
        List<TableRef> list = tableRefService.getByTid(tid, onlyValid);
        JSONArray ret = new JSONArray();
        for (TableRef tableRef : list) {
            ret.add(tableRef.toJson());
        }
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "查询全量的资源表引用关系列表", notes = "可以选择是否只返回valid的")
    @GetMapping("/getTableRefs")
    public WebResult getTableRefs(@RequestParam(value = "onlyValid", required = false, defaultValue = "true") boolean onlyValid) {
        LogBean.get().addProps("onlyValid", onlyValid);
        List<TableRef> list = tableRefService.getList(onlyValid);
        JSONArray ret = new JSONArray();
        for (TableRef tableRef : list) {
            ret.add(tableRef.toJson());
        }
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @ApiOperation(value = "删除单个资源表引用关系", notes = "需要指定bid和bgroup")
    @DeleteMapping("/tableRef")
    public WebResult deleteTableRef(@RequestParam("tid") long bid,
                                    @RequestParam("bgroup") String bgroup) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        tableRefService.delete(bid, bgroup);
        return WebResult.success();
    }

    @ApiOperation(value = "创建或者更新资源描述", notes = "需要指定资源url和描述")
    @PostMapping("/createOrUpdateResource")
    public WebResult createOrUpdateResource(@RequestParam("url") String url,
                                            @RequestParam("info") String info) {
        LogBean.get().addProps("url", url);
        LogBean.get().addProps("info", info);
        resourceInfoService.createOrUpdateResource(url, info);
        return WebResult.success();
    }

    @ApiOperation(value = "删除单个资源", notes = "需要指定资源ID")
    @DeleteMapping("/resource")
    public WebResult deleteResource(@RequestParam("id") Long id) {
        resourceInfoService.delete(id);
        return WebResult.success();
    }

    @ApiOperation(value = "获取资源全量列表", notes = "返回全量")
    @GetMapping("/resources")
    public WebResult getResourceList() {
        List<ResourceInfo> list = resourceInfoService.getList();
        return WebResult.success(list);
    }

    @ApiOperation(value = "一个mock接口", notes = "用于在指定tid和分片key的情况下确定分片结果，使用的是默认的分片函数")
    @GetMapping("/mock")
    public WebResult mock(@RequestParam("tid") long tid,
                          @RequestParam("key") String key) throws Exception {
        LogBean.get().addProps("tid", tid);
        LogBean.get().addProps("key", key);
        Table table = tableService.get(tid);
        if (table == null) {
            LogBean.get().addProps("tid.not.exists", true);
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tid not exists");
        }
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(table.getDetail());
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            LogBean.get().addProps("check.fail", true);
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "resourceTable check fail");
        }
        ResourceTable.Type type = resourceTable.getType();
        LogBean.get().addProps("type", type);
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            Object ret = ReadableResourceTableUtil.readableResourceOperation(resourceOperation);
            LogBean.get().addProps("ret", ret);
            return WebResult.success(ret);
        } else if (type == ResourceTable.Type.SHADING) {
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            int bucketSize = shadingTable.getBucketSize();
            DefaultShadingFunc shadingFunc = new DefaultShadingFunc();
            int shadingCode = shadingFunc.shadingCode(key.getBytes("utf-8"));
            int index = shadingCode % bucketSize;
            ResourceOperation resourceOperation = shadingTable.getResourceOperationMap().get(index);
            Object ret = ReadableResourceTableUtil.readableResourceOperation(resourceOperation);
            LogBean.get().addProps("ret", ret);
            return WebResult.success(ret);
        }
        throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "type wrong");
    }

    @ApiOperation(value = "查询读写统计（全量）", notes = "用于查询上报的资源读写统计")
    @GetMapping("/rwStats/total")
    public WebResult rwStatsTotal() {
        RwStats stats = statsService.getStats();
        return WebResult.success(stats);
    }

    @ApiOperation(value = "根据资源url查询读写统计", notes = "用于查询上报的资源读写统计，根据资源url查询")
    @GetMapping("/rwStatsByResourceUrl")
    public WebResult rwStatsByResourceUrl(@RequestParam("url") String url) {
        ResourceInfo resourceInfo = resourceInfoService.getByUrl(url);
        if (resourceInfo == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "resource not exists");
        }
        JSONObject jsonObject = rwStats(resourceInfo);
        return WebResult.success(jsonObject);
    }

    @ApiOperation(value = "根据资源id查询读写统计", notes = "用于查询上报的资源读写统计，根据资源id查询")
    @GetMapping("/rwStatsByResourceId")
    public WebResult rwStatsByResourceId(@RequestParam("id") Long id) {
        ResourceInfo resourceInfo = resourceInfoService.get(id);
        if (resourceInfo == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "resource not exists");
        }
        JSONObject jsonObject = rwStats(resourceInfo);
        return WebResult.success(jsonObject);
    }

    @ApiOperation(value = "根据bid和bgroup查询读写统计", notes = "用于查询上报的资源读写统计，根据bid和bgroup查询")
    @GetMapping("/rwStatsByBidBgroup")
    public WebResult rwStatsByBidBgroup(@RequestParam("bid") Long bid,
                                        @RequestParam("bgroup") String bgroup) {
        TableRef tableRef = tableRefService.getByBidBGroup(bid, bgroup);
        if (tableRef == null) {
            throw new AppException(CamelliaApiCode.NOT_EXISTS.getCode(), "tableRef not exists");
        }

        RwStats stats = statsService.getStats();
        RwStats.BusinessTotal businessTotal = null;
        for (RwStats.BusinessTotal total : stats.getBusinessTotalList()) {
            if (total.getBid().equals(String.valueOf(bid)) && total.getBgroup().equals(bgroup)) {
                businessTotal = total;
                break;
            }
        }
        RwStats.BusinessDetail businessDetail = null;
        for (RwStats.BusinessDetail detail : stats.getBusinessDetailList()) {
            if (detail.getBid().equals(String.valueOf(bid)) && detail.getBgroup().equals(bgroup)) {
                businessDetail = detail;
                break;
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", businessTotal);
        jsonObject.put("detail", businessDetail);
        jsonObject.put("info", tableRef.getInfo());
        return WebResult.success(jsonObject);
    }

    private JSONObject rwStats(ResourceInfo resourceInfo) {
        String resourceUrl = resourceInfo.getUrl();
        RwStats stats = statsService.getStats();
        RwStats.Total total = null;
        for (RwStats.Total item : stats.getTotalList()) {
            if (item.getResource().equals(resourceUrl)) {
                total = item;
                break;
            }
        }
        RwStats.Detail detail = null;
        for (RwStats.Detail item : stats.getDetailList()) {
            if (item.getResource().equals(resourceUrl)) {
                detail = item;
                break;
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", total);
        jsonObject.put("detail", detail);
        jsonObject.put("info", resourceInfo.getInfo());
        return jsonObject;
    }
}
