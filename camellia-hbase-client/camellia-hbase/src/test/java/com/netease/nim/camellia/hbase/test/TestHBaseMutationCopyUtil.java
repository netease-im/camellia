package com.netease.nim.camellia.hbase.test;

import com.netease.nim.camellia.hbase.exception.CamelliaHBaseException;
import com.netease.nim.camellia.hbase.util.HBaseMutationCopyUtil;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by caojiajun on 2026/10/12
 */
public class TestHBaseMutationCopyUtil {

    private final byte[] row = Bytes.toBytes("row-1");
    private final byte[] family = Bytes.toBytes("f1");

    @Test
    public void testCopyPutKeepsSemantics() {
        Put put = new Put(row);
        put.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        put.setDurability(Durability.SKIP_WAL);
        put.setTTL(1234L);
        put.setPriority(7);
        put.setAttribute("agent-key", Bytes.toBytes("agent-value"));

        Put copy = HBaseMutationCopyUtil.copyPut(put);

        Assert.assertNotSame(put, copy);
        Assert.assertEquals(Durability.SKIP_WAL, copy.getDurability());
        Assert.assertEquals(1234L, copy.getTTL());
        Assert.assertEquals(7, copy.getPriority());
        Assert.assertArrayEquals(Bytes.toBytes("agent-value"), copy.getAttribute("agent-key"));
        Assert.assertArrayEquals(put.getRow(), copy.getRow());
        Assert.assertEquals(put.getTimestamp(), copy.getTimestamp());
        Assert.assertNotSame(put.getFamilyCellMap(), copy.getFamilyCellMap());
        Assert.assertNotSame(put.getFamilyCellMap().get(family), copy.getFamilyCellMap().get(family));
        Assert.assertEquals(put.getFamilyCellMap().get(family), copy.getFamilyCellMap().get(family));
    }

    @Test
    public void testCopyPutIsolation() {
        Put put = new Put(row);
        put.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        int size = put.getFamilyCellMap().get(family).size();

        Put copy = HBaseMutationCopyUtil.copyPut(put);

        put.addColumn(family, Bytes.toBytes("q2"), Bytes.toBytes("v2"));
        Assert.assertEquals(size, copy.getFamilyCellMap().get(family).size());
        Assert.assertEquals(size + 1, put.getFamilyCellMap().get(family).size());

        copy.addColumn(family, Bytes.toBytes("q3"), Bytes.toBytes("v3"));
        Assert.assertEquals(size + 1, put.getFamilyCellMap().get(family).size());
    }

    @Test
    public void testCopyDelete() {
        Delete delete = new Delete(row);
        delete.addColumn(family, Bytes.toBytes("q1"));
        delete.setDurability(Durability.SKIP_WAL);

        Delete copy = HBaseMutationCopyUtil.copyDelete(delete);

        Assert.assertNotSame(delete, copy);
        Assert.assertEquals(Durability.SKIP_WAL, copy.getDurability());
        Assert.assertArrayEquals(delete.getRow(), copy.getRow());
        Assert.assertNotSame(delete.getFamilyCellMap(), copy.getFamilyCellMap());
        Assert.assertNotSame(delete.getFamilyCellMap().get(family), copy.getFamilyCellMap().get(family));
    }

    @Test
    public void testCopyAppendAndIncrement() throws Exception {
        Append append = new Append(row);
        append.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("+a"));
        append.setDurability(Durability.SKIP_WAL);
        append.setTimeRange(10L, 20L);

        Append appendCopy = HBaseMutationCopyUtil.copyAppend(append);
        Assert.assertEquals(Durability.SKIP_WAL, appendCopy.getDurability());
        Assert.assertEquals(10L, appendCopy.getTimeRange().getMin());
        Assert.assertEquals(20L, appendCopy.getTimeRange().getMax());
        Assert.assertNotSame(append.getFamilyCellMap().get(family), appendCopy.getFamilyCellMap().get(family));

        Increment increment = new Increment(row);
        increment.addColumn(family, Bytes.toBytes("q1"), 5L);
        increment.setDurability(Durability.SKIP_WAL);
        increment.setTimeRange(11L, 21L);

