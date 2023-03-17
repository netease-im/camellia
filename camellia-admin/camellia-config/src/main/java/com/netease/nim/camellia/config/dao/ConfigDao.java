package com.netease.nim.camellia.config.dao;


import com.netease.nim.camellia.config.model.Config;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/7
 */
public interface ConfigDao {

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config where id = #{id}")
    Config getById(@Param("id") long id);

    @Delete("delete from camellia_config where id = #{id}")
    int deleteById(@Param("id") long id);

    @Update("update camellia_config set `value` = #{value}, `valid_flag` = #{validFlag}, `info` = #{info}, `type` = #{type}," +
            " `operator` = #{operator}, `version` = #{version}, `update_time` = #{updateTime} where id = #{id}")
    int update(Config config);

    @Insert("insert into camellia_config (`namespace`, `key`, `value`, `valid_flag`, `info`," +
            " `type`, `creator`, `operator`, `version`, `create_time`, `update_time`)" +
            " values (#{namespace}, #{key}, #{value}, #{validFlag}, #{info}, #{type}," +
            " #{creator}, #{operator}, #{version}, #{createTime}, #{updateTime});")
    int create(Config config);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 limit #{offset}, #{limit}")
    List<Config> getValidList(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 and " +
            " (`key` like concat('%', #{keyword}, '%') or `value` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<Config> getValidListAndKeyword(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config " +
            " where `namespace` = #{namespace} limit #{offset}, #{limit}")
    List<Config> getList(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config " +
            " where `namespace` = #{namespace} and " +
            " (`key` like concat('%', #{keyword}, '%') or `value` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<Config> getListAndKeyword(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config where `namespace` = #{namespace} limit 100000")
    List<Config> findAllValidByNamespace(@Param("namespace") String namespace);

    @Select("select `id`, `namespace`, `key`, `value`, `valid_flag` as validFlag, `info`, `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config where `namespace` = #{namespace} and `key` = #{key}")
    Config findByNamespaceAndKey(@Param("namespace") String namespace, @Param("key") String key);
}
