package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.converter.StringConverter;
import com.netease.nim.camellia.tools.encrypt.CamelliaEncryptAesConfig;
import com.netease.nim.camellia.tools.encrypt.CamelliaEncryptor;

/**
 * Created by caojiajun on 2021/8/16
 */
public class EncryptStringConverter implements StringConverter {

    private final CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig("abc"));

    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        return encryptor.encrypt(originalValue);
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        return encryptor.decrypt(convertedValue);
    }
}
