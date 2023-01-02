package com.netease.nim.camellia.id.gen.springboot.idloader;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Created by caojiajun on 2021/9/27
 */
public interface CamelliaIdLoaderMapper {

    @Insert("insert into camellia_id_info (tag, id, createTime, updateTime) values (#{tag}, #{id}, #{createTime}, #{updateTime});")
    int insert(@Param("tag") String tag,
               @Param("id") long id,
               @Param("createTime") long createTime,
               @Param("updateTime") long updateTime);

    @Select("select id from camellia_id_info where tag=#{tag} for update;")
    Long selectForUpdate(@Param("tag") String tag);

    @Update("update camellia_id_info set id=#{id}, updateTime=#{updateTime} where tag=#{tag} and id < #{id}")
    int update(@Param("tag") String tag,
               @Param("id") long id,
               @Param("updateTime") long updateTime);

    @Select("select tag, id from camellia_id_info limit 10000")
    List<TagIdInfo> selectTagIdMaps();

    @Select("select id from camellia_id_info where tag=#{tag};")
    Long selectId(@Param("tag") String tag);
}
