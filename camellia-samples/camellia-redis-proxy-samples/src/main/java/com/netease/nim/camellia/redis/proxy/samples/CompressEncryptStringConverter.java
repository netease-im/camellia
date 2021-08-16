package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.converter.StringConverter;
import com.netease.nim.camellia.tools.CamelliaCompressor;
import com.netease.nim.camellia.tools.CamelliaEncryptAesConfig;
import com.netease.nim.camellia.tools.CamelliaEncryptor;

/**
 * Created by caojiajun on 2021/8/16
 */
public class CompressEncryptStringConverter implements StringConverter {

    private final CamelliaCompressor compressor = new CamelliaCompressor();
    private final CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig("abc"));

    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        return encryptor.encrypt(compressor.compress(originalValue));
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        return compressor.decompress(encryptor.decrypt(convertedValue));
    }
}
