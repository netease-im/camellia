package com.netease.nim.camellia.hbase.util;

import com.netease.nim.camellia.hbase.exception.CamelliaHBaseException;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.RowMutations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

/**
 *
 * Created by caojiajun on 2026/10/12
 */
public class HBaseMutationCopyUtil {

    //HBase的Put/Delete/Append/Increment/RowMutations都是可变对象且非线程安全
    //camellia多写(MultiWriteInvoker)会把同一批参数交给多个线程并发写多个集群
    //同时javaagent(如skywalking的hbase插件会给mutation设置attribute)也会修改这些对象
    //因此每个写请求都复制一份，保证每个线程/每个集群操作的是独立对象
    public static Put copyPut(Put put) {
        Put copy = new Put(put);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(put.getDurability());
        isolateFamilyCellMap(copy, put);
        return copy;
    }

    public static List<Put> copyPuts(List<Put> puts) {
        List<Put> list = new ArrayList<>(puts.size());
        for (Put put : puts) {
            list.add(copyPut(put));
        }
        return list;
    }

    public static Delete copyDelete(Delete delete) {
        Delete copy = new Delete(delete);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(delete.getDurability());
        isolateFamilyCellMap(copy, delete);
        return copy;
    }

    public static List<Delete> copyDeletes(List<Delete> deletes) {
        List<Delete> list = new ArrayList<>(deletes.size());
        for (Delete delete : deletes) {
            list.add(copyDelete(delete));
        }
        return list;
    }

    public static Append copyAppend(Append append) {
        Append copy = new Append(append);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(append.getDurability());
        isolateFamilyCellMap(copy, append);
        return copy;
    }

    public static Increment copyIncrement(Increment increment) {
        Increment copy = new Increment(increment);
        //拷贝构造函数不会拷贝durability，需要手动设置
        copy.setDurability(increment.getDurability());
        isolateFamilyCellMap(copy, increment);
        return copy;
    }

    //Table.batch支持Get/Put/Delete/Increment/Append/RowMutations，这里除Get外都支持复制
    public static Row copyRow(Row row) {
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
        if (row instanceof RowMutations) {
            return copyRowMutations((RowMutations) row);
        }
        throw new CamelliaHBaseException("not support row type: " + row.getClass().getName());
    }

    //这里必须用deprecated的add(Put)/add(Delete)重载：hbase-client 1.x只提供这两个方法
    //2.x新增的add(Mutation)在1.x运行时不存在，使用它会抛NoSuchMethodError
    @SuppressWarnings("deprecation")
    public static RowMutations copyRowMutations(RowMutations rm) {
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

    //hbase-client 2.x的拷贝构造函数会为每个列族新建List，1.x的Delete/Append/Increment只复制外层Map
    //列族List仍然与原始对象共享，这里判断到共享时替换为独立List，保证各版本下列族容器都不共享
    private static void isolateFamilyCellMap(Mutation copy, Mutation original) {
        NavigableMap<byte[], List<Cell>> copyMap = copy.getFamilyCellMap();
        NavigableMap<byte[], List<Cell>> originalMap = original.getFamilyCellMap();
        for (byte[] family : new ArrayList<>(copyMap.keySet())) {
            List<Cell> copyList = copyMap.get(family);
            if (copyList == originalMap.get(family)) {
                copyMap.put(family, new ArrayList<>(copyList));
            }
        }
    }
}
