package com.netease.nim.camellia.hot.key.server;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCalculator {

    private final ConcurrentLinkedHashMap<String, ConcurrentLinkedHashMap<String, HotKeyCounter>> counterMap;

    private final ConcurrentLinkedHashMap<String, TopNCounter> topNCounterMap;

    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyServerProperties properties;
    private final HotKeyEventHandler hotKeyEventHandler;

    public HotKeyCalculator(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService, HotKeyNotifyService hotKeyNotifyService) {
        this.hotKeyConfigService = hotKeyConfigService;
        this.properties = properties;
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, ConcurrentLinkedHashMap<String, HotKeyCounter>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        this.topNCounterMap = new ConcurrentLinkedHashMap.Builder<String, TopNCounter>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        this.hotKeyEventHandler = new HotKeyEventHandler(properties, hotKeyConfigService, hotKeyNotifyService);
        //热key规则发生变化，所有计数器清零
        hotKeyConfigService.registerCallback(counterMap::remove);
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
            //获取滑动窗口热点计算器
            HotKeyCounter hotKeyCounter = getHotKeyCounter(counter.getNamespace(), counter.getKey() + "|" + counter.getAction(), rule);
            //计算是否是热点
            boolean hot = hotKeyCounter.addAndCheckHot(counter.getCount());
            if (hot) {
                //如果是热点，推给hotKeyEventHandler处理
                hotKeyEventHandler.newHotKey(new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), rule.getExpireMillis()));
            }
            //如果是key的更新/删除操作，则需要看看是否需要广播
            if (counter.getAction() == KeyAction.DELETE || counter.getAction() == KeyAction.UPDATE) {
                hotKeyEventHandler.hotKeyUpdate(counter);
            }
            //计算topN
            TopNCounter topNCounter = getTopNCounter(counter.getNamespace());
            topNCounter.update(counter);
        }
    }

    private TopNCounter getTopNCounter(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(topNCounterMap, namespace, n -> new TopNCounter(namespace));
    }

    private Rule getRule(KeyCounter counter) {
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(counter.getNamespace());
        return RuleUtils.rulePass(hotKeyConfig, counter.getKey());
    }

    private ConcurrentLinkedHashMap<String, HotKeyCounter> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, n -> new ConcurrentLinkedHashMap.Builder<String, HotKeyCounter>()
                .initialCapacity(properties.getCacheCapacityPerNamespace())
                .maximumWeightedCapacity(properties.getCacheCapacityPerNamespace())
                .build());
    }

    private HotKeyCounter getHotKeyCounter(String namespace, String key, Rule rule) {
        ConcurrentLinkedHashMap<String, HotKeyCounter> map = getMap(namespace);
        return CamelliaMapUtils.computeIfAbsent(map, key, k -> new HotKeyCounter(namespace, key, rule));
    }
}
