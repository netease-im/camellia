package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.event.HotKeyEventHandler;
import com.netease.nim.camellia.hot.key.server.notify.HotKeyNotifyService;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCalculator {

    private final HotKeyCounterManager hotKeyCounterManager;
    private final TopNCounterManager topNCounterManager;

    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyEventHandler hotKeyEventHandler;

    public HotKeyCalculator(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService,
                            HotKeyNotifyService hotKeyNotifyService, HotKeyCallbackManager callbackManager) {
        this.hotKeyConfigService = hotKeyConfigService;
        this.hotKeyCounterManager = new HotKeyCounterManager(properties);
        this.topNCounterManager = new TopNCounterManager(properties);
        this.hotKeyEventHandler = new HotKeyEventHandler(properties, hotKeyConfigService, hotKeyNotifyService, callbackManager);
        //热key规则发生变化，所有计数器清零
        hotKeyConfigService.registerCallback(hotKeyCounterManager::remove);
    }

    /**
     * 热点计算器
     * @param counters 计数
     */
    public void calculate(List<KeyCounter> counters) {
        if (counters == null || counters.isEmpty()) {
            return;
        }
        for (KeyCounter counter : counters) {
            //获取规则
            Rule rule = getRule(counter);
            if (rule == null) continue;
            //计算是否是热点
            boolean hot = hotKeyCounterManager.addAndCheckHot(counter.getNamespace(),
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
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(counter.getNamespace());
        return RuleUtils.rulePass(hotKeyConfig, counter.getKey());
    }
}
