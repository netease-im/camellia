package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.TableRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Query(value = "select * from camellia_table_ref  " +
            "where 1=1 " +
            "AND if(:tid is not null,tid=:tid,1=1) " +
            "AND if(:bid is not null,bid=:bid,1=1) " +
            "AND if(:bgroup is not null,bgroup=:bgroup,1=1) " +
            "AND if(:validFlag is not null,valid_flag=:validFlag,1=1) " +
            "AND if(:info is not null,info like CONCAT('%',:info,'%'),1=1) " +
            "order by id " +
            "LIMIT :currentNum,:pageSize", nativeQuery = true)
    List<TableRef> findByTidBidBGroupValid(@Param("tid") Long tid,
                                           @Param("bid") Long bid,
                                           @Param("bgroup") String bgroup,
                                           @Param("validFlag") Integer validFlag,
                                           @Param("info") String info,
                                           @Param("currentNum") Integer currentNum,
                                           @Param("pageSize") Integer pageSize);


    @Query(value = "select count(*) from camellia_table_ref " +
            "where 1=1 " +
            "AND if(:tid is not null,tid=:tid,1=1) " +
            "AND if(:bid is not null,bid=:bid,1=1) " +
            "AND if(:bgroup is not null,bgroup=:bgroup,1=1) " +
            "AND if(:info is not null,info like CONCAT('%',:info,'%'),1=1) "+
            "AND if(:validFlag is not null,valid_flag=:validFlag,1=1) ", nativeQuery = true)
    Integer countByTidBidBGroupValid(@Param("tid") Long tid,
                                     @Param("bid") Long bid,
                                     @Param("bgroup") String bgroup,
                                     @Param("validFlag") Integer validFlag,
                                     @Param("info") String info);
}
