package com.netease.nim.camellia.console.dao.mapper;

import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Mapper
public interface CamelliaTableHistoryMapper {

    @Insert("INSERT into camellia_table_history (`address`,`dashboardId`,`username`,`tid`,`resourceInfo`,`detail`,`opType`,`createdTime`) " +
            "values(#{address},#{dashboardId},#{username},#{tid},#{resourceInfo},#{detail},#{opType},#{createdTime})")
    @Options(useGeneratedKeys=true, keyProperty="id", keyColumn="id")
    int saveCamelliaTableHistory(CamelliaTableHistory tableHistory);

    @Select("SELECT * " +
            "FROM camellia_table_history " +
            "WHERE id=#{id} and dashboardId=#{dashboardId}")
    CamelliaTableHistory findById(@Param("id") Long id,@Param("dashboardId") Long dashboardId);

    @Select({"<script>",
            "SELECT id,address,dashboardId,username,tid,resourceInfo,opType,createdTime ",
            "FROM camellia_table_history ",
            "WHERE dashboardId=#{dashboardId} ",
            "<if test ='tid !=null'>",
            "and tid=#{tid}",
            "</if>",
            "ORDER BY createdTime DESC ",
            "LIMIT #{currentNum},#{pageSize}",
            "</script>"})
    List<CamelliaTableHistory> findAllByTid(@Param("dashboardId")Long dashboardId,
                                            @Param("tid")Long tid,
                                            @Param("currentNum")Integer currentNum,
                                            @Param("pageSize") Integer pageSize);

    @Select({"<script>",
            "SELECT COUNT(id) ",
            "FROM camellia_table_history ",
            "WHERE dashboardId=#{dashboardId}",
            "<if test ='tid !=null'>",
            "and tid=#{tid}",
            "</if>",
            "</script>"})
    Integer countByTid(@Param("dashboardId")Long dashboardId,
                       @Param("tid")Long tid);
}
