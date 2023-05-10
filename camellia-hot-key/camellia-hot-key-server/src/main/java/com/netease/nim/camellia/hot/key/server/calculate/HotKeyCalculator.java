package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.hot.key.server.event.HotKeyEventHandler;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    public HotKeyCalculator(int id, CacheableHotKeyConfigService configService, HotKeyCounterManager hotKeyCounterManager,
                            TopNCounterManager topNCounterManager, HotKeyEventHandler hotKeyEventHandler) {
        this.id = id;
        this.configService = configService;
        this.hotKeyCounterManager = hotKeyCounterManager;
        this.topNCounterManager = topNCounterManager;
        this.hotKeyEventHandler = hotKeyEventHandler;
    }

    /**
     * 热点计算器
     * @param counters 计数
     */
    public void calculate(List<KeyCounter> counters) {
        if (counters == null || counters.isEmpty()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("calculate, id = {}, size = {}", id, counters.size());
        }
        for (KeyCounter counter : counters) {
            //获取规则
            Rule rule = getRule(counter);
            if (rule == null) continue;
            //计算是否是热点
            boolean hot = hotKeyCounterManager.check(counter.getNamespace(),
                    counter.getKey() + "|" + counter.getAction().getValue(), rule, counter.getCount());
            if (hot) {
                //如果是热点，推给hotKeyEventHandler处理
                hotKeyEventHandler.newHotKey(new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), rule.getExpireMillis()));
            }
            //如果是key的更新/删除操作，则需要看看是否需要广播
            if (counter.getAction() == KeyAction.DELETE || counter.getAction() == KeyAction.UPDATE) {
                hotKeyEventHandler.hotKeyUpdate(counter);
            }
            //计算topN
            topNCounterManager.update(counter);
        }
    }

    private Rule getRule(KeyCounter counter) {
        HotKeyConfig hotKeyConfig = configService.get(counter.getNamespace());
        return RuleUtils.rulePass(hotKeyConfig, counter.getKey());
    }
}
