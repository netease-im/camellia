package com.netease.nim.camellia.redis.proxy.plugin.converter;


/**
 * Created by caojiajun on 2021/8/6
 */
public class ConverterConfig {
    private KeyConverter keyConverter;
    private StringConverter stringConverter;
    private SetConverter setConverter;
    private ListConverter listConverter;
    private HashConverter hashConverter;
    private ZSetConverter zSetConverter;

    public KeyConverter getKeyConverter() {
        return keyConverter;
    }

    public void setKeyConverter(KeyConverter keyConverter) {
        this.keyConverter = keyConverter;
    }

    public StringConverter getStringConverter() {
        return stringConverter;
    }

    public void setStringConverter(StringConverter stringConverter) {
        this.stringConverter = stringConverter;
    }

    public SetConverter getSetConverter() {
        return setConverter;
    }

    public void setSetConverter(SetConverter setConverter) {
        this.setConverter = setConverter;
    }

    public ListConverter getListConverter() {
        return listConverter;
    }

    public void setListConverter(ListConverter listConverter) {
        this.listConverter = listConverter;
    }

    public HashConverter getHashConverter() {
        return hashConverter;
    }

    public void setHashConverter(HashConverter hashConverter) {
        this.hashConverter = hashConverter;
    }

    public ZSetConverter getzSetConverter() {
        return zSetConverter;
    }

    public void setzSetConverter(ZSetConverter zSetConverter) {
        this.zSetConverter = zSetConverter;
    }
}
