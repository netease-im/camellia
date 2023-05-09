package com.netease.nim.camellia.hot.key.server;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.HotKeyCounter;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCalculator {

    private final ConcurrentLinkedHashMap<String, ConcurrentLinkedHashMap<String, SlidingWindowHotKeyCounter>> counterMap;

    private final ConcurrentLinkedHashMap<String, TopNCounter> topNCounterMap;

    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyServerProperties properties;
    private final HotKeyEventHandler hotKeyEventHandler;

    public HotKeyCalculator(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService) {
        this.hotKeyConfigService = hotKeyConfigService;
        this.properties = properties;
        this.counterMap = new ConcurrentLinkedHashMap.Builder<String, ConcurrentLinkedHashMap<String, SlidingWindowHotKeyCounter>>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        this.topNCounterMap = new ConcurrentLinkedHashMap.Builder<String, TopNCounter>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        this.hotKeyEventHandler = new HotKeyEventHandler(hotKeyConfigService);
        //热key规则发生变化，所有计数器清零
        hotKeyConfigService.registerCallback(counterMap::remove);
    }

    /**
     * 热点计算器
     * @param counters 计数
     */
    public void calculate(List<HotKeyCounter> counters) {
        if (counters == null || counters.isEmpty()) {
            return;
        }
        for (HotKeyCounter counter : counters) {
            //获取规则
            Rule rule = getRule(counter);
            if (rule == null) continue;
            //获取滑动窗口热点计算器
            SlidingWindowHotKeyCounter slidingWindowCounter = getSlidingWindowCounter(counter.getNamespace(), counter.getKey() + "|" + counter.getAction(), rule);
            //计算是否是热点
            boolean hot = slidingWindowCounter.addAndCheckHot(counter.getCount());
            if (hot) {
                //如果是热点，推给hotKeyEventHandler处理
                hotKeyEventHandler.hotKey(new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), rule.getExpireMillis()));
            }
            //计算topN
            TopNCounter topNCounter = getTopNCounter(counter.getNamespace());
            topNCounter.update(counter);
        }
    }

    private TopNCounter getTopNCounter(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(topNCounterMap, namespace, n -> new TopNCounter(namespace));
    }

    private Rule getRule(HotKeyCounter counter) {
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(counter.getNamespace());
        return RuleUtils.rulePass(hotKeyConfig, counter.getKey());
    }

    private ConcurrentLinkedHashMap<String, SlidingWindowHotKeyCounter> getMap(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(counterMap, namespace, n -> new ConcurrentLinkedHashMap.Builder<String, SlidingWindowHotKeyCounter>()
                .initialCapacity(properties.getCacheCapacityPerNamespace())
                .maximumWeightedCapacity(properties.getCacheCapacityPerNamespace())
                .build());
    }

    private SlidingWindowHotKeyCounter getSlidingWindowCounter(String namespace, String key, Rule rule) {
        ConcurrentLinkedHashMap<String, SlidingWindowHotKeyCounter> map = getMap(namespace);
        return CamelliaMapUtils.computeIfAbsent(map, key, k -> new SlidingWindowHotKeyCounter(namespace, key, rule));
    }
}
