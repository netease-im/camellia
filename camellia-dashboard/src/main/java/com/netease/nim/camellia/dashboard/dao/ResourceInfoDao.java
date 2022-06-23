package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/11/21.
 */
@Repository
public interface ResourceInfoDao extends JpaRepository<ResourceInfo, Long> {

    ResourceInfo findByUrl(String url);


    @Query(value = "select * from camellia_resource_info " +
            "where " +
            "if(:url is not null,url like CONCAT('%',:url,'%'),1=1) " +
            "order by id " +
            "LIMIT :currentNum,:pageSize",nativeQuery = true)
    List<ResourceInfo> getPageList(@Param("url") String url,
                                   @Param("currentNum") Integer currentNum,
                                   @Param("pageSize") Integer pageSize);

    @Query(value = "select count(*) from camellia_resource_info " +
            "where " +
            "if(:url is not null,url like CONCAT('%',:url,'%'),1=1) ",nativeQuery = true)
    Integer countAll(@Param("url") String url);



    @Query(value = "select * from camellia_resource_info " +
            "where " +
            "if(:url is not null,url like CONCAT('%',:url,'%') or info like CONCAT('%',:url,'%') ,1=1) " +
            "order by id " +
            "LIMIT 0,:size",nativeQuery = true)
    List<ResourceInfo> queryListByUrl(@Param("url") String url,
                                      @Param("size") Integer size);
}
