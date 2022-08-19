package com.netease.nim.camellia.console.dao.mapper;

import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Mapper
public interface CamelliaRefHistoryMapper {

    @Insert("INSERT into camellia_ref_history (`address`,`dashboardId`,`username`,`bid`,`bgroup`,`refInfo`,`tid`,`resourceInfo`,`detail`,`opType`,`createdTime`) " +
            "VALUES(#{address},#{dashboardId},#{username},#{bid},#{bgroup},#{refInfo},#{tid},#{resourceInfo},#{detail},#{opType},#{createdTime})")
    @Options(useGeneratedKeys=true, keyProperty="id", keyColumn="id")
    int saveCamelliaRefHistory(CamelliaRefHistory refHistory);


    @Insert({"<script>",
            "INSERT into camellia_ref_history (`address`,`dashboardId`,`username`,`bid`,`bgroup`,`refInfo`,`tid`,`resourceInfo`,`detail`,`opType`,`createdTime`) ",
            "VALUES",
            "<foreach collection='list' item='item' separator=',' >",
            "(#{item.address},#{item.dashboardId},#{item.username},#{item.bid},#{item.bgroup},#{item.refInfo},#{item.tid},#{item.resourceInfo},#{item.detail},#{item.opType},#{item.createdTime})",
            "</foreach>",
            "</script>"})
    @Options(useGeneratedKeys=true, keyProperty="id", keyColumn="id")
    void saveCamelliaRefHistories(@Param("list") List<CamelliaRefHistory> refHistories);

    @Select("SELECT id,address,dashboardId,username,bid,bgroup,refInfo,tid,resourceInfo,opType,createdTime " +
            "FROM camellia_ref_history " +
            "WHERE dashboardId=#{dashboardId} " +
            "ORDER BY createdTime DESC " +
            "LIMIT #{currentNum},#{pageSize}")
    List<CamelliaRefHistory> findAllByDashboardId(@Param("dashboardId") Long dashboardId,
                                                  @Param("currentNum") Integer currentNum,
                                                  @Param("pageSize") Integer pageSize);

    @Select("SELECT * " +
            "FROM camellia_ref_history " +
            "WHERE id=#{id} and dashboardId=#{dashboardId}")
    CamelliaRefHistory findById(@Param("id") Long id,@Param("dashboardId") Long dashboardId);


    @Select({"<script>",
            "SELECT id,address,dashboardId,username,bid,bgroup,refInfo,tid,resourceInfo,opType,createdTime ",
            "FROM camellia_ref_history " ,
            "WHERE dashboardId=#{dashboardId}",
            "<if test ='bid !=null'>",
            "and bid=#{bid}",
            "</if>",
            "<if test ='bgroup !=null'>",
            "and bgroup=#{bgroup}",
            "</if>",
            "ORDER BY createdTime DESC ",
            "LIMIT #{currentNum},#{pageSize}",
            "</script>"})
    List<CamelliaRefHistory> findAllByBidAndBgroup(@Param("dashboardId")Long dashboardId,
                                                   @Param("bid")Long bid,
                                                   @Param("bgroup")String bgroup,
                                                   @Param("currentNum")Integer currentNum,
                                                   @Param("pageSize") Integer pageSize);



    @Select("SELECT COUNT(id) " +
            "FROM camellia_ref_history " +
            "WHERE dashboardId=#{dashboardId}")
    Integer countById(Long dashboardId);

    @Select({"<script>",
            "SELECT count(*) ",
            "FROM camellia_ref_history " ,
            "WHERE dashboardId=#{dashboardId}",
            "<if test ='bid !=null'>",
            "and bid=#{bid}",
            "</if>",
            "<if test ='bgroup !=null'>",
            "and bgroup=#{bgroup}",
            "</if>",
            "</script>"})
    Integer countByBidAndBgroup(@Param("dashboardId")Long dashboardId,
                                @Param("bid")Long bid,
                                @Param("bgroup")String bgroup);
}
