package com.netease.nim.camellia.core.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被本注解修饰的参数会作为shardingKey的组成部分
 * 顺序为方法的入参顺序
 * 注意：对于非byte[]类型的参数，会调用toString方法，进而utf-8编码成byte[]去生成shardingKey
 * Created by caojiajun on 2019/5/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ShardingParam {
    public static enum Type {
        Simple,
        Collection,
        ;
    }

    Type type() default Type.Simple;
}
