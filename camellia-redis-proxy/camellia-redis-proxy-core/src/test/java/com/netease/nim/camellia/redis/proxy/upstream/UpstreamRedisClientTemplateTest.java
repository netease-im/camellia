package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import org.junit.Assert;
import org.junit.Test;

public class UpstreamRedisClientTemplateTest {

    @Test
    public void shouldRejectDbSizeInTransaction() {
        ChannelInfo channelInfo = new ChannelInfo();
        Assert.assertFalse(UpstreamRedisClientTemplate.shouldRejectDbSize(channelInfo));
        channelInfo.setInTransaction(true);
        Assert.assertTrue(UpstreamRedisClientTemplate.shouldRejectDbSize(channelInfo));
    }
}
