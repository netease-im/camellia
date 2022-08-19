package com.netease.nim.camellia.console.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CamelliaConsoleImportSelector.class)
@MapperScan(basePackages = "com.netease.nim.camellia.console.dao.mapper")
public @interface EnableCamelliaConsole {
}
