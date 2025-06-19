package com.netease.nim.camellia.hbase;

import com.netease.nim.camellia.core.api.*;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTransferUtil;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.resource.HBaseResourceWrapper;
import com.netease.nim.camellia.hbase.resource.HBaseTemplateResourceTableUpdater;
import com.netease.nim.camellia.hbase.util.CamelliaHBaseInitUtil;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;

import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseTemplate implements ICamelliaHBaseTemplate {

    private static final long defaultBid = -1;
    private static final String defaultBgroup = "local";
    private static final long defaultCheckIntervalMillis = 5000;
    private static final boolean defaultMonitorEnable = false;

    private final ReloadableProxyFactory<CamelliaHBaseClientImpl> factory;
    private final CamelliaApi service;

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, CamelliaApi service, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis) {
        this.service = service;
        this.factory = new ReloadableProxyFactory.Builder<CamelliaHBaseClientImpl>()
                .service(new ApiServiceWrapper(service, env))
                .clazz(CamelliaHBaseClientImpl.class)
                .bid(bid)
                .bgroup(bgroup)
                .monitorEnable(monitorEnable)
                .checkIntervalMillis(checkIntervalMillis)
                .proxyEnv(env.getProxyEnv())
                .build();
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, HBaseTemplateResourceTableUpdater updater) {
        this(env, new LocalDynamicCamelliaApi(updater.getResourceTable(), HBaseResourceUtil.HBaseResourceTableChecker),
                defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
        updater.addCallback(new ResourceTableUpdateCallback() {
            @Override
            public void callback(ResourceTable resourceTable) {
                if (service instanceof LocalDynamicCamelliaApi) {
                    ((LocalDynamicCamelliaApi) service).updateResourceTable(resourceTable);
                }
                reloadResourceTable();
            }
        });
    }

    public final void reloadResourceTable() {
        factory.reload(false);
    }

    public CamelliaHBaseTemplate(HBaseTemplateResourceTableUpdater updater) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), updater);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis,
                                 int connectTimeoutMillis, int readTimeoutMillis) {
        this(env, CamelliaApiUtil.init(url, connectTimeoutMillis, readTimeoutMillis), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis,
                                 int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, connectTimeoutMillis, readTimeoutMillis, headerMap), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis) {
        this(env, CamelliaApiUtil.init(url), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, headerMap), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, CamelliaApi service, long bid, String bgroup) {
        this(env, service, bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup) {
        this(env, CamelliaApiUtil.init(url), bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, String url, long bid, String bgroup, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, headerMap), bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaHBaseTemplate(String url, long bid, String bgroup, boolean monitorEnable, long checkIntervalMillis) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), url, bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(String url, long bid, String bgroup, boolean monitorEnable, long checkIntervalMillis, Map<String, String> headerMap) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), url, bid, bgroup, monitorEnable, checkIntervalMillis, headerMap);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, ResourceTable resourceTable) {
        this(env, new LocalCamelliaApi(resourceTable), defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaHBaseTemplate(ResourceTable resourceTable) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), resourceTable);
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, HBaseResource hBaseResource) {
        this(env, ResourceTableUtil.simpleTable(hBaseResource));
    }

    public CamelliaHBaseTemplate(HBaseResource hBaseResource) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), ResourceTableUtil.simpleTable(hBaseResource));
    }

    public CamelliaHBaseTemplate(String hbaseXmlFile) {
        this(CamelliaHBaseInitUtil.initHBaseEnvFromHBaseFile(hbaseXmlFile), CamelliaHBaseInitUtil.initHBaseResourceFromFile(hbaseXmlFile));
    }

    public CamelliaHBaseTemplate(CamelliaHBaseEnv env, ReloadableLocalFileCamelliaApi reloadableLocalFileCamelliaApi, long checkIntervalMillis) {
        this(env, reloadableLocalFileCamelliaApi, defaultBid, defaultBgroup, defaultMonitorEnable, checkIntervalMillis);
    }

    public CamelliaHBaseTemplate(ReloadableLocalFileCamelliaApi reloadableLocalFileCamelliaApi) {
        this(CamelliaHBaseEnv.defaultHBaseEnv(), reloadableLocalFileCamelliaApi, defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    private static class ApiServiceWrapper implements CamelliaApi {

        private final CamelliaApi service;
        private final CamelliaHBaseEnv env;

        ApiServiceWrapper(CamelliaApi service, CamelliaHBaseEnv env) {
            this.service = service;
            this.env = env;
        }

        @Override
        public CamelliaApiResponse getResourceTable(Long bid,
                                                    String bgroup,
                                                    String md5) {
            CamelliaApiResponse response = service.getResourceTable(bid, bgroup, md5);
            ResourceTable resourceTable = ResourceTransferUtil.transfer(response.getResourceTable(), new ResourceTransferUtil.ResourceTransferFunc() {
                @Override
                public Resource transfer(Resource resource) {
                    HBaseResourceWrapper resourceWrapper = new HBaseResourceWrapper(resource);
                    resourceWrapper.setEnv(env);
                    return resourceWrapper;
                }
            });
            response.setResourceTable(resourceTable);
            return response;
        }

        @Override
        public CamelliaApiV2Response getResourceTableV2(Long bid, String bgroup, String md5) {
            return ResourceTableUtil.toV2Response(getResourceTable(bid, bgroup, md5));
        }

        @Override
        public boolean reportStats(ResourceStats resourceStats) {
            return service.reportStats(resourceStats);
        }
    }

    @Override
    public void put(String tableName, Put put) {
        factory.getProxy().put(tableName, put);
    }

    @Override
    public void put(String tableName, List<Put> puts) {
        factory.getProxy().put(tableName, puts);
    }

    @Override
    public void delete(String tableName, Delete delete) {
        factory.getProxy().delete(tableName, delete);
    }

    @Override
    public void delete(String tableName, List<Delete> deletes) {
        factory.getProxy().delete(tableName, deletes);
    }

    @Override
    public void batchWriteOpe(String tableName, List<? extends Row> actions, Object[] results) {
        factory.getProxy().batchWriteOpe(tableName, actions, results);
    }

    @Override
    public boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Delete delete) {
        return factory.getProxy().checkAndDelete(tableName, row, family, qualifier, value, delete);
    }

    @Override
    public boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Delete delete) {
        return factory.getProxy().checkAndDelete(tableName, row, family, qualifier, compareOp, value, delete);
    }

    @Override
    public Result get(String tableName, Get get) {
        return factory.getProxy().get(tableName, get);
    }

    @Override
    public Result[] get(String tableName, List<Get> gets) {
        return factory.getProxy().get(tableName, gets);
    }

    @Override
    public ResultScanner scan(String tableName, Scan scan) {
        return factory.getProxy().scan(tableName, scan);
    }

    @Override
    public boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Put put) {
        return factory.getProxy().checkAndPut(tableName, row, family, qualifier, value, put);
    }

    @Override
    public boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Put put) {
        return factory.getProxy().checkAndPut(tableName, row, family, qualifier, compareOp, value, put);
    }

    @Override
    public boolean exists(String tableName, Get get) {
        return factory.getProxy().exists(tableName, get);
    }

    @Override
    public boolean[] existsAll(String tableName, List<Get> gets) {
        return factory.getProxy().existsAll(tableName, gets);
    }

    @Override
    public boolean checkAndMutate(String tableName, byte[] row, byte[] family, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, RowMutations mutation) {
        return factory.getProxy().checkAndMutate(tableName, row, family, qualifier, compareOp, value, mutation);
    }

    @Override
    public void mutateRow(String tableName, RowMutations rm) {
        factory.getProxy().mutateRow(tableName, rm);
    }

    @Override
    public Result append(String tableName, Append append) {
        return factory.getProxy().append(tableName, append);
    }

    @Override
    public Result increment(String tableName, Increment increment) {
        return factory.getProxy().increment(tableName, increment);
    }

    @Override
    public long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount) {
        return factory.getProxy().incrementColumnValue(tableName, row, family, qualifier, amount);
    }

    @Override
    public long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount, Durability durability) {
        return factory.getProxy().incrementColumnValue(tableName, row, family, qualifier, amount, durability);
    }
}
