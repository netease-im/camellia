package com.netease.nim.camellia.redis.proxy.upstream.local.storage;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisLocalStorageResource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.*;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.db.MemFlushCommand;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact.CompactExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.Wal;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.key_max_len;

/**
 * Created by caojiajun on 2025/1/3
 */
public class RedisLocalStorageClient implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisLocalStorageClient.class);

    private final ConcurrentHashMap<Short, Long> timeMap = new ConcurrentHashMap<>();

    private final Resource resource;
    private final String dir;
    private Commands commands;
    private final SlotLock slotLock = new SlotLock();

    public RedisLocalStorageClient(RedisLocalStorageResource resource) {
        this.resource = resource;
        this.dir = resource.getDir();
    }

    @Override
    public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (db > 0) {
            for (CompletableFuture<Reply> future : futureList) {
                future.complete(ErrorReply.DB_INDEX_OUT_OF_RANGE);
            }
            return;
        }
        for (int i=0; i<commands.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futureList.get(i);
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand.getCommandKeyType() == RedisCommand.CommandKeyType.None) {
                sendNoneKeyCommand(redisCommand, command, future);
            } else {
                List<byte[]> keys = command.getKeys();
                for (byte[] key : keys) {
                    if (key.length > key_max_len) {
                        future.complete(ErrorReply.KEY_TOO_LONG);
                        return;
                    }
                }
                if (keys.size() == 1) {
                    byte[] key = keys.getFirst();
                    sendCommand(redisCommand, key, command, future);
                } else {
                    sendMultiKeyCommand(redisCommand, command, future);
                }
            }
        }
    }

    @Override
    public void start() {
        try {
            LocalStorageReadWrite readWrite = new LocalStorageReadWrite(dir);
            CompactExecutor compactExecutor = new CompactExecutor(readWrite);

            Wal wal = new Wal(readWrite);
            wal.recover();

            CommandConfig commandConfig = new CommandConfig();
            commandConfig.setCompactExecutor(compactExecutor);
            commandConfig.setReadWrite(readWrite);
            commandConfig.setWal(wal);

            commands = new Commands(commandConfig);

            ScheduledExecutorService scheduler = LocalStorageExecutors.getInstance().getScheduler();
            scheduler.scheduleAtFixedRate(this::flush, 10, 10, TimeUnit.SECONDS);

            logger.info("local storage client start success, dir = {}", dir);
        } catch (Exception e) {
            logger.error("local storage client start error, dir = {}", dir, e);
            throw new CamelliaRedisException(e);
        }
    }

    private void flush() {
        try {
            int seconds = ProxyDynamicConf.getInt("local.storage.flush.max.interval.seconds", 600);
            for (short slot=0; slot<RedisClusterCRC16Utils.SLOT_SIZE; slot++) {
                boolean flush;
                Long lastWriteCommandTime = timeMap.get(slot);
                if (lastWriteCommandTime == null) {
                    flush = false;
                    timeMap.put(slot, TimeCache.currentMillis);
                } else {
                    flush = TimeCache.currentMillis - lastWriteCommandTime >= seconds * 1000L;
                }
                if (!flush) continue;
                ReentrantLock lock = slotLock.getLock(slot);
                lock.lock();
                try {
                    ICommand invoker = commands.getCommandInvoker(RedisCommand.MEMFLUSH);
                    commands.execute(invoker, slot, MemFlushCommand.command);
                    timeMap.put(slot, TimeCache.currentMillis);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisLocalStorageClient.class, "send memflush command error, slot = " + slot, e);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.error("send memflush command error", e);
        }
    }

    private void sendCommand(RedisCommand redisCommand, byte[] key, Command command, CompletableFuture<Reply> future) {
        try {
            short slot = (short) RedisClusterCRC16Utils.getSlot(key);
            ICommand invoker = commands.getCommandInvoker(redisCommand);
            if (invoker == null) {
                future.complete(Utils.commandNotSupport(redisCommand));
                return;
            }
            if (!commands.parse(invoker, command)) {
                future.complete(ErrorReply.argNumWrong(redisCommand));
                return;
            }

            if (redisCommand.getType() == RedisCommand.Type.WRITE) {
                timeMap.put(slot, TimeCache.currentMillis);
            }

            try {
                ReentrantLock lock = slotLock.getLock(slot);
                lock.lock();
                //
                Reply reply;
                try {
                    reply = commands.execute(invoker, slot, command);
                } finally {
                    lock.unlock();
                }
                if (reply == null) {
                    ErrorLogCollector.collect(RedisLocalStorageClient.class, "command receive null reply, command = " + command.getName());
                }
                future.complete(reply);
            } catch (Exception e) {
                ErrorLogCollector.collect(RedisLocalStorageClient.class, "send command error, command = " + command.getName(), e);
                future.complete(ErrorReply.NOT_AVAILABLE);
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisLocalStorageClient.class, "send command error, command = " + command.getName(), e);
            future.complete(ErrorReply.INTERNAL_ERROR);
        }
    }

    private void sendNoneKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        if (redisCommand == RedisCommand.PING) {
            future.complete(StatusReply.PONG);
        } else if (redisCommand == RedisCommand.ECHO) {
            byte[][] objects = command.getObjects();
            if (objects.length == 2) {
                future.complete(new BulkReply(objects[1]));
            } else {
                future.complete(ErrorReply.argNumWrong(redisCommand));
            }
        } else {
            future.complete(Utils.commandNotSupport(redisCommand));
        }
    }

    private void sendMultiKeyCommand(RedisCommand redisCommand, Command command, CompletableFuture<Reply> future) {
        List<byte[]> keys = command.getKeys();
        if (redisCommand == RedisCommand.DEL || redisCommand == RedisCommand.UNLINK || redisCommand == RedisCommand.EXISTS) {
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (byte[] key : keys) {
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(redisCommand, key, new Command(new byte[][]{redisCommand.raw(), key}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> future.complete(Utils.mergeIntegerReply(replies)));
        } else if (redisCommand == RedisCommand.MGET) {
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (byte[] key : keys) {
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(RedisCommand.GET, key, new Command(new byte[][]{RedisCommand.GET.raw(), key}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        future.complete(reply);
                        return;
                    }
                }
                future.complete(new MultiBulkReply(replies.toArray(new Reply[0])));
            });
        } else if (redisCommand == RedisCommand.MSET) {
            byte[][] objects = command.getObjects();
            if (objects.length < 3 || objects.length % 2 == 0) {
                future.complete(ErrorReply.argNumWrong(redisCommand));
                return;
            }
            List<CompletableFuture<Reply>> futures = new ArrayList<>(keys.size());
            for (int i=1; i<objects.length; i+=2) {
                byte[] key = objects[i];
                byte[] value = objects[i+1];
                CompletableFuture<Reply> f = new CompletableFuture<>();
                sendCommand(RedisCommand.SET, key, new Command(new byte[][]{RedisCommand.SET.raw(), key, value}), f);
                futures.add(f);
            }
            CompletableFutureUtils.allOf(futures).thenAccept(replies -> {
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        future.complete(reply);
                        return;
                    }
                }
                future.complete(StatusReply.OK);
            });
        } else {
            future.complete(Utils.commandNotSupport(redisCommand));
        }
    }

    @Override
    public void preheat() {
        //do nothing
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void shutdown() {
        //do nothing
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void renew() {
        //do nothing
    }
}
