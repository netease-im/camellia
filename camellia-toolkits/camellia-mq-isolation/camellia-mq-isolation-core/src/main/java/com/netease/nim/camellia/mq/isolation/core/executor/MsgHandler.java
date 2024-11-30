package com.netease.nim.camellia.mq.isolation.core.executor;

import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerContext;

/**
 * Created by caojiajun on 2024/2/4
 */
public interface MsgHandler {

    /**
     * 实际的业务消息处理接口，由业务层自己实现
     * @param context 消息上下文
     * @return result
     */
    MsgHandlerResult onMsg(ConsumerContext context);

}
