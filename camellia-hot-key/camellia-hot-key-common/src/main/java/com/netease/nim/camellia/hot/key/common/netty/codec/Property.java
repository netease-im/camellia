package com.netease.nim.camellia.hot.key.common.netty.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Property implements Marshallable {

    public Map<Integer, Value> props = new HashMap<>();

    public void marshal(Pack p) {
        p.putVarUint(props.size());
        for (Iterator<Integer> it = iterator(); it.hasNext();) {
            int tag = it.next();
            p.putVarUint(tag);
            Value value = props.get(tag);
            p.putVarbin(value.bytes);
        }
    }

    public void unmarshal(Unpack p) {
        int cnt = p.popVarUint();
        for (int i = 0; i < cnt; ++i) {
            int tag = p.popVarUint();
            props.put(tag, new Value(p.popVarbin()));
        }
    }

    public boolean equals(Property prop) {
        return this.props.equals(prop.props);
    }

    public Iterator<Integer> iterator() {
        return props.keySet().iterator();
    }

    public String get(Integer tag) {
        Value v = props.get(tag);

        if (v != null) {
            return props.get(tag).toString();
        }
        return null;
    }


    public String get(Integer tag, String defaultStr) {
        Value v = props.get(tag);
        if (v != null) {
            return props.get(tag).toString();
        }
        return defaultStr;
    }


    public byte[] getBytes(Integer tag) {
        Value v = props.get(tag);
        if (v != null) {
            return v.bytes;
        }
        return null;
    }

    public void putBytes(Integer tag, byte[] bytes) {
        Value v = new Value(bytes);
        props.put(tag, v);
    }

    public void put(Integer tag, String value) {
        props.put(tag, new Value(value));
    }

    public int getInteger(Integer tag) {
        String value = get(tag);
        if (value == null || value.equals(""))
            return 0;
        return Integer.parseInt(value);
    }

    public int getInteger(Integer tag, Integer defaultV) {
        String value = get(tag);
        if (value == null || value.equals(""))
            return defaultV;
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultV;
        }
    }

    public void putInteger(Integer tag, int value) {
        props.put(tag, new Value(String.valueOf(value)));
    }

    public long getLong(Integer tag) {
        String value = get(tag);
        try {
            if (value == null || value.equals(""))
                return 0;
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    public long getLong(Integer tag,long defaultVal) {
        String value = get(tag);
        try {
            if (value == null || value.equals(""))
                return 0;
            return Long.parseLong(value);
        } catch (Exception ex) {
            return defaultVal;
        }
    }

    public double getDouble(Integer tag){
        String value = get(tag);
        try {
            if (value == null || value.equals(""))
                return 0.0;
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    public void putLong(Integer tag, long value) {
        props.put(tag, new Value(String.valueOf(value)));
    }

    public int size() {
        return props.size();
    }

    public void clear() {
        props.clear();
    }

    public Property duplicate() {
        Property ret = new Property();

        for (Map.Entry<Integer, Value> entry: props.entrySet()) {
            ret.putValue(entry.getKey(), entry.getValue());
        }

        return ret;
    }

    public void putValue(Integer key, Value value) {
        props.put(key, value);
    }

    public static byte[] fromString(String str) {
        return (str == null ? "" : str).getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] bytes) {
        if (bytes != null) {
            return new String(bytes, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public Value remove(Integer tag) {
        return props.remove(tag);
    }

    public String toString() {
        return toJSONObject().toString();
    }

    public JSONObject toJSONObject() {
        JSONObject jo = new JSONObject(true);
        for (Map.Entry<Integer, Value> entry: props.entrySet()) {
            jo.put(String.valueOf(entry.getKey()), entry.getValue().toString());
        }
        return jo;
    }

    public JSONObject toJSONObject(final Map<String, String> readableKeyMap) {
        JSONObject jo = new JSONObject(true);
        for (Map.Entry<Integer, Value> entry: props.entrySet()) {
            String key = String.valueOf(entry.getKey());
            key = String.format("%s [%s]", key, readableKeyMap.get(key));
            jo.put(key, entry.getValue().toString());
        }
        return jo;
    }

    public String toJSONString() {
        return JSON.toJSONStringZ(toJSONObject(),
            SerializeConfig.getGlobalInstance(),
            SerializerFeature.PrettyFormat, SerializerFeature.QuoteFieldNames);
    }

    public Collection<String> values() {
        ArrayList<String> list = new ArrayList<String>(props.size());
        for (Value value: props.values()) {
            list.add(value.toString());
        }
        return list;
    }

    public boolean containsKey(int key) {
        return props.containsKey(key);
    }

    public final static class Value {
        private String str;
        private byte[] bytes;

        public Value(byte[] bytes) {
            super();
            setBytes(bytes);
        }

        public Value(String str) {
            super();
            set(str);
        }

        private void set(String str) {
            this.str = str;
            this.bytes = fromString(str);
        }

        private void setBytes(byte[] bytes) {
            this.str = null;
            if (bytes == null) {
                this.bytes = fromString("");
                this.str = "";
            } else {
                this.bytes = bytes;
            }
        }

        @Override
        public String toString() {
            if (str == null) {
                str = fromBytes(bytes);
            }
            return str;
        }

    }
}
