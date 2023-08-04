package com.netease.nim.camellia.redis.proxy.conf;

/**
 * policy of multi-write
 * Created by caojiajun on 2020/9/27
 */
public enum MultiWriteMode {

    /**
     * first redis reply, then reply to client
     */
    FIRST_RESOURCE_ONLY(1),

    /**
     * all multi-write redis reply, then reply first redis's reply
     */
    ALL_RESOURCES_NO_CHECK(2),

    /**
     * all multi-write redis reply, and none of replies is error, then reply first redis's reply, else reply the first error reply
     */
    ALL_RESOURCES_CHECK_ERROR(3),
    ;

    private final int value;

    MultiWriteMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MultiWriteMode getByValue(String value) {
        if (value == null) {
            return FIRST_RESOURCE_ONLY;
        }
        for (MultiWriteMode mode : MultiWriteMode.values()) {
            if (value.equalsIgnoreCase(mode.name()) || value.equalsIgnoreCase(String.valueOf(mode.getValue()))) {
                return mode;
            }
        }
        return FIRST_RESOURCE_ONLY;
    }
}
