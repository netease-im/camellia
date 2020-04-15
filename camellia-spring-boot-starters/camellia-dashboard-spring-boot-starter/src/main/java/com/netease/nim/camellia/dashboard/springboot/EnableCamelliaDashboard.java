package com.netease.nim.camellia.dashboard.springboot;

import com.netease.nim.camellia.dashboard.CamelliaDashboardScanBase;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.*;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CamelliaDashboardImportSelector.class)
@EnableJpaRepositories(basePackageClasses = CamelliaDashboardScanBase.class)
@EntityScan(basePackageClasses = CamelliaDashboardScanBase.class)
public @interface EnableCamelliaDashboard {
}
