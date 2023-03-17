package com.netease.nim.camellia.config.springboot;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CamelliaConfigImportSelector.class)
@EntityScan(basePackageClasses = CamelliaConfigImportSelector.class)
public @interface EnableCamelliaConfig {
}
