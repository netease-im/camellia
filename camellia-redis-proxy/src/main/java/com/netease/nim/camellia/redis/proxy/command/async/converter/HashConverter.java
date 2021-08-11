package com.netease.nim.camellia.redis.proxy.command.async.converter;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

/**
 * hash数据结构包括field和value两个组成部分，因此包括2*2=4个接口方法
 * Created by caojiajun on 2021/8/10
 */
public interface HashConverter {

    /**
     * 将原始value进行转换
     * @param commandContext command上下文
     * @param key 所属key
     * @param originalValue 原始value
     * @return 转换后的value
     */
    byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue);

    /**
     * 将转换后的value逆向恢复成原始value
     * @param commandContext command上下文
     * @param key 所属key
     * @param convertedValue 转换后的value
     * @return 原始value
     */
    byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue);

    /**
     * 将原始field进行转换
     * @param commandContext command上下文
     * @param key 所属key
     * @param originalValue 原始field
     * @return 转换后的field
     */
    byte[] fieldConvert(CommandContext commandContext, byte[] key, byte[] originalValue);

    /**
     * 将转换后的field逆向恢复成原始field
     * @param commandContext command上下文
     * @param key 所属key
     * @param convertedValue 转换后的field
     * @return 原始field
     */
    byte[] fieldReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue);
}
