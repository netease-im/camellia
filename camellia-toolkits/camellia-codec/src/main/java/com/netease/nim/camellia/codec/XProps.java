package com.netease.nim.camellia.codec;

import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * 在某些场景下，会比Props更节省内存一点
 * Created by caojiajun on 2023/7/26
 */
public class XProps implements IProps {

    private final Map<Integer, XValue> map = new HashMap<>();

    public XProps duplicate() {
        XProps ret = new XProps();
        ret.map.putAll(map);
        return ret;
    }

    public boolean equals(XProps prop) {
        return this.map.equals(prop.map);
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
    public void putString(int tag, String value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public String getString(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return null;
        }
        return value.getStr();
    }

    @Override
    public String getString(int tag, String defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getStr();
    }

    @Override
    public void putBytes(int tag, byte[] value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public byte[] getBytes(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return null;
        }
        return value.getBytes();
    }

    @Override
    public byte[] getBytes(int tag, byte[] defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getBytes();
    }

    @Override
    public void putByte(int tag, byte value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public byte getByte(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0;
        }
        return value.getByte();
    }

    @Override
    public byte getByte(int tag, byte defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getByte();
    }

    @Override
    public void putShort(int tag, short value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public short getShort(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0;
        }
        return value.getShort();
    }

    @Override
    public short getShort(int tag, short defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getShort();
    }

    @Override
    public void putInteger(int tag, int value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public int getInteger(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0;
        }
        return value.getInt();
    }

    @Override
    public int getInteger(int tag, int defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getInt();
    }

    @Override
    public void putLong(int tag, long value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public long getLong(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0;
        }
        return value.getLong();
    }

    @Override
    public long getLong(int tag, long defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getLong();
    }

    @Override
    public void putDouble(int tag, double value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public double getDouble(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0.0;
        }
        return value.getDouble();
    }

    @Override
    public double getDouble(int tag, double defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getDouble();
    }

    @Override
    public void putFloat(int tag, float value) {
        map.put(tag, new XValue(value));
    }

    @Override
    public float getFloat(int tag) {
        XValue value = map.get(tag);
        if (value == null) {
            return 0.0f;
        }
        return value.getFloat();
    }

    @Override
    public float getFloat(int tag, float defaultValue) {
        XValue value = map.get(tag);
        if (value == null) {
            return defaultValue;
        }
        return value.getFloat();
    }

    @Override
    public boolean containsKey(int tag) {
        return map.containsKey(tag);
    }

    @Override
    public int size() {
        return map.size();
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
    public List<Integer> tags() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    public void marshal(Pack p) {
        p.putVarUint(map.size());
        for (Map.Entry<Integer, XValue> entry : map.entrySet()) {
            p.putVarUint(entry.getKey());
            p.putMarshallable(entry.getValue());
        }
    }

    @Override
    public void unmarshal(Unpack p) {
        int cnt = p.popVarUint();
        for (int i = 0; i < cnt; ++i) {
            int tag = p.popVarUint();
            XValue value = new XValue();
            p.popMarshallable(value);
            map.put(tag, value);
        }
    }

    private static class XValue implements Marshallable {
        private static final byte BYTE = (byte) 1;
        private static final byte SHORT = (byte) 2;
        private static final byte INT = (byte) 3;
        private static final byte LONG = (byte) 4;
        private static final byte DOUBLE = (byte) 5;
        private static final byte FLOAT = (byte) 6;
        private static final byte BYTE_ARRAY = (byte) 7;
        private static final byte STRING = (byte) 8;

        private byte type;
        private Marshallable data;

        public XValue() {
        }

        public XValue(byte value) {
            this.type = BYTE;
            this.data = new ByteMable(value);
        }

        public XValue(short value) {
            this.type = SHORT;
            this.data = new ShortMable(value);
        }

        public XValue(int value) {
            this.type = INT;
            this.data = new VarIntMable(value);
        }

        public XValue(long value) {
            this.type = LONG;
            this.data = new VarLongMable(value);
        }

        public XValue(double value) {
            this.type = DOUBLE;
            this.data = new DoubleMable(value);
        }

        public XValue(float value) {
            this.type = FLOAT;
            this.data = new FloatMable(value);
        }

        public XValue(byte[] value) {
            this.type = BYTE_ARRAY;
            this.data = new BytesMable(value);
        }

        public XValue(String value) {
            this.type = STRING;
            this.data = new StrMable(value);
        }

        @Override
        public void marshal(Pack pack) {
            pack.putByte(type);
            pack.putMarshallable(data);
        }

        @Override
        public void unmarshal(Unpack unpack) {
            type = unpack.popByte();
            if (type == BYTE) {
                data = new ByteMable();
            } else if (type == SHORT) {
                data = new ShortMable();
            } else if (type == INT) {
                data = new VarIntMable();
            } else if (type == LONG) {
                data = new VarLongMable();
            } else if (type == DOUBLE) {
                data = new DoubleMable();
            } else if (type == FLOAT) {
                data = new FloatMable();
            } else if (type == BYTE_ARRAY) {
                data = new BytesMable();
            } else if (type == STRING) {
                data = new StrMable();
            } else {
                throw new IllegalArgumentException("unknown type=" + type);
            }
            unpack.popMarshallable(data);
        }

        private String name(byte type) {
            if (type == BYTE) {
                return "BYTE";
            } else if (type == SHORT) {
                return "SHORT";
            } else if (type == INT) {
                return "INT";
            } else if (type == LONG) {
                return "LONG";
            } else if (type == DOUBLE) {
                return "DOUBLE";
            } else if (type == FLOAT) {
                return "FLOAT";
            } else if (type == BYTE_ARRAY) {
                return "BYTE_ARRAY";
            } else if (type == STRING) {
                return "STRING";
            } else {
                return "UNKNOWN=" + type;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("type=").append(name(type)).append(",");
            if (type == BYTE) {
                builder.append("value=").append(getByte());
            } else if (type == SHORT) {
                builder.append("value=").append(getShort());
            } else if (type == INT) {
                builder.append("value=").append(getInt());
            } else if (type == LONG) {
                builder.append("value=").append(getLong());
            } else if (type == DOUBLE) {
                builder.append("value=").append(getDouble());
            } else if (type == FLOAT) {
                builder.append("value=").append(getFloat());
            } else if (type == BYTE_ARRAY) {
                builder.append("value=").append(Base64.getEncoder().encodeToString(getBytes()));
            } else if (type == STRING) {
                builder.append("value=").append(getStr());
            }
            return builder.toString();
        }

        public byte getByte() {
            if (type != BYTE) {
                throw new IllegalArgumentException("type not match, expect " + name(BYTE) + ", actual " + name(type));
            }
            return ((ByteMable) data).getData();
        }

        public short getShort() {
            if (type != SHORT) {
                throw new IllegalArgumentException("type not match, expect " + name(SHORT) + ", actual " + name(type));
            }
            return ((ShortMable) data).getData();
        }

        public int getInt() {
            if (type != INT) {
                throw new IllegalArgumentException("type not match, expect " + name(INT) + ", actual " + name(type));
            }
            return ((VarIntMable) data).getData();
        }

        public long getLong() {
            if (type != LONG) {
                throw new IllegalArgumentException("type not match, expect " + name(LONG) + ", actual " + name(type));
            }
            return ((VarLongMable) data).getData();
        }

        public double getDouble() {
            if (type != DOUBLE) {
                throw new IllegalArgumentException("type not match, expect " + name(DOUBLE) + ", actual " + name(type));
            }
            return ((DoubleMable) data).getData();
        }

        public float getFloat() {
            if (type != FLOAT) {
                throw new IllegalArgumentException("type not match, expect " + name(FLOAT) + ", actual " + name(type));
            }
            return ((FloatMable) data).getData();
        }

        public String getStr() {
            if (type != STRING) {
                throw new IllegalArgumentException("type not match, expect " + name(STRING) + ", actual " + name(type));
            }
            return ((StrMable) data).getData();
        }

        public byte[] getBytes() {
            if (type != BYTE_ARRAY) {
                throw new IllegalArgumentException("type not match, expect " + name(BYTE_ARRAY) + ", actual " + name(type));
            }
            return ((BytesMable) data).getData();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XValue value = (XValue) o;
            if (type != value.type) {
                return false;
            }
            if (data == null && value.data != null) {
                return false;
            }
            if (data != null && value.data == null) {
                return false;
            }
            if (type == BYTE) {
                return getByte() == value.getByte();
            } else if (type == SHORT) {
                return getShort() == value.getShort();
            } else if (type == INT) {
                return getInt() == value.getInt();
            } else if (type == LONG) {
                return getLong() == value.getLong();
            } else if (type == DOUBLE) {
                return getDouble() == value.getDouble();
            } else if (type == FLOAT) {
                return getFloat() == value.getFloat();
            } else if (type == BYTE_ARRAY) {
                return Arrays.equals(getBytes(), value.getBytes());
            } else if (type == STRING) {
                return getStr().equals(value.getStr());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
