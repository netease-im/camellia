package com.netease.nim.camellia.console.dao.mapper;

import com.netease.nim.camellia.console.model.CamelliaDashboard;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Mapper
public interface DashboardMapper {

    @Select("SELECT * FROM camellia_dashboard where did=#{did}")
    CamelliaDashboard findByDId(@Param("did") Long did);

    @Select("SELECT * FROM camellia_dashboard where tag=#{tag} and isUse=1")
    CamelliaDashboard findByTag(@Param("tag")String tag);

    @Select("SELECT * FROM camellia_dashboard where address=#{address}")
    CamelliaDashboard findByAddress(@Param("address")String address);

    @Insert("INSERT into camellia_dashboard (`address`,`tag`,`isOnline`,`isUse`,`updatedTime`,`createdTime`)" +
            "values(#{address},#{tag},#{isOnline},1,#{updatedTime},#{createdTime})")
    @Options(useGeneratedKeys=true, keyProperty="did", keyColumn="did")
    int saveCamelliaDashboard(CamelliaDashboard camelliaDashboard);

    @Update("UPDATE camellia_dashboard SET tag=#{tag} ,updatedTime=#{updatedTime} where did=#{did}")
    int updateTagByDId(CamelliaDashboard camelliaDashboard);

    @Update("UPDATE camellia_dashboard SET isOnline=#{isOnline},updatedTime=#{updatedTime} where did=#{did}")
    int updateOnlineStatusByDId(CamelliaDashboard camelliaDashboard);

    @Update("UPDATE camellia_dashboard SET isOnline=#{isOnline},isUse=#{isUse},updatedTime=#{updatedTime} where did=#{did}")
    int updateIsUseByDId(CamelliaDashboard camelliaDashboard);



    @Delete("DELETE FROM camellia_dashboard where did=#{did}")
    Integer deleteByDId(Long did);


    @Select({"<script>",
            "SELECT * FROM camellia_dashboard where 1=1 ",
            "<if test='isUse !=null'>",
            "and isUse=#{isUse}",
            "</if>",
            "<if test='isOnline !=null'>",
            "and isOnline=#{isOnline}",
            "</if>",
            "</script>"})
    List<CamelliaDashboard> findByUseAndOnline(@Param("isUse") Integer isUse,@Param("isOnline") Integer isOnline);



    @Select({"<script>",
            "SELECT * FROM camellia_dashboard where did in",
            "<foreach collection='list' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "<if test='isUse !=null'>",
            "and isUse=#{isUse}",
            "</if>",
            "<if test='isOnline != null'>",
            "and isOnline=#{isOnline}",
            "</if>",
            "</script>"})
    List<CamelliaDashboard> findInDidsIsUseIsOnline( @Param("list")List<Integer> list, @Param("isUse") Integer isUse, @Param("isOnline") Integer isOnline);


}
