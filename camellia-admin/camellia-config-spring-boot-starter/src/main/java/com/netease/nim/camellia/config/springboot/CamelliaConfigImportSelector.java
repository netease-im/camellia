package com.netease.nim.camellia.config.springboot;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaConfigImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] { CamelliaConfigConfigurationStarter.class.getName() };
    }
}
