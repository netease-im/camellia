package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.TableRefAddition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 * @date 2022/6/7 16:37
 */
@Repository
public interface TableRefAdditionDao extends JpaRepository<TableRefAddition, Long> {

    @Query(value = "select ctr.id,ctr.bid,ctr.bgroup,ctr.tid,ctr.info,ctr.valid_flag,ctr.create_time,ctr.update_time,ct.info as resource_info " +
            "from camellia_table_ref ctr left join camellia_table ct on ctr.tid=ct.tid " +
            "where 1=1 " +
            "AND if(:tid is not null,ctr.tid=:tid,1=1) " +
            "AND if(:bid is not null,ctr.bid=:bid,1=1) " +
            "AND if(:bgroup is not null,ctr.bgroup=:bgroup,1=1) " +
            "AND if(:validFlag is not null,ctr.valid_flag=:validFlag,1=1) " +
            "AND if(:info is not null,ctr.info like CONCAT('%',:info,'%'),1=1) " +
            "AND if(:resourceInfo is not null,ct.info like CONCAT('%',:resourceInfo,'%'),1=1) " +
            "order by ctr.id " +
            "LIMIT :currentNum,:pageSize", nativeQuery = true)
    List<TableRefAddition> findByTidBidBGroupValid(@Param("tid") Long tid,
                                                   @Param("bid") Long bid,
                                                   @Param("bgroup") String bgroup,
                                                   @Param("validFlag") Integer validFlag,
                                                   @Param("info") String info,
                                                   @Param("resourceInfo")String resourceInfo,
                                                   @Param("currentNum") Integer currentNum,
                                                   @Param("pageSize") Integer pageSize);


    @Query(value = "select count(*) from camellia_table_ref ctr left join camellia_table ct on ctr.tid=ct.tid  " +
            "where 1=1 " +
            "AND if(:tid is not null,ctr.tid=:tid,1=1) " +
            "AND if(:bid is not null,ctr.bid=:bid,1=1) " +
            "AND if(:bgroup is not null,ctr.bgroup=:bgroup,1=1) " +
            "AND if(:info is not null,ctr.info like CONCAT('%',:info,'%'),1=1) "+
            "AND if(:validFlag is not null,ctr.valid_flag=:validFlag,1=1) " +
            "AND if(:resourceInfo is not null,ct.info like CONCAT('%',:resourceInfo,'%'),1=1) ", nativeQuery = true)
    Integer countByTidBidBGroupValid(@Param("tid") Long tid,
                                     @Param("bid") Long bid,
                                     @Param("bgroup") String bgroup,
                                     @Param("validFlag") Integer validFlag,
                                     @Param("info") String info,
                                     @Param("resourceInfo")String resourceInfo);

}
