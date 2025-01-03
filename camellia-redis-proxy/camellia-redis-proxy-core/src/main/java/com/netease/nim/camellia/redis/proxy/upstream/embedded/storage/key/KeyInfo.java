package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key;

import com.netease.nim.camellia.codec.Marshallable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.ValueLocation;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyInfo implements Marshallable {

    public static final KeyInfo DELETE = new KeyInfo();

    private DataType dataType;
    private byte flag;
    private byte[] key;
    private long expireTime;
    private ValueLocation valueLocation;
    private byte[] extra;

    public static enum FlagType {
        DEFAULT((byte) 0),
        CONTAINS_EXPIRE_TIME((byte) 1),
        CONTAINS_EXTRA((byte) 2),
        CONTAINS_VALUE((byte) 4),
        ;

        private final byte value;

        FlagType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static FlagType getByValue(int tagValue) {
            for (FlagType t : FlagType.values()) {
                if (t.getValue() == tagValue) {
                    return t;
                }
            }
            return DEFAULT;
        }
    }

    public KeyInfo() {
    }

    public KeyInfo(DataType dataType) {
        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public boolean isExpire() {
        if (expireTime <= 0) {
            return false;
        }
        return expireTime <= System.currentTimeMillis();
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
        if (expireTime > 0) {
            setContainsExpireTime();
        } else {
            clearContainsExpireTime();
        }
    }

    public byte[] getExtra() {
        return extra;
    }

    public void setExtra(byte[] extra) {
        this.extra = extra;
        if (extra != null) {
            setContainsExtra();
        } else {
            clearContainsExtra();
        }
    }

    public void setValueLocation(ValueLocation valueLocation) {
        this.valueLocation = valueLocation;
        if (valueLocation != null) {
            setContainsValue();
        } else {
            clearContainsValue();
        }
    }

    public ValueLocation getValueLocation() {
        return valueLocation;
    }

    public void setContainsExpireTime() {
        flag |= FlagType.CONTAINS_EXPIRE_TIME.getValue();
    }

    public void setContainsExtra() {
        flag |= FlagType.CONTAINS_EXTRA.getValue();
    }

    public void setContainsValue() {
        flag |= FlagType.CONTAINS_VALUE.getValue();
    }

    public void clearContainsExpireTime() {
        flag &= (byte) ~ FlagType.CONTAINS_EXPIRE_TIME.getValue();
    }

    public void clearContainsExtra() {
        flag &= (byte) ~ FlagType.CONTAINS_EXTRA.getValue();
    }

    public void clearContainsValue() {
        flag &= (byte) ~ FlagType.CONTAINS_VALUE.getValue();
    }

    public boolean containsExpireTime() {
        return 0 != (flag & FlagType.CONTAINS_EXPIRE_TIME.getValue());
    }

    public boolean containsExtra() {
        return 0 != (flag & FlagType.CONTAINS_EXTRA.getValue());
    }

    public boolean containsValue() {
        return 0 != (flag & FlagType.CONTAINS_VALUE.getValue());
    }

    @Override
    public void marshal(Pack pack) {
        pack.putByte(dataType.getValue());
        pack.putByte(flag);
        pack.putVarbin(key);
        if (containsExpireTime()) {
            pack.putLong(expireTime);
        }
        if (containsValue()) {
            pack.putLong(valueLocation.fileId());
            pack.putLong(valueLocation.offset());
        }
        if (containsExtra()) {
            pack.putVarbin(extra);
        }
    }

    @Override
    public void unmarshal(Unpack unpack) {
        dataType = DataType.getByValue(unpack.popByte());
        flag = unpack.popByte();
        key = unpack.popVarbin();
        if (containsExpireTime()) {
            expireTime = unpack.popLong();
        }
        if (containsValue()) {
            long valueFileId = unpack.popLong();
            long valueOffset = unpack.popLong();
            valueLocation = new ValueLocation(valueFileId, valueOffset);
        }
        if (containsExtra()) {
            extra = unpack.popVarbin();
        }
    }
}
