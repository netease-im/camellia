package com.netease.nim.camellia.redis.proxy.command.async.converter;

/**
 * Created by caojiajun on 2021/8/6
 */
public class ConverterConfig {
    private StringConverter stringConverter;

    public StringConverter getStringConverter() {
        return stringConverter;
    }

    public void setStringConverter(StringConverter stringConverter) {
        this.stringConverter = stringConverter;
    }
}
