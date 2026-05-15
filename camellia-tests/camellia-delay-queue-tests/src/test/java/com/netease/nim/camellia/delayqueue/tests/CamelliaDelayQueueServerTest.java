package com.netease.nim.camellia.delayqueue.tests;

import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsg;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgAckRequest;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgAckResponse;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgDeleteRequest;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgDeleteResponse;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgGetRequest;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgGetResponse;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgPullRequest;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgPullResponse;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgSendRequest;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgSendResponse;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgStatus;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayMsgErrorCode;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServerConfig;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests server-side delay queue semantics without requiring an external Redis.
 */
public class CamelliaDelayQueueServerTest {

    private static final int SUCCESS = CamelliaDelayMsgErrorCode.SUCCESS.getValue();

    private InMemoryRedisTemplate redis;
    private CamelliaDelayQueueServer server;
    private String topic;

    @Before
    public void setUp() throws Exception {
        redis = InMemoryRedisTemplate.create();
        CamelliaDelayQueueServerConfig config = new CamelliaDelayQueueServerConfig();
        config.setNamespace("test-" + UUID.randomUUID().toString().replace("-", ""));
        config.setAckTimeoutMillis(20);
        config.setTtlMillis(300);
        config.setMaxRetry(1);
        config.setEndLifeMsgExpireMillis(1000);
        server = newServer(config, redis);
        topic = "topic-" + UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    public void shouldSendPullAckAndGetConsumedMessage() {
        CamelliaDelayMsg sent = send("msg-1", "payload", 0, 1000, 1);
        Assert.assertEquals(CamelliaDelayMsgStatus.READY.getValue(), sent.getStatus());

        CamelliaDelayMsgPullResponse pullResponse = pull(1, 100);
        Assert.assertEquals(SUCCESS, pullResponse.getCode());
        Assert.assertEquals(1, pullResponse.getDelayMsgList().size());
        CamelliaDelayMsg pulled = pullResponse.getDelayMsgList().get(0);
        Assert.assertEquals("msg-1", pulled.getMsgId());
        Assert.assertEquals("payload", pulled.getMsg());
        Assert.assertEquals(CamelliaDelayMsgStatus.CONSUMING.getValue(), pulled.getStatus());
        Assert.assertEquals(1, pulled.getRetry());

        CamelliaDelayMsgAckResponse ackResponse = ack("msg-1", true);
        Assert.assertEquals(SUCCESS, ackResponse.getCode());

        CamelliaDelayMsgGetResponse getResponse = get("msg-1");
        Assert.assertEquals(SUCCESS, getResponse.getCode());
        Assert.assertEquals(CamelliaDelayMsgStatus.CONSUME_OK.getValue(), getResponse.getDelayMsg().getStatus());
        Assert.assertTrue(pull(1, 100).getDelayMsgList().isEmpty());
    }

    @Test
    public void shouldHoldDelayedMessageUntilTriggerTime() throws Exception {
        send("msg-2", "delayed", 200, 1000, 1);

        Assert.assertTrue(pull(1, 100).getDelayMsgList().isEmpty());

        Thread.sleep(260);
        redis.triggerDueWaitingMessages(topic);

        CamelliaDelayMsgPullResponse pullResponse = pull(1, 100);
        Assert.assertEquals(1, pullResponse.getDelayMsgList().size());
        Assert.assertEquals("msg-2", pullResponse.getDelayMsgList().get(0).getMsgId());
    }

    @Test
    public void shouldRetryMessageAfterAckTimeout() throws Exception {
        send("msg-3", "retry", 0, 1000, 1);

        CamelliaDelayMsg first = pull(1, 20).getDelayMsgList().get(0);
        Assert.assertEquals(1, first.getRetry());
        Assert.assertTrue(pull(1, 20).getDelayMsgList().isEmpty());

        Thread.sleep(60);
        redis.retryTimedOutAckMessages(topic);

        CamelliaDelayMsg second = pull(1, 20).getDelayMsgList().get(0);
        Assert.assertEquals("msg-3", second.getMsgId());
        Assert.assertEquals(2, second.getRetry());
    }

    @Test
    public void shouldExpireAfterRetryExhausted() throws Exception {
        send("msg-4", "retry-exhaust", 0, 1000, 0);

        CamelliaDelayMsg first = pull(1, 20).getDelayMsgList().get(0);
        Assert.assertEquals(1, first.getRetry());
        Thread.sleep(60);
        redis.retryTimedOutAckMessages(topic);

        CamelliaDelayMsgPullResponse secondPull = pull(1, 20);
        Assert.assertTrue(secondPull.getDelayMsgList().isEmpty());

        CamelliaDelayMsgGetResponse getResponse = get("msg-4");
        Assert.assertEquals(SUCCESS, getResponse.getCode());
        Assert.assertEquals(CamelliaDelayMsgStatus.RETRY_EXHAUST.getValue(), getResponse.getDelayMsg().getStatus());
    }

    @Test
    public void shouldDeleteMessageAndReportMissingForUnknownMessage() {
        send("msg-5", "delete", 0, 1000, 1);

        CamelliaDelayMsgDeleteRequest deleteRequest = new CamelliaDelayMsgDeleteRequest();
        deleteRequest.setTopic(topic);
        deleteRequest.setMsgId("msg-5");
        CamelliaDelayMsgDeleteResponse deleteResponse = server.deleteMsg(deleteRequest);
        Assert.assertEquals(SUCCESS, deleteResponse.getCode());

        CamelliaDelayMsgGetResponse getResponse = get("msg-5");
        Assert.assertEquals(SUCCESS, getResponse.getCode());
        Assert.assertEquals(CamelliaDelayMsgStatus.DELETE.getValue(), getResponse.getDelayMsg().getStatus());

        CamelliaDelayMsgDeleteRequest missingRequest = new CamelliaDelayMsgDeleteRequest();
        missingRequest.setTopic(topic);
        missingRequest.setMsgId("missing");
        Assert.assertEquals(CamelliaDelayMsgErrorCode.NOT_EXISTS.getValue(), server.deleteMsg(missingRequest).getCode());
    }

    @Test
    public void shouldRejectInvalidAndDeduplicateMessageId() {
        CamelliaDelayMsgSendRequest invalid = new CamelliaDelayMsgSendRequest();
        invalid.setTopic(topic);
        invalid.setMsg("");
        invalid.setDelayMillis(0);
        Assert.assertEquals(CamelliaDelayMsgErrorCode.PARAM_WRONG.getValue(), server.sendMsg(invalid).getCode());

        CamelliaDelayMsg first = send("same-id", "first", 0, 1000, 1);
        CamelliaDelayMsg second = send("same-id", "second", 0, 1000, 1);
        Assert.assertEquals(first.getMsg(), second.getMsg());
        Assert.assertEquals(first.getMsgId(), second.getMsgId());
        Assert.assertEquals(1, redis.readySize(topic));
    }

    @Test
    public void shouldConsumeBatchWithoutDuplicateTerminalAck() {
        int count = 20;
        for (int i = 0; i < count; i++) {
            send("batch-" + i, "payload-" + i, 0, 1000, 1);
        }

        Set<String> consumed = new HashSet<>();
        while (consumed.size() < count) {
            CamelliaDelayMsgPullResponse response = pull(5, 100);
            Assert.assertFalse(response.getDelayMsgList().isEmpty());
            for (CamelliaDelayMsg msg : response.getDelayMsgList()) {
                Assert.assertTrue("duplicate message " + msg.getMsgId(), consumed.add(msg.getMsgId()));
                Assert.assertEquals(SUCCESS, ack(msg.getMsgId(), true).getCode());
            }
        }

        Assert.assertTrue(pull(5, 100).getDelayMsgList().isEmpty());
        Assert.assertEquals(count, consumed.size());
    }

    private CamelliaDelayMsg send(String msgId, String payload, long delayMillis, long ttlMillis, int maxRetry) {
        CamelliaDelayMsgSendRequest request = new CamelliaDelayMsgSendRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        request.setMsg(payload);
        request.setDelayMillis(delayMillis);
        request.setTtlMillis(ttlMillis);
        request.setMaxRetry(maxRetry);
        CamelliaDelayMsgSendResponse response = server.sendMsg(request);
        Assert.assertEquals(SUCCESS, response.getCode());
        return response.getDelayMsg();
    }

    private CamelliaDelayMsgPullResponse pull(int batch, long ackTimeoutMillis) {
        CamelliaDelayMsgPullRequest request = new CamelliaDelayMsgPullRequest();
        request.setTopic(topic);
        request.setBatch(batch);
        request.setAckTimeoutMillis(ackTimeoutMillis);
        return server.pullMsg(request);
    }

    private CamelliaDelayMsgAckResponse ack(String msgId, boolean ack) {
        CamelliaDelayMsgAckRequest request = new CamelliaDelayMsgAckRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        request.setAck(ack);
        return server.ackMsg(request);
    }

    private CamelliaDelayMsgGetResponse get(String msgId) {
        CamelliaDelayMsgGetRequest request = new CamelliaDelayMsgGetRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        return server.getMsg(request);
    }

    private static CamelliaDelayQueueServer newServer(CamelliaDelayQueueServerConfig config, InMemoryRedisTemplate redis) throws Exception {
        CamelliaDelayQueueServer server = (CamelliaDelayQueueServer) allocateInstance(CamelliaDelayQueueServer.class);
        setField(server, "serverConfig", config);
        setField(server, "template", redis);
        setField(server, "activeTimeMap", new CamelliaLocalCache());
        setField(server, "eventMap", new ConcurrentHashMap<String, Long>());
        setField(server, "callbackSet", new ConcurrentHashMap<>());
        return server;
    }

    private static Object allocateInstance(Class<?> clazz) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, clazz);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class InMemoryRedisTemplate extends CamelliaRedisTemplate {

        private Map<String, Value> strings;
        private Map<String, ArrayDeque<String>> lists;
        private Map<String, TreeMap<Double, LinkedHashSet<String>>> zsets;

        private InMemoryRedisTemplate() {
            super((String) null);
        }

        private static InMemoryRedisTemplate create() throws Exception {
            InMemoryRedisTemplate template = (InMemoryRedisTemplate) allocateInstance(InMemoryRedisTemplate.class);
            template.strings = new ConcurrentHashMap<>();
            template.lists = new ConcurrentHashMap<>();
            template.zsets = new ConcurrentHashMap<>();
            return template;
        }

        @Override
        public String set(String key, String value, String nxxx, String expx, long time) {
            cleanup(key);
            if ("NX".equalsIgnoreCase(nxxx) && strings.containsKey(key)) {
                return null;
            }
            strings.put(key, new Value(value, expireAt(time)));
            return "OK";
        }

        @Override
        public String psetex(String key, long milliseconds, String value) {
            strings.put(key, new Value(value, expireAt(milliseconds)));
            return "OK";
        }

        @Override
        public String get(String key) {
            cleanup(key);
            Value value = strings.get(key);
            return value == null ? null : value.value;
        }

        @Override
        public Long del(String key) {
            boolean removed = strings.remove(key) != null;
            removed = lists.remove(key) != null || removed;
            removed = zsets.remove(key) != null || removed;
            return removed ? 1L : 0L;
        }

        @Override
        public List<String> mget(String... keys) {
            List<String> values = new ArrayList<>(keys.length);
            for (String key : keys) {
                values.add(get(key));
            }
            return values;
        }

        @Override
        public Long lpush(String key, String... values) {
            ArrayDeque<String> list = list(key);
            for (String value : values) {
                list.addFirst(value);
            }
            return (long) list.size();
        }

        @Override
        public String rpop(String key) {
            return list(key).pollLast();
        }

        @Override
        public Long llen(String key) {
            return (long) list(key).size();
        }

        @Override
        public List<String> lrange(String key, long start, long end) {
            List<String> values = new ArrayList<>(list(key));
            if (values.isEmpty()) {
                return Collections.emptyList();
            }
            int from = (int) Math.max(0, start);
            int to = (int) Math.min(values.size() - 1, end);
            if (from > to) {
                return Collections.emptyList();
            }
            return new ArrayList<>(values.subList(from, to + 1));
        }

        @Override
        public Long lrem(String key, long count, String value) {
            ArrayDeque<String> list = list(key);
            long removed = 0;
            ArrayDeque<String> retained = new ArrayDeque<>();
            while (!list.isEmpty()) {
                String current = list.pollFirst();
                if (current.equals(value) && (count == 0 || removed < Math.abs(count))) {
                    removed++;
                } else {
                    retained.addLast(current);
                }
            }
            lists.put(key, retained);
            return removed;
        }

        @Override
        public Long zadd(String key, double score, String member) {
            TreeMap<Double, LinkedHashSet<String>> zset = zset(key);
            removeZMember(zset, member);
            zset.computeIfAbsent(score, k -> new LinkedHashSet<>()).add(member);
            return 1L;
        }

        @Override
        public Long zrem(String key, String... members) {
            TreeMap<Double, LinkedHashSet<String>> zset = zset(key);
            long removed = 0;
            for (String member : members) {
                if (removeZMember(zset, member)) {
                    removed++;
                }
            }
            return removed;
        }

        @Override
        public Set<String> zrange(String key, long start, long end) {
            List<String> values = zValues(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (values.isEmpty()) {
                return Collections.emptySet();
            }
            int from = (int) Math.max(0, start);
            int to = (int) Math.min(values.size() - 1, end);
            if (from > to) {
                return Collections.emptySet();
            }
            return new LinkedHashSet<>(values.subList(from, to + 1));
        }

        @Override
        public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
            List<String> values = zValues(key, min, max);
            if (offset >= values.size()) {
                return Collections.emptySet();
            }
            int to = Math.min(values.size(), offset + count);
            return new LinkedHashSet<>(values.subList(offset, to));
        }

        @Override
        public Long zcard(String key) {
            return (long) zValues(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).size();
        }

        @Override
        public Double zscore(String key, String member) {
            for (Map.Entry<Double, LinkedHashSet<String>> entry : zset(key).entrySet()) {
                if (entry.getValue().contains(member)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        public Object eval(String script, List<String> keys, List<String> args) {
            if (keys.size() == 2 && args.size() == 1 && script.contains("rpop")) {
                String msgId = rpop(keys.get(0));
                if (msgId != null) {
                    zadd(keys.get(1), Double.parseDouble(args.get(0)), msgId);
                    return msgId.getBytes();
                }
                return null;
            }
            if (keys.size() == 2 && script.contains("zrem") && script.contains("lpush")) {
                int count = Integer.parseInt(args.get(0));
                for (int i = 1; i <= count; i++) {
                    String msgId = args.get(i);
                    zrem(keys.get(0), msgId);
                    lpush(keys.get(1), msgId);
                }
                return null;
            }
            throw new UnsupportedOperationException("Unsupported script: " + script);
        }

        void triggerDueWaitingMessages(String topic) {
            moveDue(queueKey(zsets.keySet(), "camellia_delay_queue_waiting", topic), queueKey(lists.keySet(), "camellia_delay_queue_ready", topic));
        }

        void retryTimedOutAckMessages(String topic) {
            moveDue(queueKey(zsets.keySet(), "camellia_delay_queue_ack", topic), queueKey(lists.keySet(), "camellia_delay_queue_ready", topic));
        }

        int readySize(String topic) {
            return list(queueKey(lists.keySet(), "camellia_delay_queue_ready", topic)).size();
        }

        private void moveDue(String zsetKey, String readyKey) {
            Set<String> due = zrangeByScore(zsetKey, 0, System.currentTimeMillis() + 100, 0, 1000);
            for (String msgId : due) {
                zrem(zsetKey, msgId);
                lpush(readyKey, msgId);
            }
        }

        private String queueKey(Set<String> keys, String prefix, String topic) {
            for (String key : keys) {
                if (key.startsWith(prefix + "|{|") && key.endsWith("|" + topic + "|}")) {
                    return key;
                }
            }
            throw new IllegalStateException("queue key not found, prefix=" + prefix + ", topic=" + topic);
        }

        private ArrayDeque<String> list(String key) {
            return lists.computeIfAbsent(key, k -> new ArrayDeque<>());
        }

        private TreeMap<Double, LinkedHashSet<String>> zset(String key) {
            return zsets.computeIfAbsent(key, k -> new TreeMap<>());
        }

        private List<String> zValues(String key, double min, double max) {
            List<String> values = new ArrayList<>();
            for (Map.Entry<Double, LinkedHashSet<String>> entry : zset(key).subMap(min, true, max, true).entrySet()) {
                values.addAll(entry.getValue());
            }
            return values;
        }

        private boolean removeZMember(TreeMap<Double, LinkedHashSet<String>> zset, String member) {
            boolean removed = false;
            List<Double> emptyScores = new ArrayList<>();
            for (Map.Entry<Double, LinkedHashSet<String>> entry : zset.entrySet()) {
                if (entry.getValue().remove(member)) {
                    removed = true;
                }
                if (entry.getValue().isEmpty()) {
                    emptyScores.add(entry.getKey());
                }
            }
            for (Double score : emptyScores) {
                zset.remove(score);
            }
            return removed;
        }

        private long expireAt(long milliseconds) {
            return milliseconds <= 0 ? System.currentTimeMillis() - 1 : System.currentTimeMillis() + milliseconds;
        }

        private void cleanup(String key) {
            Value value = strings.get(key);
            if (value != null && value.expireAt < System.currentTimeMillis()) {
                strings.remove(key);
            }
        }

        private static class Value {
            private final String value;
            private final long expireAt;

            private Value(String value, long expireAt) {
                this.value = value;
                this.expireAt = expireAt;
            }
        }
    }
}
