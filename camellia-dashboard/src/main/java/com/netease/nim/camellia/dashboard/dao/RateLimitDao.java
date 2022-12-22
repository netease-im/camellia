package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.RateLimit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Repository
public interface RateLimitDao extends JpaRepository<RateLimit, Long> {
    boolean existsByBidAndBgroup(Long bid, String bgroup);

    boolean existsById(long id);


    @Query(
            value = "select camellia_rate_limit.*\n" +
                    "from camellia_rate_limit\n" +
                    "where if(:bid is not null, bid = :bid, true)\n" +
                    "  and if(:bgroup is not null, bgroup = :bgroup, true)\n",
            countQuery = "select count(*)\n" +
                    "from camellia_rate_limit\n" +
                    "where if(:bid is not null, bid = :bid, true)\n" +
                    "  and if(:bgroup is not null, bgroup = :bgroup, true)\n",
            nativeQuery = true
    )
    Page<RateLimit> findAllByConditions(@Param("bid") Long bid, @Param("bgroup") String bgroup, Pageable pageable);

    boolean existsByBidAndAndBgroup(long bid, String bgroup);
}



