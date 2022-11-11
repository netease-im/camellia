package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.constant.IpCheckMode;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.dashboard.model.TableRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Repository
public interface IpCheckerDao extends JpaRepository<IpChecker, Long> {
    @Query(
            value = "select camellia_ip_checker.*\n" +
                    "from camellia_ip_checker\n" +
                    "where if(:bid is not null, bid = :bid, true)\n" +
                    "  and if(:bgroup is not null, bgroup = :bgroup, true)\n" +
                    "  and if(:mode is not null, `mode` = :mode, true)\n" +
                    "  and if(:ip is not null, ip_list like concat('%', :ip, '%'), true)\n",
            countQuery = "select count(*)\n" +
                    "from camellia_ip_checker\n" +
                    "where if(:bid is not null, bid = :bid, true)\n" +
                    "  and if(:bgroup is not null, bgroup = :bgroup, true)\n" +
                    "  and if(:mode is not null, `mode` = :mode, true)\n" +
                    "  and if(:ip is not null, ip_list like concat('%', :ip, '%'), true)\n",
            nativeQuery = true
    )
    Page<IpChecker> findAllBy(
            @Param("bid") Long bid,
            @Param("bgroup") String bgroup,
            @Param("mode") Integer mode,
            @Param("ip") String ip,
            Pageable pageable);

    boolean existsByBidAndBgroup(Long bid, String bgroup);
}



