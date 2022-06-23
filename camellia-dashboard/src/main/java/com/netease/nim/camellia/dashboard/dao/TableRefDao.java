package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.TableRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by caojiajun on 2019/5/29.
 */
@Repository
public interface TableRefDao extends JpaRepository<TableRef, Long> {

    TableRef findByBidAndBgroup(long bid, String bgroup);

    List<TableRef> findByTid(long tid);

    List<TableRef> findByBid(long bid);
}



