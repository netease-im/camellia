package com.netease.nim.camellia.config.dao;

import com.netease.nim.camellia.config.model.ConfigNamespace;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/15
 */
public interface ConfigNamespaceDao {

    @Insert("insert into camellia_config_namespace (`namespace`, `alias`, `valid_flag`, `info`," +
            "`creator`, `operator`, `version`, `create_time`, `update_time`)" +
            " values (#{namespace}, #{alias}, #{validFlag}, #{info}," +
            " #{creator}, #{operator}, #{version}, #{createTime}, #{updateTime});")
    int create(ConfigNamespace configNamespace);

    @Update("update camellia_config_namespace set `alias` = #{alias}, `valid_flag` = #{validFlag}, `info` = #{info}," +
            " `operator` = #{operator}, `version` = #{version}, `update_time` = #{updateTime} where id = #{id}")
    int update(ConfigNamespace configNamespace);

    @Delete("delete from camellia_config_namespace where id = #{id}")
    int delete(@Param("id") long id);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace where id = #{id}")
    ConfigNamespace getById(@Param("id") long id);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`, " +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace " +
            " where `valid_flag` = 1 limit #{offset}, #{limit}")
    List<ConfigNamespace> getValidList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`, " +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace " +
            " where `valid_flag` = 1 and (namespace like concat('%', #{keyword}, '%') or info like concat('%', #{keyword}, '%') or creator like concat('%', #{keyword}, '%') or operator like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<ConfigNamespace> getValidListAndKeyword(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace " +
            " limit #{offset}, #{limit}")
    List<ConfigNamespace> getList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`," +
            "  `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace " +
            " where (namespace like concat('%', #{keyword}, '%') or info like concat('%', #{keyword}, '%') or creator like concat('%', #{keyword}, '%') or operator like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<ConfigNamespace> getListAndKeyword(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `namespace`, `alias`, `valid_flag` as `validFlag`, `info`," +
            " `creator`, `operator`, `version`, `create_time` as `createTime`, `update_time` as `updateTime` from camellia_config_namespace " +
            " where `namespace` = #{namespace}")
    ConfigNamespace getByNamespace(@Param("namespace") String namespace);
}
