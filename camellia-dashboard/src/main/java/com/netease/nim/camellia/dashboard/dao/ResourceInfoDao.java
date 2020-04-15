package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.ResourceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * Created by caojiajun on 2019/11/21.
 */
@Repository
public interface ResourceInfoDao extends JpaRepository<ResourceInfo, Long> {

    ResourceInfo findByUrl(String url);

}
