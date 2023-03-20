package com.netease.nim.camellia.config.dao;

import com.netease.nim.camellia.config.model.ConfigHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/15
 */
public interface ConfigHistoryDao {

    @Insert("insert into camellia_config_history (`ctype`, `namespace`, `config_id`, `old_config`, `new_config`, `operator_type`, `operator`,`create_time`)" +
            " values (#{type}, #{namespace}, #{configId}, #{oldConfig}, #{newConfig}, #{operatorType}, #{operator}, #{createTime});")
    int create(ConfigHistory configHistory);

    @Select("select `id`, `ctype` as `type`, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `type` = #{type} limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByType(@Param("type") int type, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `ctype` as type, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `type` = #{type} and (`namespace` like concat('%', #{keyword}, '%') or `old_config` like concat('%', #{keyword}, '%') or `newConfig` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByTypeAndKeyword(@Param("type") int type, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `ctype` as type, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `type` = #{type} and `config_id` = #{configId} limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByTypeAndConfigId(@Param("type") int type, long configId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `ctype` as type, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `type` = #{type} and `config_id` = #{configId} and (`namespace` like concat('%', #{keyword}, '%') or `old_config` like concat('%', #{keyword}, '%') or `newConfig` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByTypeAndConfigIdAndKeyword(@Param("type") int type, long configId, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    @Select("select `id`, `ctype` as type, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `namespace` = #{namespace} limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByNamespace(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select `id`, `ctype` as type, `namespace`, `config_id` as `configId`, `old_config` as `oldConfig`, `new_config` as `newConfig` , `operator_type` as `operatorType`, `operator`,`create_time` as `createTime` from camellia_config_history " +
            " where `namespace` = #{namespace} and (`namespace` like concat('%', #{keyword}, '%') or `old_config` like concat('%', #{keyword}, '%') or `newConfig` like concat('%', #{keyword}, '%')) limit #{offset}, #{limit}")
    List<ConfigHistory> getConfigHistoryListByNamespaceAndKeyword(@Param("namespace") String namespace, @Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

}
