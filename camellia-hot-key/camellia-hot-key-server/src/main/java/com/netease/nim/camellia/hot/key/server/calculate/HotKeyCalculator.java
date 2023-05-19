package com.netease.nim.camellia.hot.key.server.calculate;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.hot.key.server.event.HotKeyEventHandler;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyCalculatorMonitor;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyCalculatorMonitorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCalculator {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCalculator.class);

    private final int id;
    private final CacheableHotKeyConfigService configService;
    private final HotKeyCounterManager hotKeyCounterManager;
    private final HotKeyEventHandler hotKeyEventHandler;
    private final TopNCounterManager topNCounterManager;
    private final HotKeyCalculatorMonitor monitor = new HotKeyCalculatorMonitor();

    public HotKeyCalculator(int id, CacheableHotKeyConfigService configService, HotKeyCounterManager hotKeyCounterManager,
                            TopNCounterManager topNCounterManager, HotKeyEventHandler hotKeyEventHandler) {
        this.id = id;
        this.configService = configService;
        this.hotKeyCounterManager = hotKeyCounterManager;
        this.topNCounterManager = topNCounterManager;
        this.hotKeyEventHandler = hotKeyEventHandler;
        HotKeyCalculatorMonitorCollector.register(id, monitor);
    }

    /**
     * 热点计算器，单线程执行
     * @param counter 计数
     */
    public void calculate(KeyCounter counter) {
        if (logger.isDebugEnabled()) {
            logger.debug("calculate, id = {}, counter = {}", id, JSONObject.toJSONString(counter));
        }
        //获取规则
        Rule rule = getRule(counter);
        if (rule == null) {
            //监控埋点
            monitor.updateRuleNotMatch(counter.getNamespace(), 1);
            return;
        }
        String uniqueKey = counter.getKey() + "|" + counter.getAction().getValue();
        //计算是否是热点
        long current = hotKeyCounterManager.update(counter.getNamespace(),
                uniqueKey, rule, counter.getCount());
        boolean hot = current >= rule.getCheckThreshold();
        if (hot) {
            //如果是热点，推给hotKeyEventHandler处理
            HotKey hotKey = new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), rule.getExpireMillis());
            hotKeyEventHandler.newHotKey(hotKey, rule, current);
        }
        //如果是key的更新/删除操作，则需要看看是否需要广播
        if (counter.getAction() == KeyAction.DELETE || counter.getAction() == KeyAction.UPDATE) {
            hotKeyEventHandler.hotKeyUpdate(counter);
        }
        //计算topN
        topNCounterManager.update(counter);

        //监控埋点
        if (hot) {
            monitor.updateHot(counter.getNamespace(), 1);
        } else {
            monitor.updateNormal(counter.getNamespace(), 1);
        }
    }

    private Rule getRule(KeyCounter counter) {
        HotKeyConfig hotKeyConfig = configService.get(counter.getNamespace());
        return RuleUtils.rulePass(hotKeyConfig, counter.getKey());
    }
}
