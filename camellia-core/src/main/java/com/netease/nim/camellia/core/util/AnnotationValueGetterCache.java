package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/28
 */
public class AnnotationValueGetterCache {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationValueGetterCache.class);
    private final ConcurrentHashMap<Class<? extends Annotation>, ConcurrentHashMap<Method, AnnotationCacheInfo>> map = new ConcurrentHashMap<>();

    /**
     * 预热反射缓存
     * @param clazz 对象
     * @param annotation 注解
     */
    public void preheatAnnotationValueByParameterField(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Method method : clazz.getMethods()) {
            getAnnotationValueByParameterField(annotation, method, null);
        }
    }

    /**
     * 查询某个Method上的参数是否包含某个注解，如果有则把被注解的参数值返回
     * 会校验method的所有parameter以及parameter的所有field
     * 只会返回第一个满足条件的参数
     * @param annotation 注解类型
     * @param method 方法
     * @param objects 方法入参
     * @return 包含注解的字段的value
     */
    public Object getAnnotationValueByParameterField(Class<? extends Annotation> annotation, Method method, Object[] objects) {
        try {
            ConcurrentHashMap<Method, AnnotationCacheInfo> subMap = CamelliaMapUtils.computeIfAbsent(map, annotation, k -> new ConcurrentHashMap<>());
            AnnotationCacheInfo cacheInfo = subMap.get(method);
            if (cacheInfo != null) {
                if (cacheInfo.type == AnnotationCacheInfo.Type.NULL) {
                    return null;
                } else if (cacheInfo.type == AnnotationCacheInfo.Type.PARAMETERS) {
                    return objects == null ? null : objects[cacheInfo.parameterIndex];
                } else if (cacheInfo.type == AnnotationCacheInfo.Type.FIELDS) {
                    List<Field> fields = cacheInfo.fields;
                    if (objects == null) return null;
                    Object object = objects[cacheInfo.parameterIndex];
                    for (Field field : fields) {
                        if (object == null) {
                            return null;
                        }
                        object = field.get(object);
                    }
                    return object;
                }
            }
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i=0; i<parameterAnnotations.length; i++) {
                //先检查方法入参
                Annotation[] parameterAnnotation = parameterAnnotations[i];
                boolean hasAnnotation = false;
                for (Annotation paramAnnotation : parameterAnnotation) {
                    if (annotation.isAssignableFrom(paramAnnotation.annotationType())) {
                        hasAnnotation = true;
                        break;
                    }
                }
                if (hasAnnotation) {
                    subMap.put(method, new AnnotationCacheInfo(AnnotationCacheInfo.Type.PARAMETERS, i, null));
                    return objects == null ? null : objects[i];
                }
                //如果方法入参是一个业务对象，则检查其内部成员变量
                Class<?> parameterClazz = parameterTypes[i];
                List<Field> fields = new ArrayList<>();
                AnnotationFindInfo annotationFindInfo = getAnnotationValueByField(parameterClazz, annotation, objects == null ? null : objects[i], fields);
                if (annotationFindInfo.success) {
                    Collections.reverse(fields);
                    subMap.put(method, new AnnotationCacheInfo(AnnotationCacheInfo.Type.FIELDS, i, fields));
                    return annotationFindInfo.result;
                }
            }
            subMap.put(method, new AnnotationCacheInfo(AnnotationCacheInfo.Type.NULL, -1, null));
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private AnnotationFindInfo getAnnotationValueByField(Class<?> clazz, Class<? extends Annotation> annotation, Object object, List<Field> fields) throws IllegalAccessException {
        if (isBaseType(clazz) || isCollectionType(clazz)) {
            //如果是基本类型，则返回查不到
            return new AnnotationFindInfo(false, null);
        }
        Class<?> targetClass = clazz;
        //先检查成员变量是否被注解修饰
        for (Field field : targetClass.getDeclaredFields()) {
            Annotation declaredAnnotation = field.getDeclaredAnnotation(annotation);
            if (declaredAnnotation != null) {
                field.setAccessible(true);
                fields.add(field);
                return new AnnotationFindInfo(true, object == null ? null : field.get(object));
            }
        }
        //再检查父类的成员变量是否被注解修饰
        while (true) {
            Class<?> superclass = targetClass.getSuperclass();
            if (superclass == null) {
                break;
            }
            for (Field field : superclass.getDeclaredFields()) {
                Annotation declaredAnnotation = field.getDeclaredAnnotation(annotation);
                if (declaredAnnotation != null) {
                    field.setAccessible(true);
                    fields.add(field);
                    return new AnnotationFindInfo(true, object == null ? null : field.get(object));
                }
            }
            targetClass = superclass;
        }
        targetClass = clazz;
        //如果成员变量是用户定义的对象，则递归的查找
        //先递归查找普通成员对象
        for (Field field : targetClass.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (isBaseType(fieldType) || isCollectionType(fieldType)) continue;
            field.setAccessible(true);
            AnnotationFindInfo findInfo = getAnnotationValueByField(fieldType, annotation, object == null ? null : field.get(object), fields);
            if (findInfo.success) {
                fields.add(field);
                return findInfo;
            }
        }
        //再递归查找父类的成员对象
        while (true) {
            Class<?> superclass = targetClass.getSuperclass();
            if (superclass == null) {
                break;
            }
            for (Field field : superclass.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (isBaseType(fieldType) || isCollectionType(fieldType)) continue;
                field.setAccessible(true);
                AnnotationFindInfo findInfo = getAnnotationValueByField(fieldType, annotation, object == null ? null : field.get(object), fields);
                if (findInfo.success) {
                    fields.add(field);
                    return findInfo;
                }
            }
            targetClass = superclass;
        }
        return new AnnotationFindInfo(false, null);
    }

    private static class AnnotationFindInfo {
        boolean success;
        Object result;

        public AnnotationFindInfo(boolean success, Object result) {
            this.success = success;
            this.result = result;
        }
    }

    private static class AnnotationCacheInfo {
        Type type;
        int parameterIndex;
        List<Field> fields;

        public AnnotationCacheInfo(Type type, int parameterIndex, List<Field> fields) {
            this.type = type;
            this.parameterIndex = parameterIndex;
            this.fields = fields;
        }

        private static enum Type {
            NULL,
            PARAMETERS,
            FIELDS,
            ;
        }
    }

    private boolean isBaseType(Class<?> clazz) {
        if (String.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Enum.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Byte.class.isAssignableFrom(clazz) || byte.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Character.class.isAssignableFrom(clazz) || char.class.isAssignableFrom(clazz)) {
            return true;
        }
        return Short.class.isAssignableFrom(clazz) || short.class.isAssignableFrom(clazz);
    }

    private boolean isCollectionType(Class<?> clazz) {
        if (List.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Set.class.isAssignableFrom(clazz)) {
            return true;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }
}
