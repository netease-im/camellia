package com.netease.nim.camellia.codec;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/26
 */
public interface IProps extends Marshallable {

    void putString(int tag, String value);

    String getString(int tag);

    String getString(int tag, String defaultValue);

    void putBytes(int tag, byte[] value);

    byte[] getBytes(int tag);

    byte[] getBytes(int tag, byte[] defaultValue);

    void putByte(int tag, byte value);

    byte getByte(int tag);

    byte getByte(int tag, byte defaultValue);

    void putShort(int tag, short value);

    short getShort(int tag);

    short getShort(int tag, short defaultValue);

    void putInteger(int tag, int value);

    int getInteger(int tag);

    int getInteger(int tag, int defaultValue);

    void putLong(int tag, long value);

    long getLong(int tag);

    long getLong(int tag, long defaultValue);

    void putDouble(int tag, double value);

    double getDouble(int tag);

    double getDouble(int tag, double defaultValue);

    void putFloat(int tag, float value);

    float getFloat(int tag);

    float getFloat(int tag, float defaultValue);

    boolean containsKey(int tag);

    int size();

    void clear();

    boolean remove(int tag);

    List<Integer> tags();
}
