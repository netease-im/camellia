package com.netease.nim.camellia.hbase;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.connection.CamelliaHBaseConnectionFactory;
import com.netease.nim.camellia.hbase.exception.CamelliaHBaseException;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.resource.HBaseResourceWrapper;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseClientImpl {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHBaseClientImpl.class);

    private CamelliaHBaseConnectionFactory connectionFactory;
    private HBaseResource resource;

    public CamelliaHBaseClientImpl(Resource resource) {
        if (resource == null) return;
        if (resource instanceof HBaseResourceWrapper) {
            CamelliaHBaseEnv env = ((HBaseResourceWrapper) resource).getEnv();
            this.connectionFactory = env.getConnectionFactory();
            this.resource = HBaseResourceUtil.parseResourceByUrl(resource);
            this.connectionFactory.getHBaseConnection(this.resource);
        } else {
            throw new IllegalArgumentException("not HBaseResourceWrapper");
        }
    }

    @WriteOp
    public void put(String tableName, Put put) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.put(put);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} put, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(put.getRow()));
            }
        }
    }

    @WriteOp
    public void put(String tableName, List<Put> puts) {
        List<Put> list = new ArrayList<>(puts);
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.put(list);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} put, resource = {}, size = {}", tableName, resource.getUrl(), puts.size());
            }
        }
    }

    @WriteOp
    public void delete(String tableName, Delete delete) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.delete(delete);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} delete, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(delete.getRow()));
            }
        }
    }

    @WriteOp
    public void delete(String tableName, List<Delete> deletes) {
        List<Delete> list = new ArrayList<>(deletes);
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.delete(list);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} delete, resource = {}, size = {}", tableName, resource.getUrl(), deletes.size());
            }
        }
    }

    @WriteOp
    public void batchWriteOpe(String tableName, List<? extends Row> actions, Object[] results) {
        for (Row action : actions) {
            if (action instanceof Get) {
                throw new CamelliaHBaseException("not support GET");
            }
        }
        List<? extends Row> list = new ArrayList<>(actions);
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.batch(list, results);
            }
        } catch (IOException | InterruptedException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} batchWriteOpe, resource = {}, size = {}", tableName, resource.getUrl(), actions.size());
            }
        }
    }

    @WriteOp
    public boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Delete delete) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndDelete(row, family, qualifier, value, delete);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} checkAndDelete, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @WriteOp
    public boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier,
                                  CompareFilter.CompareOp compareOp, byte[] value, Delete delete) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndDelete(row, family, qualifier, compareOp, value, delete);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} checkAndDelete, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @ReadOp
    public Result get(String tableName, Get get) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.get(get);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} get, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(get.getRow()));
            }
        }
    }

    @ReadOp
    public Result[] get(String tableName, List<Get> gets) {
        List<Get> list = new ArrayList<>(gets);
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.get(list);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} get, resource = {}, size = {}", tableName, resource.getUrl(), gets.size());
            }
        }
    }

    @ReadOp
    public ResultScanner scan(String tableName, Scan scan) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.getScanner(scan);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} scan, resource = {}, startRowKey = {}, endRowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(scan.getStartRow()), Bytes.toHex(scan.getStopRow()));
            }
        }
    }

    @WriteOp
    public boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Put put) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndPut(row, family, qualifier, value, put);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} checkAndPut, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @WriteOp
    public boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier,
                               CompareFilter.CompareOp compareOp, byte[] value, Put put) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndPut(row, family, qualifier, compareOp, value, put);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} checkAndPut, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @ReadOp
    public boolean exists(String tableName, Get get) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.exists(get);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} exists, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(get.getRow()));
            }
        }
    }

    @ReadOp
    public boolean[] existsAll(String tableName, List<Get> gets) {
        ArrayList<Get> list = new ArrayList<>(gets);
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.existsAll(list);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} existsAll, resource = {}, size = {}",
                        tableName, resource.getUrl(), gets.size());
            }
        }
    }

    @WriteOp
    public boolean checkAndMutate(String tableName, byte[] row, byte[] family, byte[] qualifier,
                                  CompareFilter.CompareOp compareOp, byte[] value, RowMutations mutation) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndMutate(row, family, qualifier, compareOp, value, mutation);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} checkAndMutate, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @WriteOp
    public void mutateRow(String tableName, RowMutations rm) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.mutateRow(rm);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} mutateRow, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(rm.getRow()));
            }
        }
    }

    @WriteOp
    public Result append(String tableName, Append append) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.append(append);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} append, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(append.getRow()));
            }
        }
    }

    @WriteOp
    public Result increment(String tableName, Increment increment) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.increment(increment);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} increment, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(increment.getRow()));
            }
        }
    }

    @WriteOp
    public long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.incrementColumnValue(row, family, qualifier, amount);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} incrementColumnValue, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }

    @WriteOp
    public long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount, Durability durability) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.incrementColumnValue(row, family, qualifier, amount, durability);
            }
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("{} incrementColumnValue, resource = {}, rowKey = {}",
                        tableName, resource.getUrl(), Bytes.toHex(row));
            }
        }
    }
}
