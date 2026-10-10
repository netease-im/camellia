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

    //HBase的Put/Delete/Append/Increment/RowMutations都是可变对象且非线程安全
    //camellia多写(MultiWriteInvoker)会把同一批参数交给多个线程并发写多个集群
    //同时javaagent(如skywalking的hbase插件会给mutation设置attribute)也会修改这些对象
    //因此每个写请求都复制一份，保证每个线程/每个集群操作的是独立对象
    private static Put copyPut(Put put) {
        Put copy = new Put(put);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(put.getDurability());
        return copy;
    }

    private static List<Put> copyPuts(List<Put> puts) {
        List<Put> list = new ArrayList<>(puts.size());
        for (Put put : puts) {
            list.add(copyPut(put));
        }
        return list;
    }

    private static Delete copyDelete(Delete delete) {
        Delete copy = new Delete(delete);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(delete.getDurability());
        return copy;
    }

    private static List<Delete> copyDeletes(List<Delete> deletes) {
        List<Delete> list = new ArrayList<>(deletes.size());
        for (Delete delete : deletes) {
            list.add(copyDelete(delete));
        }
        return list;
    }

    private static Append copyAppend(Append append) {
        Append copy = new Append(append);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(append.getDurability());
        return copy;
    }

    private static Increment copyIncrement(Increment increment) {
        Increment copy = new Increment(increment);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(increment.getDurability());
        return copy;
    }

    private static Row copyRow(Row row) {
        if (row instanceof Put) {
            return copyPut((Put) row);
        }
        if (row instanceof Delete) {
            return copyDelete((Delete) row);
        }
        if (row instanceof Append) {
            return copyAppend((Append) row);
        }
        if (row instanceof Increment) {
            return copyIncrement((Increment) row);
        }
        throw new CamelliaHBaseException("not support row type: " + row.getClass().getName());
    }

    private static RowMutations copyRowMutations(RowMutations rm) {
        try {
            RowMutations copy = new RowMutations(rm.getRow());
            for (Mutation mutation : rm.getMutations()) {
                if (mutation instanceof Put) {
                    copy.add(copyPut((Put) mutation));
                } else if (mutation instanceof Delete) {
                    copy.add(copyDelete((Delete) mutation));
                } else {
                    throw new CamelliaHBaseException("not support mutation type: " + mutation.getClass().getName());
                }
            }
            return copy;
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        }
    }

    @WriteOp
    public void put(String tableName, Put put) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.put(copyPut(put));
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
        List<Put> list = copyPuts(puts);
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
                table.delete(copyDelete(delete));
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
        List<Delete> list = copyDeletes(deletes);
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
        List<Row> list = new ArrayList<>(actions.size());
        for (Row action : actions) {
            list.add(copyRow(action));
        }
        //results是调用方传入的数组，多写时会被多个线程共享，这里先用本地数组，最后再回填
        Object[] localResults = new Object[actions.size()];
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                table.batch(list, localResults);
            }
        } catch (IOException | InterruptedException e) {
            throw new CamelliaHBaseException(e);
        } finally {
            if (results != null) {
                System.arraycopy(localResults, 0, results, 0, Math.min(localResults.length, results.length));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("{} batchWriteOpe, resource = {}, size = {}", tableName, resource.getUrl(), actions.size());
            }
        }
    }

    @WriteOp
    public boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Delete delete) {
        try {
            try (Table table = connectionFactory.getHBaseConnection(resource).getTable(tableName)) {
                return table.checkAndDelete(row, family, qualifier, value, copyDelete(delete));
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
                return table.checkAndDelete(row, family, qualifier, compareOp, value, copyDelete(delete));
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
                return table.checkAndPut(row, family, qualifier, value, copyPut(put));
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
                return table.checkAndPut(row, family, qualifier, compareOp, value, copyPut(put));
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
                return table.checkAndMutate(row, family, qualifier, compareOp, value, copyRowMutations(mutation));
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
                table.mutateRow(copyRowMutations(rm));
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
                return table.append(copyAppend(append));
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
                return table.increment(copyIncrement(increment));
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
