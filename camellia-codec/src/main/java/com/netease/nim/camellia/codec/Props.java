package com.netease.nim.camellia.codec;

import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Props implements IProps {

    private final Map<Integer, Value> map = new HashMap<>();

    public boolean equals(Props prop) {
        return this.map.equals(prop.map);
    }

    public Props duplicate() {
        Props ret = new Props();
        ret.map.putAll(map);
        return ret;
    }

    public String toString() {
        return toJSONObject().toString();
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject(true);
        List<Integer> tags = tags();
        Collections.sort(tags);
        for (Integer tag : tags) {
            json.put(String.valueOf(tag), map.get(tag).toString());
        }
        return json;
    }

    @Override
    public void marshal(Pack p) {
        p.putVarUint(map.size());
        for (Map.Entry<Integer, Value> entry : map.entrySet()) {
            Integer tag = entry.getKey();
            Value value = entry.getValue();
            p.putVarUint(tag);
            p.putVarbin(value.bytes);
        }
    }

    @Override
    public void unmarshal(Unpack p) {
        int cnt = p.popVarUint();
        for (int i = 0; i < cnt; ++i) {
            int tag = p.popVarUint();
            map.put(tag, new Value(p.popVarbin()));
        }
    }

    @Override
    public String getString(int tag) {
        Value v = map.get(tag);
        if (v != null) {
            return v.toString();
        }
        return null;
    }

    @Override
    public String getString(int tag, String defaultStr) {
        Value v = map.get(tag);
        if (v != null) {
            return v.toString();
        }
        return defaultStr;
    }


    @Override
    public byte[] getBytes(int tag) {
        Value v = map.get(tag);
        if (v != null) {
            return v.bytes;
        }
        return null;
    }

    @Override
    public void putBytes(int tag, byte[] bytes) {
        if (bytes == null) return;
        Value v = new Value(bytes);
        map.put(tag, v);
    }

    @Override
    public void putString(int tag, String value) {
        if (value == null) return;
        map.put(tag, new Value(value));
    }

    @Override
    public int getInteger(int tag) {
        String value = getString(tag);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getInteger(int tag, int defaultV) {
        String value = getString(tag);
        if (value == null) {
            return defaultV;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultV;
        }
    }

    @Override
    public void putInteger(int tag, int value) {
        map.put(tag, new Value(String.valueOf(value)));
    }

    @Override
    public long getLong(int tag) {
        String value = getString(tag);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    public long getLong(int tag, long defaultV) {
        String value = getString(tag);
        if (value == null) {
            return defaultV;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return defaultV;
        }
    }

    @Override
    public double getDouble(int tag){
        String value = getString(tag);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    @Override
    public void putLong(int tag, long value) {
        map.put(tag, new Value(String.valueOf(value)));
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public List<Integer> tags() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean remove(int tag) {
        return map.remove(tag) != null;
    }

    @Override
    public byte[] getBytes(int tag, byte[] defaultValue) {
        Value value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.bytes;
    }

    @Override
    public void putByte(int tag, byte value) {
        putString(tag, String.valueOf(value));
    }

    @Override
    public byte getByte(int tag) {
        String value = getString(tag);
        if (value == null) {
            return 0;
        }
        try {
            return Byte.parseByte(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public byte getByte(int tag, byte defaultValue) {
        String value = getString(tag);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Byte.parseByte(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public void putShort(int tag, short value) {
        putString(tag, String.valueOf(value));
    }

    @Override
    public short getShort(int tag) {
        String value = getString(tag);
        if (value == null) {
            return 0;
        }
        try {
            return Byte.parseByte(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public short getShort(int tag, short defaultValue) {
        String value = getString(tag);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Byte.parseByte(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public void putDouble(int tag, double value) {
        putString(tag, String.valueOf(value));
    }

    @Override
    public double getDouble(int tag, double defaultValue) {
        String value = getString(tag);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public void putFloat(int tag, float value) {
        putString(tag, String.valueOf(value));
    }

    @Override
    public float getFloat(int tag) {
        String value = getString(tag);
        if (value == null) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (Exception ex) {
            return 0.0f;
        }
    }

    @Override
    public float getFloat(int tag, float defaultValue) {
        String value = getString(tag);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    private final static class Value {
        private final String str;
        private final byte[] bytes;

        public Value(byte[] bytes) {
            if (bytes == null) {
                throw new IllegalArgumentException("bytes is null");
            }
            this.bytes = bytes;
            this.str = new String(bytes, StandardCharsets.UTF_8);
        }

        public Value(String str) {
            if (str == null) {
                throw new IllegalArgumentException("str is null");
            }
            this.str = str;
            this.bytes = str.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return str;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value value = (Value) o;
            return Objects.equals(str, value.str) && Arrays.equals(bytes, value.bytes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(str);
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }
    }
}
