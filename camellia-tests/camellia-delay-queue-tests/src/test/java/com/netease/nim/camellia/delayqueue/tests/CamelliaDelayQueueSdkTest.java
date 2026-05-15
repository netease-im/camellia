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
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayQueueException;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import com.netease.nim.camellia.delayqueue.sdk.api.CamelliaDelayQueueApi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CamelliaDelayQueueSdkTest {

    private static final int SUCCESS = CamelliaDelayMsgErrorCode.SUCCESS.getValue();

    private FakeApi api;
    private CamelliaDelayQueueSdk sdk;

    @Before
    public void setUp() throws Exception {
        api = new FakeApi();
        CamelliaDelayQueueSdkConfig config = new CamelliaDelayQueueSdkConfig();
        config.setUrl("http://127.0.0.1:1");
        CamelliaDelayMsgListenerConfig listenerConfig = new CamelliaDelayMsgListenerConfig();
        listenerConfig.setLongPollingEnable(false);
        listenerConfig.setPullIntervalTimeMillis(5);
        listenerConfig.setAckTimeoutMillis(20);
        listenerConfig.setPullThreads(1);
        listenerConfig.setConsumeThreads(1);
        config.setListenerConfig(listenerConfig);
        sdk = new CamelliaDelayQueueSdk(config);
        setApi(sdk, api);
    }

    @Test
    public void shouldWrapSendGetAndDeleteResponses() {
        CamelliaDelayMsg sent = sdk.sendMsg("topic", "msg-id", "payload", 10, 100, 2);
        Assert.assertEquals("topic", sent.getTopic());
        Assert.assertEquals("msg-id", sent.getMsgId());
        Assert.assertEquals("payload", sent.getMsg());
        Assert.assertEquals(10, api.lastSendRequest.getDelayMillis());

        Assert.assertEquals(sent, sdk.getMsg("topic", "msg-id"));
        Assert.assertTrue(sdk.deleteMsg("topic", "msg-id"));
        Assert.assertFalse(sdk.deleteMsg("topic", "missing"));
    }

    @Test
    public void shouldThrowWhenSendResponseIsNotSuccess() {
        api.failNextSend = true;
        try {
            sdk.sendMsg("topic", "payload", 0);
            Assert.fail("expected exception");
        } catch (CamelliaDelayQueueException e) {
            Assert.assertEquals(CamelliaDelayMsgErrorCode.PARAM_WRONG, e.getErrorCode());
        }
    }

    @Test
    public void shouldAckTrueWhenListenerConsumesSuccessfully() throws Exception {
        CamelliaDelayMsg msg = msg("topic", "success-msg", "payload");
        api.enqueue(msg);
        CountDownLatch consumed = new CountDownLatch(1);

        long listenerId = sdk.addMsgListener("topic", delayMsg -> {
            consumed.countDown();
            return true;
        });

        Assert.assertTrue(consumed.await(2, TimeUnit.SECONDS));
        assertEventually(() -> api.ackRequests.size() == 1, 2000);
        Assert.assertTrue(api.ackRequests.get(0).isAck());
        Assert.assertEquals("success-msg", api.ackRequests.get(0).getMsgId());
        Assert.assertTrue(sdk.removeMsgListener(listenerId));
    }

    @Test
    public void shouldAckFalseWhenListenerReturnsFalseOrThrows() throws Exception {
        api.enqueue(msg("topic", "false-msg", "payload"));
        api.enqueue(msg("topic", "throw-msg", "payload"));
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch consumed = new CountDownLatch(2);

        long listenerId = sdk.addMsgListener("topic", delayMsg -> {
            consumed.countDown();
            int call = calls.incrementAndGet();
            if (call == 1) {
                return false;
            }
            throw new IllegalStateException("fail");
        });

        Assert.assertTrue(consumed.await(2, TimeUnit.SECONDS));
        assertEventually(() -> api.ackRequests.size() == 2, 2000);
        Assert.assertFalse(api.ackRequests.get(0).isAck());
        Assert.assertFalse(api.ackRequests.get(1).isAck());
        Assert.assertTrue(sdk.removeMsgListener(listenerId));
    }

    @Test
    public void shouldStopConsumingAfterListenerRemoved() throws Exception {
        CountDownLatch consumed = new CountDownLatch(1);
        AtomicReference<String> consumedId = new AtomicReference<>();
        api.enqueue(msg("topic", "before-remove", "payload"));

        long listenerId = sdk.addMsgListener("topic", delayMsg -> {
            consumedId.set(delayMsg.getMsgId());
            consumed.countDown();
            return true;
        });
        Assert.assertTrue(consumed.await(2, TimeUnit.SECONDS));
        Assert.assertEquals("before-remove", consumedId.get());

        Assert.assertTrue(sdk.removeMsgListener(listenerId));
        int ackCount = api.ackRequests.size();
        api.enqueue(msg("topic", "after-remove", "payload"));
        Thread.sleep(100);
        Assert.assertEquals(ackCount, api.ackRequests.size());
    }

    private static CamelliaDelayMsg msg(String topic, String msgId, String payload) {
        CamelliaDelayMsg msg = new CamelliaDelayMsg();
        msg.setTopic(topic);
        msg.setMsgId(msgId);
        msg.setMsg(payload);
        msg.setProduceTime(System.currentTimeMillis());
        msg.setTriggerTime(System.currentTimeMillis());
        msg.setExpireTime(System.currentTimeMillis() + 1000);
        msg.setStatus(CamelliaDelayMsgStatus.READY.getValue());
        msg.setMaxRetry(1);
        return msg;
    }

    private static void setApi(CamelliaDelayQueueSdk sdk, CamelliaDelayQueueApi api) throws Exception {
        Field field = CamelliaDelayQueueSdk.class.getDeclaredField("api");
        field.setAccessible(true);
        field.set(sdk, api);
    }

    private static void assertEventually(BooleanSupplier supplier, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (supplier.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        Assert.assertTrue(supplier.getAsBoolean());
    }

    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private static class FakeApi extends CamelliaDelayQueueApi {

        private final List<CamelliaDelayMsg> queue = Collections.synchronizedList(new ArrayList<>());
        private final List<CamelliaDelayMsgAckRequest> ackRequests = Collections.synchronizedList(new ArrayList<>());
        private CamelliaDelayMsgSendRequest lastSendRequest;
        private CamelliaDelayMsg lastSentMsg;
        private volatile boolean failNextSend;

        FakeApi() {
            super(config());
        }

        @Override
        public CamelliaDelayMsgSendResponse sendMsg(CamelliaDelayMsgSendRequest request) {
            lastSendRequest = request;
            CamelliaDelayMsgSendResponse response = new CamelliaDelayMsgSendResponse();
            if (failNextSend) {
                failNextSend = false;
                response.setCode(CamelliaDelayMsgErrorCode.PARAM_WRONG.getValue());
                response.setMsg("param wrong");
                return response;
            }
            CamelliaDelayMsg msg = msg(request.getTopic(), request.getMsgId() == null ? "generated" : request.getMsgId(), request.getMsg());
            msg.setTriggerTime(System.currentTimeMillis() + request.getDelayMillis());
            msg.setExpireTime(msg.getTriggerTime() + request.getTtlMillis());
            msg.setMaxRetry(request.getMaxRetry());
            lastSentMsg = msg;
            response.setCode(SUCCESS);
            response.setMsg("success");
            response.setDelayMsg(msg);
            return response;
        }

        @Override
        public CamelliaDelayMsgDeleteResponse deleteMsg(CamelliaDelayMsgDeleteRequest request) {
            CamelliaDelayMsgDeleteResponse response = new CamelliaDelayMsgDeleteResponse();
            response.setCode("missing".equals(request.getMsgId()) ? CamelliaDelayMsgErrorCode.NOT_EXISTS.getValue() : SUCCESS);
            response.setMsg("success");
            return response;
        }

        @Override
        public CamelliaDelayMsgGetResponse getMsg(CamelliaDelayMsgGetRequest request) {
            CamelliaDelayMsgGetResponse response = new CamelliaDelayMsgGetResponse();
            response.setCode(lastSentMsg == null ? CamelliaDelayMsgErrorCode.NOT_EXISTS.getValue() : SUCCESS);
            response.setMsg("success");
            response.setDelayMsg(lastSentMsg);
            return response;
        }

        @Override
        public CamelliaDelayMsgPullResponse pullMsg(CamelliaDelayMsgPullRequest request) {
            CamelliaDelayMsgPullResponse response = new CamelliaDelayMsgPullResponse();
            response.setCode(SUCCESS);
            List<CamelliaDelayMsg> messages = new ArrayList<>();
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    messages.add(queue.remove(0));
                }
            }
            response.setDelayMsgList(messages);
            return response;
        }

        @Override
        public CamelliaDelayMsgAckResponse ackMsg(CamelliaDelayMsgAckRequest request) {
            ackRequests.add(request);
            CamelliaDelayMsgAckResponse response = new CamelliaDelayMsgAckResponse();
            response.setCode(SUCCESS);
            response.setMsg("success");
            return response;
        }

        void enqueue(CamelliaDelayMsg msg) {
            queue.add(msg);
        }

        private static CamelliaDelayQueueSdkConfig config() {
            CamelliaDelayQueueSdkConfig config = new CamelliaDelayQueueSdkConfig();
            config.setUrl("http://127.0.0.1:1");
            return config;
        }
    }
}
