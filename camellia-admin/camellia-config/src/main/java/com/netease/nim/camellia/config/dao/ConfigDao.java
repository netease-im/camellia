package com.netease.nim.camellia.config.dao;


import com.netease.nim.camellia.config.model.Config;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/7
 */
public interface ConfigDao {

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config where id = #{id}")
    Config getById(@Param("id") long id);

    @Delete("delete from camellia_config where id = #{id}")
    int deleteById(@Param("id") long id);

    @Update("update camellia_config set `cvalue` = #{cvalue}, `valid_flag` = #{validFlag}, `info` = #{info}, `ctype` = #{type}," +
            " `operator` = #{operator}, `version` = #{version}, `update_time` = #{updateTime} where id = #{id}")
    int update(Config config);

    @Insert("insert into camellia_config (`namespace`, `ckey`, `cvalue`, `valid_flag`, `info`," +
            " `ctype`, `creator`, `operator`, `version`, `create_time`, `update_time`)" +
            " values (#{namespace}, #{key}, #{value}, #{validFlag}, #{info}, #{type}," +
            " #{creator}, #{operator}, #{version}, #{createTime}, #{updateTime});")
    int create(Config config);

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTim` from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 limit #{offset}, #{limit}")
    List<Config> getValidList(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 limit #{offset}, #{limit}")
    long getValidListCount(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 and " +
            " (`ckey` like concat('%', #{keyword}, '%') or `cvalue` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<Config> getValidListAndKeyword(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select count(*) from camellia_config " +
            " where `namespace` = #{namespace} and `valid_flag` = 1 and " +
            " (`ckey` like concat('%', #{keyword}, '%') or `cvalue` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    long getValidListAndKeywordCount(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as createTime, `update_time` as updateTime from camellia_config " +
            " where `namespace` = #{namespace} limit #{offset}, #{limit}")
    List<Config> getList(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from camellia_config " +
            " where `namespace` = #{namespace} limit #{offset}, #{limit}")
    long getListCount(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `ckey` as key, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config " +
            " where `namespace` = #{namespace} and " +
            " (`ckey` like concat('%', #{keyword}, '%') or `cvalue` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<Config> getListAndKeyword(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select count(*) from camellia_config " +
            " where `namespace` = #{namespace} and " +
            " (`ckey` like concat('%', #{keyword}, '%') or `cvalue` like concat('%', #{keyword}, '%') or `info` like concat('%', #{keyword}, '%') or `operator` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    long getListAndKeywordCount(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config where `namespace` = #{namespace} limit 100000")
    List<Config> findAllValidByNamespace(@Param("namespace") String namespace);

    @Select("select `id`, `namespace`, `ckey` as `key`, `cvalue` as `value`, `valid_flag` as `validFlag`, `info`, `ctype` as `type`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config where `namespace` = #{namespace} and `ckey` = #{key}")
    Config findByNamespaceAndKey(@Param("namespace") String namespace, @Param("key") String key);
}
