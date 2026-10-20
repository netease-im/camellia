package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;

public class CommandsRouterTest {

    private static Field queueFactoryField;
    private static Object previousQueueFactory;

    @BeforeClass
    public static void initQueueFactory() throws Exception {
        queueFactoryField = ServerConf.class.getDeclaredField("queueFactory");
        queueFactoryField.setAccessible(true);
        previousQueueFactory = queueFactoryField.get(null);
        queueFactoryField.set(null, new DefaultQueueFactory());
    }

    @AfterClass
    public static void restoreQueueFactory() throws Exception {
        queueFactoryField.set(null, previousQueueFactory);
    }

    @Test
    public void shouldRejectDbChangeDuringTransaction() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setDb(1);
        channelInfo.setInTransaction(true);

        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1));
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 2));
    }

    @Test
    public void shouldAllowFirstSelectOfBoundStandaloneDbAfterMulti() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setInTransaction(true);
        channelInfo.setBindConnection(connection(0));

        Assert.assertEquals(-1, channelInfo.getDb());
        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 0));
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1));
    }

    @Test
    public void shouldAllowFirstSelectBeforeClusterMultiBinds() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setInTransaction(true);

        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 0));
        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 2));
    }

    @Test
    public void shouldRejectDbChangeAfterWatchBinds() {
        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setBindConnection(connection(0));

        Assert.assertFalse(CommandsRouter.isDbChangeNotAllowed(channelInfo, 0));
        Assert.assertTrue(CommandsRouter.isDbChangeNotAllowed(channelInfo, 1));
        Assert.assertTrue(CommandsRouter.hasPendingBindingCommand(Collections.singletonList(task("WATCH", "key"))));
        Assert.assertTrue(CommandsRouter.hasPendingBindingCommand(Collections.singletonList(task("MULTI"))));
    }

    private static RedisConnection connection(int db) {
        RedisConnectionConfig config = new RedisConnectionConfig();
        config.setHost("127.0.0.1");
        config.setPort(6379);
        config.setDb(db);
        return new RedisConnection(config);
    }

    private static CommandTask task(String... args) {
        byte[][] objects = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            objects[i] = args[i].getBytes();
        }
        return new CommandTask(null, new Command(objects), null);
    }
}
