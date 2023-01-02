package com.netease.nim.camellia.dashboard.springboot;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaDashboardImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] { CamelliaDashboardConfigurationStarter.class.getName() };
    }
}
