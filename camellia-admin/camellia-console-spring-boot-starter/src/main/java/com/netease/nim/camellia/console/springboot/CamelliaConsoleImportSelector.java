package com.netease.nim.camellia.console.springboot;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaConsoleImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[] { CamelliaConsoleConfigurationStarter.class.getName() };

    }
}
