package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;

import java.util.*;

/**
 *
 * Created by caojiajun on 2020/5/9.
 */
public class HBaseWriteOpeSerializeUtil {

    private static final String TYPE = "type";
    private static final String DATA = "data";

    private static final String ROW = "row";
    private static final String CELLS = "cells";

    private static final String FAMILY = "family";
    private static final String QUALIFIER = "qualifier";
    private static final String VALUE = "value";

    public static enum Type {
        PUT(1),
        DELETE(2),
        ;
        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type getByValue(int value) {
            for (Type type : Type.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
    }

    public static JSONObject serializePutList(List<Put> putList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TYPE, Type.PUT.getValue());
        JSONArray data = new JSONArray();
        for (Put put : putList) {
            data.add(toJson(put));
        }
        jsonObject.put(DATA, data);
        return jsonObject;
    }

    public static Put parsePut(JSONObject jsonObject) {
        String rowHex = jsonObject.getString(ROW);
        byte[] row = Bytes.fromHex(rowHex);
        Put put = new Put(row);
        JSONArray cells = jsonObject.getJSONArray(CELLS);
        for (Object cell : cells) {
            JSONObject cellJson = (JSONObject) cell;
            String family = cellJson.getString(FAMILY);
            String qualifier = cellJson.getString(QUALIFIER);
            String value = cellJson.getString(VALUE);
            put.addColumn(Bytes.fromHex(family), Bytes.fromHex(qualifier), Bytes.fromHex(value));
        }
        return put;
    }

    public static JSONObject toJson(Put put) {
        JSONObject jsonObject = new JSONObject();
        String rowHex = Bytes.toHex(put.getRow());
        jsonObject.put(ROW, rowHex);
        NavigableMap<byte[], List<Cell>> familyCellMap = put.getFamilyCellMap();
        JSONArray cells = new JSONArray();
        for (Map.Entry<byte[], List<Cell>> entry : familyCellMap.entrySet()) {
            List<Cell> cellList = entry.getValue();
            for (Cell cell : cellList) {
                JSONObject cellJson = new JSONObject();
                cellJson.put(QUALIFIER, Bytes.toHex(getQualifier(cell)));
                cellJson.put(FAMILY, Bytes.toHex(getFamily(cell)));
                cellJson.put(VALUE, Bytes.toHex(getValue(cell)));
                cells.add(cellJson);
            }
        }
        jsonObject.put(CELLS, cells);
        return jsonObject;
    }

    public static JSONObject serializeDeleteList(List<Delete> deleteList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TYPE, Type.DELETE.getValue());
        JSONArray data = new JSONArray();
        for (Delete delete : deleteList) {
            data.add(toJson(delete));
        }
        jsonObject.put(DATA, data);
        return jsonObject;
    }

    public static Delete parseDelete(JSONObject jsonObject) {
        String rowHex = jsonObject.getString(ROW);
        byte[] row = Bytes.fromHex(rowHex);
        Delete delete = new Delete(row);
        JSONArray cells = jsonObject.getJSONArray(CELLS);
        for (Object cell : cells) {
            JSONObject cellJson = (JSONObject) cell;
            String family = cellJson.getString(FAMILY);
            String qualifier = cellJson.getString(QUALIFIER);
            delete.addColumns(Bytes.fromHex(family), Bytes.fromHex(qualifier));
        }
        return delete;
    }

    public static JSONObject toJson(Delete delete) {
        JSONObject jsonObject = new JSONObject();
        String rowKeyHex = Bytes.toHex(delete.getRow());
        jsonObject.put(ROW, rowKeyHex);
        JSONArray cells = new JSONArray();
        for (Map.Entry<byte[], List<Cell>> entry : delete.getFamilyCellMap().entrySet()) {
            for (Cell cell : entry.getValue()) {
                JSONObject cellJson = new JSONObject();
                cellJson.put(QUALIFIER, Bytes.toHex(getQualifier(cell)));
                cellJson.put(FAMILY, Bytes.toHex(getFamily(cell)));
                cells.add(cellJson);
            }
        }
        jsonObject.put(CELLS, cells);
        return jsonObject;
    }

    private static byte[] getQualifier(Cell cell) {
        byte[] qualifierArray = cell.getQualifierArray();
        byte[] qualifier = new byte[cell.getQualifierLength()];
        System.arraycopy(qualifierArray, cell.getQualifierOffset(), qualifier, 0, cell.getQualifierLength());
        return qualifier;
    }

    private static byte[] getFamily(Cell cell) {
        byte[] familyArray = cell.getFamilyArray();
        byte[] family = new byte[cell.getFamilyLength()];
        System.arraycopy(familyArray, cell.getFamilyOffset(), family, 0, cell.getFamilyLength());
        return family;
    }

    private static byte[] getValue(Cell cell) {
        byte[] valueArray = cell.getValueArray();
        byte[] value = new byte[cell.getValueLength()];
        System.arraycopy(valueArray, cell.getValueOffset(), value, 0, cell.getValueLength());
        return value;
    }

    public static Pair<Type, List<Row>> parse(JSONObject jsonObject) {
        Integer typeV = jsonObject.getInteger(TYPE);
        if (typeV == null) return null;
        Type type = Type.getByValue(typeV);
        if (type == null) return null;
        JSONArray data = jsonObject.getJSONArray(DATA);
        if (data == null || data.isEmpty()) return null;
        List<Row> rowList = new ArrayList<>();
        if (type == Type.PUT) {
            for (Object datum : data) {
                JSONObject putJson = (JSONObject) datum;
                Put put = parsePut(putJson);
                rowList.add(put);
            }
        } else if (type == Type.DELETE) {
            for (Object datum : data) {
                JSONObject deleteJson = (JSONObject) datum;
                Delete delete = parseDelete(deleteJson);
                rowList.add(delete);
            }
        }
        return new Pair<>(type, rowList);
    }
}
