package com.netease.nim.camellia.hbase;

import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public interface ICamelliaHBaseTemplate {

    void put(String tableName, Put put);

    void put(String tableName, List<Put> puts);

    void delete(String tableName, Delete delete);

    void delete(String tableName, List<Delete> deletes);

    void batchWriteOpe(String tableName, List<? extends Row> actions, Object[] results);

    boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Delete delete);

    boolean checkAndDelete(String tableName, byte[] row, byte[] family, byte[] qualifier,
                                  CompareFilter.CompareOp compareOp, byte[] value, Delete delete);

    Result get(String tableName, Get get);

    Result[] get(String tableName, List<Get> gets);

    ResultScanner scan(String tableName, Scan scan);

    boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier, byte[] value, Put put);

    boolean checkAndPut(String tableName, byte[] row, byte[] family, byte[] qualifier,
                               CompareFilter.CompareOp compareOp, byte[] value, Put put);

    boolean exists(String tableName, Get get);

    boolean[] existsAll(String tableName, List<Get> gets);

    boolean checkAndMutate(String tableName, byte[] row, byte[] family, byte[] qualifier,
                                  CompareFilter.CompareOp compareOp, byte[] value, RowMutations mutation);

    void mutateRow(String tableName, RowMutations rm);

    Result append(String tableName, Append append);

    Result increment(String tableName, Increment increment);

    long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount);

    long incrementColumnValue(String tableName, byte[] row, byte[] family, byte[] qualifier, long amount, Durability durability);
}
