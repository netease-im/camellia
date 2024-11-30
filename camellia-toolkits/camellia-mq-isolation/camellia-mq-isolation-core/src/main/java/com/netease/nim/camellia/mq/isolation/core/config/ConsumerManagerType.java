package com.netease.nim.camellia.mq.isolation.core.config;

/**
 * Created by caojiajun on 2024/2/7
 */
public enum ConsumerManagerType {

    all,//开启所有TopicType类型的所有MqInfo的消费者线程
    all_exclude_topic_type,//排除掉某些TopicType之后，开启剩余的TopicType类型的所有MqInfo的消费者线程
    all_exclude_mq_info,//排除掉某些MqInfo之后，开启剩余的所有MqInfo的消费者线程
    specify_topic_type,//仅开启指定的TopicType的所有MqInfo的消费者线程
    specify_mq_info,//仅开启指定的MqInfo的消费者线程
    specify_topic_type_exclude_mq_info,//指定TopicType，但是排除一部分MqInfo
    ;
}
