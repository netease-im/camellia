package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;

/**
 *
 * Created by caojiajun on 2021/8/10
 */
public interface ListConverter {

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
}
