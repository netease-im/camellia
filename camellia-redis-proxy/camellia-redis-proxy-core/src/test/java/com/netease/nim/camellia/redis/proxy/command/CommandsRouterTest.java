package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class CommandsRouterTest {

    @Test
    public void shouldRejectDbChangeDuringTransaction() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setDb(1);
        channelInfo.setInTransaction(true);

        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1));
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 2));
    }

    @Test
    public void shouldRejectDbChangeWhileMultiIsPendingInSameBatch() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setDb(0);
        CommandTask multi = task("MULTI");
        CommandTask exec = task("EXEC");

        Assert.assertFalse(channelInfo.isInTransaction());
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1, Collections.singletonList(multi)));
        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 0, Collections.singletonList(multi)));
        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1, Arrays.asList(multi, exec)));

        channelInfo.setDb(-1);
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1, Collections.singletonList(multi)));
    }

    private static CommandTask task(String name) {
        return new CommandTask(null, new Command(new byte[][]{name.getBytes()}), null);
    }
}