        Increment incrementCopy = HBaseMutationCopyUtil.copyIncrement(increment);
        Assert.assertEquals(Durability.SKIP_WAL, incrementCopy.getDurability());
        Assert.assertEquals(11L, incrementCopy.getTimeRange().getMin());
        Assert.assertEquals(21L, incrementCopy.getTimeRange().getMax());
        Assert.assertNotSame(increment.getFamilyCellMap().get(family), incrementCopy.getFamilyCellMap().get(family));
    }

    @Test
    public void testCopyRowDispatch() {
        Put put = new Put(row);
        put.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        Delete delete = new Delete(row);
        delete.addColumn(family, Bytes.toBytes("q1"));
        Append append = new Append(row);
        append.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("+a"));
        Increment increment = new Increment(row);
        increment.addColumn(family, Bytes.toBytes("q1"), 1L);
        RowMutations rowMutations = new RowMutations(row);

        Assert.assertTrue(HBaseMutationCopyUtil.copyRow(put) instanceof Put);
        Assert.assertTrue(HBaseMutationCopyUtil.copyRow(delete) instanceof Delete);
        Assert.assertTrue(HBaseMutationCopyUtil.copyRow(append) instanceof Append);
        Assert.assertTrue(HBaseMutationCopyUtil.copyRow(increment) instanceof Increment);
        Assert.assertTrue(HBaseMutationCopyUtil.copyRow(rowMutations) instanceof RowMutations);

        try {
            HBaseMutationCopyUtil.copyRow(new Get(row));
            Assert.fail("Get should not be supported");
        } catch (CamelliaHBaseException e) {
            Assert.assertTrue(e.getMessage().contains("not support row type"));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCopyRowMutations() throws Exception {
        Put put = new Put(row);
        put.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        put.setDurability(Durability.SKIP_WAL);
        Delete delete = new Delete(row);
        delete.addColumn(family, Bytes.toBytes("q2"));
        RowMutations rm = new RowMutations(row);
        rm.add(put);
        rm.add(delete);

        RowMutations copy = HBaseMutationCopyUtil.copyRowMutations(rm);

        Assert.assertNotSame(rm, copy);
        Assert.assertArrayEquals(rm.getRow(), copy.getRow());
        Assert.assertEquals(rm.getMutations().size(), copy.getMutations().size());
        Assert.assertNotSame(rm.getMutations().get(0), copy.getMutations().get(0));
        Assert.assertNotSame(rm.getMutations().get(1), copy.getMutations().get(1));
        Assert.assertEquals(Durability.SKIP_WAL, ((Mutation) copy.getMutations().get(0)).getDurability());
        Assert.assertNotSame(put.getFamilyCellMap().get(family),
                ((Mutation) copy.getMutations().get(0)).getFamilyCellMap().get(family));

        RowMutations unsupported = new RowMutations(row);
        unsupported.add(new Increment(row));
        try {
            HBaseMutationCopyUtil.copyRowMutations(unsupported);
            Assert.fail("Increment should not be supported in RowMutations");
        } catch (CamelliaHBaseException e) {
            Assert.assertTrue(e.getMessage().contains("not support mutation type"));
        }
    }

    @Test
    public void testCopyPutsAndDeletes() {
        Put put1 = new Put(row);
        put1.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        Put put2 = new Put(Bytes.toBytes("row-2"));
        put2.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));

        List<Put> puts = HBaseMutationCopyUtil.copyPuts(Arrays.asList(put1, put2));
        Assert.assertEquals(2, puts.size());
        Assert.assertNotSame(put1, puts.get(0));
        Assert.assertNotSame(put2, puts.get(1));
        Assert.assertArrayEquals(put1.getRow(), puts.get(0).getRow());
        Assert.assertArrayEquals(put2.getRow(), puts.get(1).getRow());

        Delete delete = new Delete(row);
        delete.addColumn(family, Bytes.toBytes("q1"));
        List<Delete> deletes = HBaseMutationCopyUtil.copyDeletes(Arrays.asList(delete));
        Assert.assertEquals(1, deletes.size());
        Assert.assertNotSame(delete, deletes.get(0));
        Assert.assertArrayEquals(delete.getRow(), deletes.get(0).getRow());
    }

    @Test
    public void testCopyPutWithMultipleFamilies() {
        byte[] family2 = Bytes.toBytes("f2");
        Put put = new Put(row);
        put.addColumn(family, Bytes.toBytes("q1"), Bytes.toBytes("v1"));
        put.addColumn(family2, Bytes.toBytes("q1"), Bytes.toBytes("v1"));

        Put copy = HBaseMutationCopyUtil.copyPut(put);

        for (byte[] familyKey : put.getFamilyCellMap().keySet()) {
            List<Cell> copyList = copy.getFamilyCellMap().get(familyKey);
            Assert.assertNotNull(copyList);
            Assert.assertNotSame(put.getFamilyCellMap().get(familyKey), copyList);
            Assert.assertEquals(put.getFamilyCellMap().get(familyKey), copyList);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCopyRowOfUnsupportedType() {
        Row unsupported = new Row() {
            @Override
            public int compareTo(Row other) {
                return 0;
            }

            @Override
            public byte[] getRow() {
                return row;
            }
        };
        try {
            HBaseMutationCopyUtil.copyRow(unsupported);
            Assert.fail("unsupported Row type should be rejected");
        } catch (CamelliaHBaseException e) {
            Assert.assertTrue(e.getMessage().contains("not support row type"));
        }
    }
}
