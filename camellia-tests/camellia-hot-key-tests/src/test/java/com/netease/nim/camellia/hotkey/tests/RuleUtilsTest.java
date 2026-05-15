package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import org.junit.Assert;
import org.junit.Test;

public class RuleUtilsTest {

    @Test
    public void shouldMatchAllSupportedRuleTypes() {
        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("exact", RuleType.exact_match, "key", 100, 1, 1000), "key"));
        Assert.assertFalse(RuleUtils.rulePass(HotKeyTestFixtures.rule("exact", RuleType.exact_match, "key", 100, 1, 1000), "key2"));

        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("prefix", RuleType.prefix_match, "user:", 100, 1, 1000), "user:1"));
        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("contains", RuleType.contains, ":hot:", 100, 1, 1000), "a:hot:b"));
        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("suffix", RuleType.suffix_match, ":tail", 100, 1, 1000), "a:tail"));
        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("not", RuleType.not_contains, ":cold:", 100, 1, 1000), "a:hot:b"));
        Assert.assertFalse(RuleUtils.rulePass(HotKeyTestFixtures.rule("not", RuleType.not_contains, ":cold:", 100, 1, 1000), "a:cold:b"));
        Assert.assertTrue(RuleUtils.rulePass(HotKeyTestFixtures.rule("all", RuleType.match_all, null, 100, 1, 1000), "any"));
    }

    @Test
    public void shouldReturnFirstMatchedRuleFromConfig() {
        HotKeyConfig config = HotKeyTestFixtures.config("namespace",
                HotKeyTestFixtures.rule("prefix", RuleType.prefix_match, "user:", 100, 1, 1000),
                HotKeyTestFixtures.rule("contains", RuleType.contains, "1", 100, 1, 1000));

        Assert.assertEquals("prefix", RuleUtils.rulePass(config, "user:1").getName());
        Assert.assertNull(RuleUtils.rulePass(config, "order:2"));
    }

    @Test
    public void shouldHandleNullInputs() {
        Assert.assertNull(RuleUtils.rulePass((HotKeyConfig) null, "key"));
        Assert.assertNull(RuleUtils.rulePass(new HotKeyConfig(), "key"));
        Assert.assertNull(RuleUtils.rulePass(HotKeyTestFixtures.config("namespace",
                HotKeyTestFixtures.rule("all", RuleType.match_all, null, 100, 1, 1000)), null));
        Assert.assertFalse(RuleUtils.rulePass((com.netease.nim.camellia.hot.key.common.model.Rule) null, "key"));
        Assert.assertFalse(RuleUtils.rulePass(HotKeyTestFixtures.rule("all", RuleType.match_all, null, 100, 1, 1000), null));
    }
}
