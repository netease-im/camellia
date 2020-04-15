package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 *
 * Created by caojiajun on 2019/5/29.
 */
@Repository
public interface TableDao extends JpaRepository<Table, Long> {
}
