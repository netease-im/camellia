package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.common.utils.HotKeyConfigUtils;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class HotKeyConfigUtilsTest {

    @Test
    public void shouldCheckAndConvertValidConfig() {
        Rule matchAll = HotKeyTestFixtures.rule("all", RuleType.match_all, "ignored", 251, 10, 5000);
        HotKeyConfig config = HotKeyTestFixtures.config("namespace", matchAll,
                HotKeyTestFixtures.rule("prefix", RuleType.prefix_match, "user:", 350, 20, 4000),
                HotKeyTestFixtures.rule("exact", RuleType.exact_match, "user:1", 400, 30, 3000));

        Assert.assertTrue(HotKeyConfigUtils.checkAndConvert(config));
        Assert.assertNull(matchAll.getKeyConfig());
        Assert.assertEquals(Long.valueOf(300), matchAll.getCheckMillis());
        Assert.assertEquals(Long.valueOf(300), config.getRules().get(1).getCheckMillis());
    }

    @Test
    public void shouldRejectInvalidConfig() {
        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert((HotKeyConfig) null));
        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert(new HotKeyConfig()));

        Rule missingKeyConfig = HotKeyTestFixtures.rule("prefix", RuleType.prefix_match, null, 100, 1, 1000);
        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert(HotKeyTestFixtures.config("namespace", missingKeyConfig)));

        Rule tooSmallWindow = HotKeyTestFixtures.rule("small", RuleType.match_all, null, 99, 1, 1000);
        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert(HotKeyTestFixtures.config("namespace", tooSmallWindow)));

        Rule missingThreshold = HotKeyTestFixtures.rule("missing-threshold", RuleType.match_all, null, 100, 1, 1000);
        missingThreshold.setCheckThreshold(null);
        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert(HotKeyTestFixtures.config("namespace", missingThreshold)));
    }

    @Test
    public void shouldRejectDuplicateRuleName() {
        HotKeyConfig config = new HotKeyConfig();
        config.setNamespace("namespace");
        config.setRules(Arrays.asList(
                HotKeyTestFixtures.rule("dup", RuleType.match_all, null, 100, 1, 1000),
                HotKeyTestFixtures.rule("dup", RuleType.exact_match, "key", 100, 1, 1000)));

        Assert.assertFalse(HotKeyConfigUtils.checkAndConvert(config));
    }

    @Test
    public void shouldDetectConfigChangeWithSortedJson() {
        HotKeyConfig oldConfig = HotKeyTestFixtures.config("namespace",
                HotKeyTestFixtures.rule("rule", RuleType.exact_match, "key", 100, 1, 1000));
        HotKeyConfig sameConfig = HotKeyTestFixtures.config("namespace",
                HotKeyTestFixtures.rule("rule", RuleType.exact_match, "key", 100, 1, 1000));
        HotKeyConfig newConfig = HotKeyTestFixtures.config("namespace",
                HotKeyTestFixtures.rule("rule", RuleType.exact_match, "key2", 100, 1, 1000));

        Assert.assertFalse(HotKeyConfigUtils.isChange(oldConfig, sameConfig));
        Assert.assertTrue(HotKeyConfigUtils.isChange(oldConfig, newConfig));
        Assert.assertTrue(HotKeyConfigUtils.isChange(null, newConfig));
        Assert.assertTrue(HotKeyConfigUtils.isChange(oldConfig, null));
    }
}
