package com.netease.nim.camellia.hot.key.server.springboot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CamelliaHotKeyServerImportSelector.class)
public @interface EnableCamelliaHotKeyServer {
}
