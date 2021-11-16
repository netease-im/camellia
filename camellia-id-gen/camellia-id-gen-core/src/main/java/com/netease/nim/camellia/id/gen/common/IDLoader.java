package com.netease.nim.camellia.id.gen.common;


import java.util.Map;

/**
 * 根据tag和步长返回一组id
 * Created by caojiajun on 2020/4/9.
 */
public interface IDLoader {

    /**
     * 获取一段id
     * @param tag tag
     * @param step 步长
     * @return 一段id
     */
    IDRange load(String tag, int step);

    /**
     * 更新id的起始值
     * @param tag tag
     * @param id 新的起始值
     * @return 成功/失败
     */
    default boolean update(String tag, long id) {
        return false;
    }

    /**
     * 查询所有tag到id起始值的map
     * @return 列表
     */
    default Map<String, Long> selectTagIdMaps() {
        return null;
    }

    /**
     * 根据tag查询当前的起始值
     * @param tag tag
     * @return id
     */
    default Long selectId(String tag) {
        return null;
    }
}
