package com.netease.nim.camellia.cache.spring;


public interface CamelliaCacheSerializer<T> {

    byte[] serialize(T t) throws CamelliaCacheSerializerException;

    T deserialize(byte[] bytes) throws CamelliaCacheSerializerException;

}
