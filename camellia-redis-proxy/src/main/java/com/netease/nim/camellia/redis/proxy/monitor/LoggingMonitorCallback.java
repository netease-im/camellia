package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/10/23
 */
public class LoggingMonitorCallback implements MonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.stats");

    @Override
    public void callback(Stats stats) {
        try {
            logger.info(">>>>>>>START>>>>>>>");
            logger.info("connect.count={}", stats.getClientConnectCount());
            logger.info("total.count={}", stats.getCount());
            logger.info("total.read.count={}", stats.getTotalReadCount());
            logger.info("total.write.count={}", stats.getTotalWriteCount());
            logger.info("====total====");
            for (TotalStats totalStats : stats.getTotalStatsList()) {
                logger.info("total.command.{}, count={}", totalStats.getCommand(), totalStats.getCount());
            }
            logger.info("====bidbgroup====");
            for (BidBgroupStats bgroupStats : stats.getBidBgroupStatsList()) {
                logger.info("bidbgroup.{}.{}, count={}", bgroupStats.getBid() == null ? "default" : bgroupStats.getBid(),
                        bgroupStats.getBgroup() == null ? "default" : bgroupStats.getBgroup(), bgroupStats.getCount());
            }
            logger.info("====detail====");
            for (DetailStats detailStats : stats.getDetailStatsList()) {
                logger.info("detail.{}.{}.{}, count={}", detailStats.getBid() == null ? "default" : detailStats.getBid(),
                        detailStats.getBgroup() == null ? "default" : detailStats.getBgroup(), detailStats.getCommand(), detailStats.getCount());
            }
            logger.info("====fail====");
            for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
                logger.info("fail[{}], count = {}", entry.getKey(), entry.getValue());
            }
            logger.info("====spend.stats====");
            for (SpendStats spendStats : stats.getSpendStatsList()) {
                logger.info("command={},count={},avgSpendMs={},maxSpendMs={},spendMsP50={},spendMsP75={},spendMsP90={},spendMsP95={},spendMsP99={},spendMsP999={}",
                        spendStats.getCommand(), spendStats.getCount(), spendStats.getAvgSpendMs(), spendStats.getMaxSpendMs(), spendStats.getSpendMsP50(),
                        spendStats.getSpendMsP75(), spendStats.getSpendMsP90(), spendStats.getSpendMsP95(), spendStats.getSpendMsP99(), spendStats.getSpendMsP999());
            }
            logger.info("====bidbgroup.spend.stats====");
            for (BidBgroupSpendStats spendStats : stats.getBidBgroupSpendStatsList()) {
                logger.info("bid={},bgroup={},command={},count={},avgSpendMs={},maxSpendMs={},spendMsP50={},spendMsP75={},spendMsP90={},spendMsP95={},spendMsP99={},spendMsP999={}", spendStats.getBid() == null ? "default" : spendStats.getBid(),
                        spendStats.getBgroup() == null ? "default" : spendStats.getBgroup(), spendStats.getCommand(), spendStats.getCount(), spendStats.getAvgSpendMs(), spendStats.getMaxSpendMs(),
                        spendStats.getSpendMsP50(), spendStats.getSpendMsP75(), spendStats.getSpendMsP90(),
                        spendStats.getSpendMsP95(), spendStats.getSpendMsP99(), spendStats.getSpendMsP999());
            }
            logger.info("====resource.stats====");
            for (ResourceStats resourceStats : stats.getResourceStatsList()) {
                logger.info("resource={},count={}", resourceStats.getResource(), resourceStats.getCount());
            }
            logger.info("====resource.command.stats====");
            for (ResourceCommandStats resourceCommandStats : stats.getResourceCommandStatsList()) {
                logger.info("resource={},command={},count={}", resourceCommandStats.getResource(), resourceCommandStats.getCommand(), resourceCommandStats.getCount());
            }
            logger.info("====bidbgroup.resource.command.stats====");
            for (ResourceBidBgroupCommandStats commandStats : stats.getResourceBidBgroupCommandStatsList()) {
                logger.info("bid={},bgroup={},resource={},command={},count={}", commandStats.getBid() == null ? "default" : commandStats.getBid(),
                        commandStats.getBgroup() == null ? "default" : commandStats.getBgroup(), commandStats.getResource(), commandStats.getCommand(), commandStats.getCount());
            }
            logger.info("====route.conf====");
            for (RouteConf routeConf : stats.getRouteConfList()) {
                logger.info("bid={},bgroup={},routeConf={},updateTime={}", routeConf.getBid() == null ? "default" : routeConf.getBid(),
                        routeConf.getBgroup() == null ? "default" : routeConf.getBgroup(), routeConf.getResourceTable(), routeConf.getUpdateTime());
            }
            logger.info("====redis.connect.stats====");
            RedisConnectStats redisConnectStats = stats.getRedisConnectStats();
            logger.info("redis.total.connect.count={}", redisConnectStats.getConnectCount());
            for (RedisConnectStats.Detail detail : redisConnectStats.getDetailList()) {
                logger.info("redis.addr={},connect.count={}", detail.getAddr(), detail.getConnectCount());
            }
            logger.info("====upstream.redis.spend.stats====");
            List<UpstreamRedisSpendStats> upstreamRedisSpendStatsList = stats.getUpstreamRedisSpendStatsList();
            for (UpstreamRedisSpendStats upstreamRedisSpendStats : upstreamRedisSpendStatsList) {
                logger.info("addr={},count={},avgSpendMs={},maxSpendMs={},spendMsP50={},spendMsP75={},spendMsP90={},spendMsP95={},spendMsP99={},spendMsP999={}", upstreamRedisSpendStats.getAddr(),
                        upstreamRedisSpendStats.getCount(), upstreamRedisSpendStats.getAvgSpendMs(), upstreamRedisSpendStats.getMaxSpendMs(), upstreamRedisSpendStats.getSpendMsP50(),
                        upstreamRedisSpendStats.getSpendMsP75(), upstreamRedisSpendStats.getSpendMsP90(), upstreamRedisSpendStats.getSpendMsP95(),
                        upstreamRedisSpendStats.getSpendMsP99(), upstreamRedisSpendStats.getSpendMsP999());
            }
            logger.info("====big.key.stats====");
            List<BigKeyStats> bigKeyStatsList = stats.getBigKeyStatsList();
            for (BigKeyStats bigKeyStats : bigKeyStatsList) {
                logger.info("bid={},bgroup={},commandType={},command={},key={},size={},threshold={}", bigKeyStats.getBid(),
                        bigKeyStats.getBgroup(), bigKeyStats.getCommandType(), bigKeyStats.getCommand(), bigKeyStats.getKey(),
                        bigKeyStats.getSize(), bigKeyStats.getThreshold());
            }
            logger.info("====hot.key.stats====");
            List<HotKeyStats> hotKeyStatsList = stats.getHotKeyStatsList();
            for (HotKeyStats hotKeyStats : hotKeyStatsList) {
                logger.info("bid={},bgroup={},key={},times={},max={},avg={},checkMillis={},checkThreshold={}", hotKeyStats.getBid(),
                        hotKeyStats.getBgroup(), hotKeyStats.getKey(), hotKeyStats.getTimes(), hotKeyStats.getMax(),
                        hotKeyStats.getAvg(), hotKeyStats.getCheckMillis(), hotKeyStats.getCheckThreshold());
            }
            logger.info("====hot.key.cache.stats====");
            List<HotKeyCacheStats> hotKeyCacheStatsList = stats.getHotKeyCacheStatsList();
            for (HotKeyCacheStats hotKeyCacheStats : hotKeyCacheStatsList) {
                logger.info("bid={},bgroup={},key={},hitCount={},checkMillis={},checkThreshold={}", hotKeyCacheStats.getBid(),
                        hotKeyCacheStats.getBgroup(), hotKeyCacheStats.getKey(),
                        hotKeyCacheStats.getHitCount(), hotKeyCacheStats.getCheckMillis(), hotKeyCacheStats.getCheckThreshold());
            }

            logger.info("====slow.command.stats====");
            List<SlowCommandStats> slowCommandStatsList = stats.getSlowCommandStatsList();
            for (SlowCommandStats slowCommandStats : slowCommandStatsList) {
                logger.info("bid={},bgroup={},command={},keys={},spendMillis={},thresholdMillis={}", slowCommandStats.getBid(),
                        slowCommandStats.getBgroup(), slowCommandStats.getCommand(),
                        slowCommandStats.getKeys(), slowCommandStats.getSpendMillis(), slowCommandStats.getThresholdMillis());
            }
            logger.info("<<<<<<<END<<<<<<<");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
