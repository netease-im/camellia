package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 *
 * Created by caojiajun on 2019/5/29.
 */
@Repository
public interface TableDao extends JpaRepository<Table, Long> {


    @Query(value = "select * from camellia_table " +
            "where 1=1 " +
            "AND if(:validFlag is not null , valid_flag=:validFlag,1=1) " +
            "AND if(:info is not null , info like CONCAT('%',:info,'%'),1=1) " +
            "AND if(:tid is not null , tid = :tid,1=1) "+
            "AND if(:detail is not null ,detail like CONCAT('%',:detail,'%'),1=1) "+
            "order by tid " +
            "LIMIT :currentNum,:pageSize",nativeQuery = true)
    List<Table> getPageValidFlagInfo(@Param("validFlag") Integer validFlag,
                                     @Param("info") String info,
                                     @Param("tid") Long tid,
                                     @Param("detail") String detail,
                                     @Param("currentNum") Integer currentNum,
                                     @Param("pageSize") Integer pageSize);

    @Query(value = "select count(*) from camellia_table " +
            "where 1=1 " +
            "AND if(:validFlag is not null , valid_flag=:validFlag,1=1) " +
            "AND if(:info is not null , info like CONCAT('%',:info,'%'),1=1) "+
            "AND if(:tid is not null , tid = :tid,1=1) "+
            "AND if(:detail is not null ,detail like CONCAT('%',:detail,'%'),1=1) ",nativeQuery = true)
    Integer countPageValidFlagInfo(@Param("validFlag") Integer validFlag,
                                   @Param("info") String info,
                                   @Param("tid") Long tid,
                                   @Param("detail") String detail);
}
