package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
            "LIMIT ?1,?2",nativeQuery = true)
    List<ResourceInfo> getPageList(int currentNum, Integer pageSize);

    @Query(value = "select count(*) from camellia_resource_info ",nativeQuery = true)
    Integer countAll();
}
