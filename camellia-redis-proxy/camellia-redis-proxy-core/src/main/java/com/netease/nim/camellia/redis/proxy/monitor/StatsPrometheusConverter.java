package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stats converter into prometheus data layout
 */
public class StatsPrometheusConverter {
    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.stats.prometheus");
    private static final String DEFAULT = "default";

    private StatsPrometheusConverter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * convert stats into prometheus data layout
     *
     * @param stats stats
     * @return prometheus data layout
     */
    public static String converter(Stats stats) {

        StringBuilder sb = new StringBuilder();
        try {
            // >>>>>>>START>>>>>>>
            sb.append("# HELP redis_proxy_connect_count Redis Proxy Connect Count\n");
            sb.append("# TYPE redis_proxy_connect_count gauge\n");
            sb.append("redis_proxy_connect_count ").append(stats.getClientConnectCount()).append("\n");

            sb.append("# HELP redis_proxy_total Redis Proxy Total\n");
            sb.append("# TYPE redis_proxy_total gauge\n");
            String totalFormat = "redis_proxy_total{type=\"%s\",} %d\n";
            sb.append(String.format(totalFormat, "all", stats.getCount()));
            sb.append(String.format(totalFormat, "read", stats.getTotalReadCount()));
            sb.append(String.format(totalFormat, "write", stats.getTotalWriteCount()));
            sb.append(String.format(totalFormat, "max_qps", stats.getMaxQps()));
            sb.append(String.format(totalFormat, "max_read_qps", stats.getMaxReadQps()));
            sb.append(String.format(totalFormat, "max_write_qps", stats.getMaxWriteQps()));

            // ====total====
            sb.append("# HELP redis_proxy_total_command Redis Proxy Total Command\n");
            sb.append("# TYPE redis_proxy_total_command gauge\n");
            for (TotalStats totalStats : stats.getTotalStatsList()) {
                sb.append(String.format("redis_proxy_total_command{command=\"%s\",} %d\n", totalStats.getCommand(), totalStats.getCount()));
            }

            // ====bid-bgroup====
            sb.append("# HELP redis_proxy_bid_bgroup Redis Proxy Bid Bgroup\n");
            sb.append("# TYPE redis_proxy_bid_bgroup gauge\n");
            for (BidBgroupStats bgroupStats : stats.getBidBgroupStatsList()) {
                String bid = bgroupStats.getBid() == null ? DEFAULT : String.valueOf(bgroupStats.getBid());
                String bgroup = bgroupStats.getBgroup() == null ? DEFAULT : bgroupStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup{bid=\"%s\",bgroup=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        bgroupStats.getCount()));
            }

            // ====detail====
            sb.append("# HELP redis_proxy_detail Redis Proxy Detail\n");
            sb.append("# TYPE redis_proxy_detail gauge\n");
            for (DetailStats detailStats : stats.getDetailStatsList()) {
                String bid = detailStats.getBid() == null ? DEFAULT : String.valueOf(detailStats.getBid());
                String bgroup = detailStats.getBgroup() == null ? DEFAULT : detailStats.getBgroup();
                sb.append(String.format("redis_proxy_detail{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        detailStats.getCommand(),
                        detailStats.getCount()));
            }

            // ====fail====
            sb.append("# HELP redis_proxy_fail Redis Proxy Fail\n");
            sb.append("# TYPE redis_proxy_fail gauge\n");
            for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
                sb.append(String.format("redis_proxy_fail{reason=\"%s\",} %d\n", entry.getKey(), entry.getValue()));
            }

            // ====spend.stats====
            sb.append("# HELP redis_proxy_spend_stats Redis Proxy Spend Stats\n");
            sb.append("# TYPE redis_proxy_spend_stats summary\n");
            for (SpendStats spendStats : stats.getSpendStatsList()) {
                sb.append(String.format("redis_proxy_spend_stats_sum{command=\"%s\",} %f\n", spendStats.getCommand(), spendStats.getAvgSpendMs() * spendStats.getCount()));
                sb.append(String.format("redis_proxy_spend_stats_count{command=\"%s\",} %d\n", spendStats.getCommand(), spendStats.getCount()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.5\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.75\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.90\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.95\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.99\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.999\"} %f\n", spendStats.getCommand(), spendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"1\"} %f\n", spendStats.getCommand(), spendStats.getMaxSpendMs()));
            }

            // ====bid-bgroup.spend.stats====
            sb.append("# HELP redis_proxy_bid_bgroup_spend_stats Redis Proxy Bid Bgroup Spend Stats\n");
            sb.append("# TYPE redis_proxy_bid_bgroup_spend_stats summary\n");
            for (BidBgroupSpendStats spendStats : stats.getBidBgroupSpendStatsList()) {
                String bid = spendStats.getBid() == null ? DEFAULT : String.valueOf(spendStats.getBid());
                String bgroup = spendStats.getBgroup() == null ? DEFAULT : spendStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats_sum{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getAvgSpendMs() * spendStats.getCount()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats_count{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d\n", bid, bgroup, spendStats.getCommand(), spendStats.getCount()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.5\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.75\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.90\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.95\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.99\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.999\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"1\"} %f\n", bid, bgroup, spendStats.getCommand(), spendStats.getMaxSpendMs()));
            }

            // ====resource.stats====
            sb.append("# HELP redis_proxy_resource_stats Redis Proxy Resource Stats\n");
            sb.append("# TYPE redis_proxy_resource_stats gauge\n");
            for (ResourceStats resourceStats : stats.getResourceStatsList()) {
                sb.append(String.format("redis_proxy_resource_stats{resource=\"%s\",} %d\n", resourceStats.getResource(), resourceStats.getCount()));
            }

            // ====resource.command.stats====
            sb.append("# HELP redis_proxy_resource_command_stats Redis Proxy Resource Command Stats\n");
            sb.append("# TYPE redis_proxy_resource_command_stats gauge\n");
            for (ResourceCommandStats resourceCommandStats : stats.getResourceCommandStatsList()) {
                sb.append(String.format("redis_proxy_resource_command_stats{resource=\"%s\",command=\"%s\",} %d\n",
                        resourceCommandStats.getResource(), resourceCommandStats.getCommand(), resourceCommandStats.getCount()));
            }

            // ====bid-bgroup.resource.command.stats====
            sb.append("# HELP redis_proxy_bid_bgroup_resource_command_stats Redis Proxy Bid Bgroup Resource Command Stats\n");
            sb.append("# TYPE redis_proxy_bid_bgroup_resource_command_stats gauge\n");
            for (ResourceBidBgroupCommandStats commandStats : stats.getResourceBidBgroupCommandStatsList()) {
                String bid = commandStats.getBid() == null ? DEFAULT : String.valueOf(commandStats.getBid());
                String bgroup = commandStats.getBgroup() == null ? DEFAULT : commandStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup_resource_command_stats{bid=\"%s\",bgroup=\"%s\",resource=\"%s\",command=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        commandStats.getResource(), commandStats.getCommand(), commandStats.getCount()));
            }

            // ====route.conf====
            sb.append("# HELP redis_proxy_route_conf Redis Proxy Route Conf\n");
            sb.append("# TYPE redis_proxy_route_conf gauge\n");
            for (RouteConf routeConf : stats.getRouteConfList()) {
                String bid = routeConf.getBid() == null ? DEFAULT : String.valueOf(routeConf.getBid());
                String bgroup = routeConf.getBgroup() == null ? DEFAULT : routeConf.getBgroup();
                String resourceTable = routeConf.getResourceTable();
                String normalizeResourceTable = resourceTable.replace("\"", "");
                sb.append(String.format("redis_proxy_route_conf{bid=\"%s\",bgroup=\"%s\",routeConf=\"%s\",updateTime=\"%d\",} 1\n",
                        bid,
                        bgroup,
                        normalizeResourceTable,
                        routeConf.getUpdateTime()));
            }
            // ====redis.connect.stats====
            sb.append("# HELP redis_proxy_redis_connect_stats Redis Proxy Redis Connect Stats\n");
            sb.append("# TYPE redis_proxy_redis_connect_stats gauge\n");
            RedisConnectStats redisConnectStats = stats.getRedisConnectStats();
            for (RedisConnectStats.Detail detail : redisConnectStats.getDetailList()) {
                sb.append(String.format("redis_proxy_redis_connect_stats{redis_addr=\"%s\",} %d\n", detail.getAddr(), detail.getConnectCount()));
            }

            // ====upstream.redis.spend.stats====
            sb.append("# HELP redis_proxy_upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats\n");
            sb.append("# TYPE redis_proxy_upstream_redis_spend_stats summary\n");
            List<UpstreamRedisSpendStats> upstreamRedisSpendStatsList = stats.getUpstreamRedisSpendStatsList();
            for (UpstreamRedisSpendStats upstreamRedisSpendStats : upstreamRedisSpendStatsList) {
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats_sum{redis_addr=\"%s\",} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getAvgSpendMs() * upstreamRedisSpendStats.getCount()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats_count{redis_addr=\"%s\",} %d\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getCount()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.5\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.75\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.9\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.95\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.99\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.999\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"1\"} %f\n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getMaxSpendMs()));
            }

            // ====big.key.stats====
            sb.append("# HELP redis_proxy_big_key_stats Redis Proxy Big Key Stats\n");
            sb.append("# TYPE redis_proxy_big_key_stats gauge\n");
            List<BigKeyStats> bigKeyStatsList = stats.getBigKeyStatsList();
            // BigKeyStatsMap: BigKeyStats group by bid, bgroup and command and count the number of keys
            Map<BigKeyStatsKey, Long> bigKeyStatsMap = bigKeyStatsList.stream()
                    .collect(
                            Collectors.groupingBy(
                                    bigKeyStats -> new BigKeyStatsKey(bigKeyStats.getBid(), bigKeyStats.getBgroup(), bigKeyStats.getCommand()),
                                    Collectors.counting()
                            )
                    );
            for (Map.Entry<BigKeyStatsKey, Long> entry : bigKeyStatsMap.entrySet()) {
                BigKeyStatsKey bigKeyStatsKey = entry.getKey();
                String bid = bigKeyStatsKey.getBid() == null ? DEFAULT : bigKeyStatsKey.getBid();
                String bgroup = bigKeyStatsKey.getBgroup() == null ? DEFAULT : bigKeyStatsKey.getBgroup();
                sb.append(String.format("redis_proxy_big_key_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        bigKeyStatsKey.getCommand(),
                        entry.getValue()));
            }

            // ====hot.key.stats====
            sb.append("# HELP redis_proxy_hot_key_stats Redis Proxy Hot Key Stats\n");
            sb.append("# TYPE redis_proxy_hot_key_stats gauge\n");
            List<HotKeyStats> hotKeyStatsList = stats.getHotKeyStatsList();
            // HotKeyStatsMap: HotKeyStats group by bid, bgroup and count the number of keys
            Map<HotKeyStatsKey, Long> hotKeyStatsMap = hotKeyStatsList.stream()
                    .collect(
                            Collectors.groupingBy(
                                    hotKeyStats -> new HotKeyStatsKey(hotKeyStats.getBid(), hotKeyStats.getBgroup()),
                                    Collectors.counting()
                            )
                    );
            for (Map.Entry<HotKeyStatsKey, Long> entry : hotKeyStatsMap.entrySet()) {
                HotKeyStatsKey hotKeyStatsKey = entry.getKey();
                String bid = hotKeyStatsKey.getBid() == null ? DEFAULT : hotKeyStatsKey.getBid();
                String bgroup = hotKeyStatsKey.getBgroup() == null ? DEFAULT : hotKeyStatsKey.getBgroup();
                sb.append(String.format("redis_proxy_hot_key_stats{bid=\"%s\",bgroup=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        entry.getValue()));
            }

            // ====hot.key.cache.stats====
            sb.append("# HELP redis_proxy_hot_key_cache_stats Redis Proxy Hot Key Cache Stats\n");
            sb.append("# TYPE redis_proxy_hot_key_cache_stats gauge\n");
            List<HotKeyCacheStats> hotKeyCacheStatsList = stats.getHotKeyCacheStatsList();
            // HotKeyCacheStatsMap: HotKeyCacheStats group by bid, bgroup and count the number of keys
            Map<HotKeyCacheStatsKey, Long> hotKeyCacheStatsMap = hotKeyCacheStatsList.stream()
                    .collect(
                            Collectors.groupingBy(
                                    hotKeyCacheStats -> new HotKeyCacheStatsKey(hotKeyCacheStats.getBid(), hotKeyCacheStats.getBgroup()),
                                    Collectors.counting()
                            )
                    );

            for (Map.Entry<HotKeyCacheStatsKey, Long> entry : hotKeyCacheStatsMap.entrySet()) {
                HotKeyCacheStatsKey hotKeyCacheStatsKey = entry.getKey();
                String bid = hotKeyCacheStatsKey.getBid() == null ? DEFAULT : hotKeyCacheStatsKey.getBid();
                String bgroup = hotKeyCacheStatsKey.getBgroup() == null ? DEFAULT : hotKeyCacheStatsKey.getBgroup();
                sb.append(String.format("redis_proxy_hot_key_cache_stats{bid=\"%s\",bgroup=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        entry.getValue()));
            }

            // ====slow.command.stats====
            sb.append("# HELP redis_proxy_slow_command_stats Redis Proxy Slow Command Stats\n");
            sb.append("# TYPE redis_proxy_slow_command_stats gauge\n");
            List<SlowCommandStats> slowCommandStatsList = stats.getSlowCommandStatsList();
            // SlowCommandStatsMap: SlowCommandStats group by bid, bgroup and command and count the number of keys
            Map<SlowCommandStatsKey, Long> slowCommandStatsMap = slowCommandStatsList.stream()
                    .collect(
                            Collectors.groupingBy(
                                    slowCommandStats -> new SlowCommandStatsKey(slowCommandStats.getBid(), slowCommandStats.getBgroup(), slowCommandStats.getCommand()),
                                    Collectors.counting()
                            )
                    );
            for (Map.Entry<SlowCommandStatsKey, Long> entry : slowCommandStatsMap.entrySet()) {
                SlowCommandStatsKey slowCommandStatsKey = entry.getKey();
                String bid = slowCommandStatsKey.getBid() == null ? DEFAULT : slowCommandStatsKey.getBid();
                String bgroup = slowCommandStatsKey.getBgroup() == null ? DEFAULT : slowCommandStatsKey.getBgroup();
                sb.append(String.format("redis_proxy_slow_command_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d\n",
                        bid,
                        bgroup,
                        slowCommandStatsKey.getCommand(),
                        entry.getValue()));
            }

            // ====upstream.fail.stats====
            sb.append("# HELP redis_proxy_upstream_fail_stats Redis Proxy Upstream Fail Stats\n");
            sb.append("# TYPE redis_proxy_upstream_fail_stats gauge\n");
            List<UpstreamFailStats> upstreamFailStatsList = stats.getUpstreamFailStatsList();
            for (UpstreamFailStats upstreamFailStats : upstreamFailStatsList) {
                sb.append(String.format("redis_proxy_upstream_fail_stats{resource=\"%s\",command=\"%s\",msg=\"%s\",} %d\n",
                        upstreamFailStats.getResource(),
                        upstreamFailStats.getCommand(),
                        upstreamFailStats.getMsg(),
                        upstreamFailStats.getCount()));
            }

            // <<<<<<<END<<<<<<<
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return sb.toString();
    }


    private static class BigKeyStatsKey extends BigKeyStats {

        public BigKeyStatsKey(String bid, String bgroup, String command) {
            super();
            this.setBid(bid);
            this.setBgroup(bgroup);
            this.setCommand(command);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BigKeyStatsKey that = (BigKeyStatsKey) o;
            return Objects.equals(getBid(), that.getBid()) &&
                    Objects.equals(getBgroup(), that.getBgroup()) &&
                    Objects.equals(getCommand(), that.getCommand());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBid(), getBgroup(), getCommand());
        }
    }

    private static class HotKeyStatsKey extends HotKeyStats {
        public HotKeyStatsKey(String bid, String bgroup) {
            super();
            this.setBid(bid);
            this.setBgroup(bgroup);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HotKeyStatsKey that = (HotKeyStatsKey) o;
            return Objects.equals(getBid(), that.getBid()) &&
                    Objects.equals(getBgroup(), that.getBgroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBid(), getBgroup());
        }
    }

    private static class HotKeyCacheStatsKey extends HotKeyCacheStats {
        public HotKeyCacheStatsKey(String bid, String bgroup) {
            super();
            this.setBid(bid);
            this.setBgroup(bgroup);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HotKeyCacheStatsKey that = (HotKeyCacheStatsKey) o;
            return Objects.equals(getBid(), that.getBid()) &&
                    Objects.equals(getBgroup(), that.getBgroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBid(), getBgroup());
        }
    }

    private static class SlowCommandStatsKey extends SlowCommandStats {
        public SlowCommandStatsKey(String bid, String bgroup, String command) {
            super();
            this.setBid(bid);
            this.setBgroup(bgroup);
            this.setCommand(command);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SlowCommandStatsKey that = (SlowCommandStatsKey) o;
            return Objects.equals(getBid(), that.getBid()) &&
                    Objects.equals(getBgroup(), that.getBgroup()) &&
                    Objects.equals(getCommand(), that.getCommand());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBid(), getBgroup(), getCommand());
        }
    }
}

