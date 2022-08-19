package com.netease.nim.camellia.console.annotation;

import com.netease.nim.camellia.console.constant.ActionRole;
import com.netease.nim.camellia.console.constant.ActionType;
import io.netty.util.internal.StringUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface ActionSecurity {

    ActionType action() default  ActionType.READ;

    String resource() default StringUtil.EMPTY_STRING;

    ActionRole role() default ActionRole.NORMAL;


}
