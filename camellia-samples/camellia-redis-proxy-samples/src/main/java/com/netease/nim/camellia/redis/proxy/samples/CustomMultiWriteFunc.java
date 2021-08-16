package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.async.interceptor.MultiWriteCommandInterceptor;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * 表示只有k1这个这个key需要双写，且双写到redis://abc@127.0.0.1:6390，其他key不需要双写
 * Created by caojiajun on 2021/7/22
 */
public class CustomMultiWriteFunc implements MultiWriteCommandInterceptor.MultiWriteFunc {

    @Override
    public MultiWriteCommandInterceptor.MultiWriteInfo multiWriteInfo(MultiWriteCommandInterceptor.KeyInfo keyInfo) {
        byte[] key = keyInfo.getKey();
        if (Utils.bytesToString(key).equals("k1")) {
            return new MultiWriteCommandInterceptor.MultiWriteInfo(true, "redis://abc@127.0.0.1:6390");
        }
        return MultiWriteCommandInterceptor.MultiWriteInfo.SKIP_MULTI_WRITE;
    }
}
