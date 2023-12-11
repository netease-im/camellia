package com.netease.nim.camellia.cache.spring;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.netease.nim.camellia.tools.compress.CamelliaCompressor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;


public class Jackson2JsonCamelliaCacheSerializer<T> implements CamelliaCacheSerializer<T> {

    private final JavaType javaType;

    private ObjectMapper objectMapper = new ObjectMapper();

    private boolean compressEnable;
    private CamelliaCompressor compressor = new CamelliaCompressor();

    private static boolean isEmpty(@Nullable byte[] data) {
        return (data == null || data.length == 0);
    }
    private static final byte[] EMPTY_ARRAY = new byte[0];

    public Jackson2JsonCamelliaCacheSerializer(Class<T> type) {
        this.javaType = getJavaType(type);
    }

    public Jackson2JsonCamelliaCacheSerializer(JavaType javaType) {
        this.javaType = javaType;
    }

    public void updateCompress(boolean compressEnable, int compressThreshold) {
        this.compressEnable = compressEnable;
        this.compressor = new CamelliaCompressor(compressThreshold);
    }

    public T deserialize(@Nullable byte[] bytes) throws CamelliaCacheSerializerException {
        if (isEmpty(bytes)) {
            return null;
        }
        bytes = compressor.decompress(bytes);
        try {
            return this.objectMapper.readValue(bytes, 0, bytes.length, javaType);
        } catch (Exception ex) {
            throw new CamelliaCacheSerializerException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] serialize(@Nullable Object t) throws CamelliaCacheSerializerException {
        if (t == null) {
            return EMPTY_ARRAY;
        }
        try {
            byte[] bytes = this.objectMapper.writeValueAsBytes(t);
            if (compressEnable) {
                bytes = compressor.compress(bytes);
            }
            return bytes;
        } catch (Exception ex) {
            throw new CamelliaCacheSerializerException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "'objectMapper' must not be null");
        this.objectMapper = objectMapper;
    }

    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }
}
