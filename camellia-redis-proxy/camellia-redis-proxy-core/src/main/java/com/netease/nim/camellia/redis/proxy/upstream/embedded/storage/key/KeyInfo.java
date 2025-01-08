package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key;

import com.netease.nim.camellia.codec.Marshallable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.ValueLocation;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.EstimateSizeValue;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyInfo implements Marshallable, EstimateSizeValue {

    public static final KeyInfo DELETE = new KeyInfo();

    private DataType dataType;
    private byte flag;
    private byte[] key;
    private long expireTime;
    private ValueLocation valueLocation;
    private byte[] extra;

    @Override
    public long estimateSize() {
        long size = 1 + 1 + key.length + 8;
        if (valueLocation != null) {
            size += 16;
        }
        if (extra != null) {
            size += extra.length;
        }
        return size;
    }

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

    public ValueLocation setValueLocation(ValueLocation valueLocation) {
        ValueLocation oldValueLocation = this.valueLocation;
        this.valueLocation = valueLocation;
        if (valueLocation != null) {
            setContainsValue();
        } else {
            clearContainsValue();
        }
        return oldValueLocation;
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
            pack.putLong(valueLocation.blockLocation().fileId());
            pack.putInt(valueLocation.blockLocation().blockId());
            pack.putInt(valueLocation.offset());
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
            long fileId = unpack.popLong();
            int blockId = unpack.popInt();
            int offset = unpack.popInt();
            valueLocation = new ValueLocation(new BlockLocation(fileId, blockId), offset);
        }
        if (containsExtra()) {
            extra = unpack.popVarbin();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyInfo keyInfo = (KeyInfo) o;
        return Objects.deepEquals(key, keyInfo.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
