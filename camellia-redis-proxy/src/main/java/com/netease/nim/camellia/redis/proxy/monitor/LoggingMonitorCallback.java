package com.netease.nim.camellia.redis.proxy.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/10/23
 */
public class LoggingMonitorCallback implements MonitorCallback {

    private static final Logger logger = LoggerFactory.getLogger("stats");

    @Override
    public void callback(Stats stats) {
        try {
            logger.info(">>>>>>>START>>>>>>>");
            logger.info("connect.count={}", stats.getClientConnectCount());
            logger.info("total.count={}", stats.getCount());
            logger.info("total.read.count={}", stats.getTotalReadCount());
            logger.info("total.write.count={}", stats.getTotalWriteCount());
            logger.info("====total====");
            for (Stats.TotalStats totalStats : stats.getTotalStatsList()) {
                logger.info("total.command.{}, count={}", totalStats.getCommand(), totalStats.getCount());
            }
            logger.info("====bidbgroup====");
            for (Stats.BidBgroupStats bgroupStats : stats.getBidBgroupStatsList()) {
                logger.info("bidbgroup.{}.{}, count={}", bgroupStats.getBid() == null ? "default" : bgroupStats.getBid(),
                        bgroupStats.getBgroup() == null ? "default" : bgroupStats.getBgroup(), bgroupStats.getCount());
            }
            logger.info("====detail====");
            for (Stats.DetailStats detailStats : stats.getDetailStatsList()) {
                logger.info("detail.{}.{}.{}, count={}", detailStats.getBid() == null ? "default" : detailStats.getBid(),
                        detailStats.getBgroup() == null ? "default" : detailStats.getBgroup(), detailStats.getCommand(), detailStats.getCount());
            }
            logger.info("====fail====");
            for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
                logger.info("fail[{}], count = {}", entry.getKey(), entry.getValue());
            }
            logger.info("====spend.stats====");
            for (Stats.SpendStats spendStats : stats.getSpendStatsList()) {
                logger.info("command={},count={},avgSpendMs={},maxSpendMs={}", spendStats.getCommand(), spendStats.getCount(), spendStats.getAvgSpendMs(), spendStats.getMaxSpendMs());
            }
            logger.info("<<<<<<<END<<<<<<<");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
