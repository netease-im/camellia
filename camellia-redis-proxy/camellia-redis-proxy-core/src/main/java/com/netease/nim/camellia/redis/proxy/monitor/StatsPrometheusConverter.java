package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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
            sb.append("# TYPE redis_proxy_total counter\n");
            String totalFormat = "redis_proxy_total{type=\"%s\",} %d%n";
            sb.append(String.format(totalFormat, "all", stats.getCount()));
            sb.append(String.format(totalFormat, "read", stats.getTotalReadCount()));
            sb.append(String.format(totalFormat, "write", stats.getTotalWriteCount()));

            // ====total====
            sb.append("# HELP redis_proxy_total_command Redis Proxy Total Command\n");
            sb.append("# TYPE redis_proxy_total_command counter\n");
            for (TotalStats totalStats : stats.getTotalStatsList()) {
                sb.append(String.format("redis_proxy_total_command{command=\"%s\",} %d%n", totalStats.getCommand(), totalStats.getCount()));
            }

            // ====bid-bgroup====
            sb.append("# HELP redis_proxy_bid_bgroup Redis Proxy Bid Bgroup\n");
            sb.append("# TYPE redis_proxy_bid_bgroup gauge\n");
            for (BidBgroupStats bgroupStats : stats.getBidBgroupStatsList()) {
                String bid1 = bgroupStats.getBid() == null ? DEFAULT : String.valueOf(bgroupStats.getBid());
                String bgroup1 = bgroupStats.getBgroup() == null ? DEFAULT : bgroupStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup{bid=\"%s\",bgroup=\"%s\",} %d%n",
                        bid1,
                        bgroup1,
                        bgroupStats.getCount()));
            }

            // ====detail====
            sb.append("# HELP redis_proxy_detail Redis Proxy Detail\n");
            sb.append("# TYPE redis_proxy_detail counter\n");
            for (DetailStats detailStats : stats.getDetailStatsList()) {
                String bid2 = detailStats.getBid() == null ? DEFAULT : String.valueOf(detailStats.getBid());
                String bgroup2 = detailStats.getBgroup() == null ? DEFAULT : detailStats.getBgroup();
                sb.append(String.format("redis_proxy_detail{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d%n",
                        bid2,
                        bgroup2,
                        detailStats.getCommand(),
                        detailStats.getCount()));
            }

            // ====fail====
            sb.append("# HELP redis_proxy_fail Redis Proxy Fail\n");
            sb.append("# TYPE redis_proxy_fail counter\n");
            for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
                sb.append(String.format("redis_proxy_fail{reason=\"%s\",} %d%n", entry.getKey(), entry.getValue()));
            }

            // ====spend.stats====
            sb.append("# HELP redis_proxy_spend_stats Redis Proxy Spend Stats\n");
            sb.append("# TYPE redis_proxy_spend_stats summary\n");
            for (SpendStats spendStats : stats.getSpendStatsList()) {
                sb.append(String.format("redis_proxy_spend_stats_sum{command=\"%s\",} %f%n", spendStats.getCommand(), spendStats.getAvgSpendMs() * spendStats.getCount()));
                sb.append(String.format("redis_proxy_spend_stats_count{command=\"%s\",} %d%n", spendStats.getCommand(), spendStats.getCount()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.5\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.75\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.90\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.95\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.99\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"0.999\"} %f%n", spendStats.getCommand(), spendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_spend_stats{command=\"%s\",quantile=\"1\"} %f%n", spendStats.getCommand(), spendStats.getMaxSpendMs()));
            }

            // ====bid-bgroup.spend.stats====
            sb.append("# HELP redis_proxy_bid_bgroup_spend_stats Redis Proxy Bid Bgroup Spend Stats\n");
            sb.append("# TYPE redis_proxy_bid_bgroup_spend_stats summary\n");
            for (BidBgroupSpendStats spendStats : stats.getBidBgroupSpendStatsList()) {
                String bid3 = spendStats.getBid() == null ? DEFAULT : String.valueOf(spendStats.getBid());
                String bgroup3 = spendStats.getBgroup() == null ? DEFAULT : spendStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats_sum{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getAvgSpendMs() * spendStats.getCount()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats_count{bid=\"%s\",bgroup=\"%s\",command=\"%s\",} %d%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getCount()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.5\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.75\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.90\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.95\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.99\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"0.999\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_bid_bgroup_spend_stats{bid=\"%s\",bgroup=\"%s\",command=\"%s\",quantile=\"1\"} %f%n", bid3, bgroup3, spendStats.getCommand(), spendStats.getMaxSpendMs()));
            }

            // ====resource.stats====
            sb.append("# HELP redis_proxy_resource_stats Redis Proxy Resource Stats\n");
            sb.append("# TYPE redis_proxy_resource_stats counter\n");
            for (ResourceStats resourceStats : stats.getResourceStatsList()) {
                sb.append(String.format("redis_proxy_resource_stats{\"resource\"=\"%s\",} %d%n", resourceStats.getResource(), resourceStats.getCount()));
            }

            // ====resource.command.stats====
            sb.append("# HELP redis_proxy_resource_command_stats Redis Proxy Resource Command Stats\n");
            sb.append("# TYPE redis_proxy_resource_command_stats counter\n");
            for (ResourceCommandStats resourceCommandStats : stats.getResourceCommandStatsList()) {
                sb.append(String.format("redis_proxy_resource_command_stats{\"resource\"=\"%s\",command=\"%s\",} %d%n",
                        resourceCommandStats.getResource(), resourceCommandStats.getCommand(), resourceCommandStats.getCount()));
            }

            // ====bid-bgroup.resource.command.stats====
            sb.append("# HELP redis_proxy_bid_bgroup_resource_command_stats Redis Proxy Bid Bgroup Resource Command Stats\n");
            sb.append("# TYPE redis_proxy_bid_bgroup_resource_command_stats counter\n");
            for (ResourceBidBgroupCommandStats commandStats : stats.getResourceBidBgroupCommandStatsList()) {
                String bid4 = commandStats.getBid() == null ? DEFAULT : String.valueOf(commandStats.getBid());
                String bgroup4 = commandStats.getBgroup() == null ? DEFAULT : commandStats.getBgroup();
                sb.append(String.format("redis_proxy_bid_bgroup_resource_command_stats{\"bid\"=\"%s\",bgroup=\"%s\",resource=\"%s\",command=\"%s\",} %d%n",
                        bid4,
                        bgroup4,
                        commandStats.getResource(), commandStats.getCommand(), commandStats.getCount()));
            }

            // ====route.conf====
            // ====redis.connect.stats====
            sb.append("# HELP redis_proxy_redis_connect_stats Redis Proxy Redis Connect Stats\n");
            sb.append("# TYPE redis_proxy_redis_connect_stats gauge\n");
            RedisConnectStats redisConnectStats = stats.getRedisConnectStats();
            for (RedisConnectStats.Detail detail : redisConnectStats.getDetailList()) {
                sb.append(String.format("redis_proxy_redis_connect_stats{redis_addr=\"%s\",} %d%n", detail.getAddr(), detail.getConnectCount()));
            }

            // ====upstream.redis.spend.stats====
            sb.append("# HELP redis_proxy_upstream_redis_spend_stats Redis Proxy Upstream Redis Spend Stats\n");
            sb.append("# TYPE redis_proxy_upstream_redis_spend_stats summary\n");
            List<UpstreamRedisSpendStats> upstreamRedisSpendStatsList = stats.getUpstreamRedisSpendStatsList();
            for (UpstreamRedisSpendStats upstreamRedisSpendStats : upstreamRedisSpendStatsList) {
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats_sum{redis_addr=\"%s\",} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getAvgSpendMs() * upstreamRedisSpendStats.getCount()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats_count{redis_addr=\"%s\",} %d%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getCount()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.5\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP50()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.75\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP75()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.9\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP90()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.95\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP95()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.99\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP99()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"0.999\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getSpendMsP999()));
                sb.append(String.format("redis_proxy_upstream_redis_spend_stats{redis_addr=\"%s\",quantile=\"1\"} %f%n", upstreamRedisSpendStats.getAddr(), upstreamRedisSpendStats.getMaxSpendMs()));
            }

            // TODO: big.key.stats
            // TODO: hot.key.stats
            // TODO: hot.key.cache.stats
            // TODO: slow.command.stats

            // <<<<<<<END<<<<<<<
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return sb.toString();
    }
}
