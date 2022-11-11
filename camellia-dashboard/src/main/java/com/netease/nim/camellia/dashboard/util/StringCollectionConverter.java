package com.netease.nim.camellia.dashboard.util;


import org.springframework.util.CollectionUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.*;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
@Converter
public class StringCollectionConverter implements AttributeConverter<Collection<String>, String> {
    private static final String GROUP_DELIMITER = ", ";

    @Override
    public String convertToDatabaseColumn(Collection<String> stringList) {
        if (CollectionUtils.isEmpty(stringList)) {
            return null;
        }
        return stringList.toString();
    }

    @Override
    public Set<String> convertToEntityAttribute(String str) {
        if (str == null || str.isEmpty()) {
            return Collections.emptySet();
        }
        String substring = str.substring(1, str.length() - 1);
        return new HashSet<>(Arrays.asList(substring.split(GROUP_DELIMITER)));
    }
}