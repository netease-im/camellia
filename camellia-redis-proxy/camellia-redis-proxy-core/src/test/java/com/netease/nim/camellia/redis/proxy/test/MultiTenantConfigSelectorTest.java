package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigSelector;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class MultiTenantConfigSelectorTest {

    @Test
    public void testPasswordOnlyConfigStillWorks() {
        MultiTenantConfigSelector selector = new MultiTenantConfigSelector(Arrays.asList(
                config(1L, "default", null, "pass1", "redis://@127.0.0.1:6379")
        ));

        ClientIdentity clientIdentity = selector.selectClientIdentity(null, "pass1");
        Assert.assertNotNull(clientIdentity);
        Assert.assertEquals(Long.valueOf(1L), clientIdentity.getBid());
        Assert.assertEquals("default", clientIdentity.getBgroup());
    }

    @Test
    public void testUsernamePasswordExactMatchHasHigherPriorityThanPasswordFallback() {
        MultiTenantConfigSelector selector = new MultiTenantConfigSelector(Arrays.asList(
                config(1L, "fallback", null, "pass1", "redis://@127.0.0.1:6379"),
                config(2L, "userA", "userA", "pass1", "redis://@127.0.0.1:6380"),
                config(3L, "userB", "userB", "pass1", "redis://@127.0.0.1:6381")
        ));

        ClientIdentity userAIdentity = selector.selectClientIdentity("userA", "pass1");
        Assert.assertNotNull(userAIdentity);
        Assert.assertEquals(Long.valueOf(2L), userAIdentity.getBid());
        Assert.assertEquals("userA", userAIdentity.getBgroup());

        ClientIdentity userBIdentity = selector.selectClientIdentity("userB", "pass1");
        Assert.assertNotNull(userBIdentity);
        Assert.assertEquals(Long.valueOf(3L), userBIdentity.getBid());
        Assert.assertEquals("userB", userBIdentity.getBgroup());

        ClientIdentity fallbackIdentity = selector.selectClientIdentity("other", "pass1");
        Assert.assertNotNull(fallbackIdentity);
        Assert.assertEquals(Long.valueOf(1L), fallbackIdentity.getBid());
        Assert.assertEquals("fallback", fallbackIdentity.getBgroup());

        ClientIdentity noUserNameIdentity = selector.selectClientIdentity(null, "pass1");
        Assert.assertNotNull(noUserNameIdentity);
        Assert.assertEquals(Long.valueOf(1L), noUserNameIdentity.getBid());
        Assert.assertEquals("fallback", noUserNameIdentity.getBgroup());
    }

    @Test
    public void testUsernamePasswordConfigRequiresUsernameWhenNoFallbackExists() {
        MultiTenantConfigSelector selector = new MultiTenantConfigSelector(Arrays.asList(
                config(2L, "userA", "userA", "pass1", "redis://@127.0.0.1:6380")
        ));

        ClientIdentity clientIdentity = selector.selectClientIdentity(null, "pass1");
        Assert.assertNull(clientIdentity);
    }

    private MultiTenantConfig config(long bid, String bgroup, String userName, String password, String route) {
        MultiTenantConfig config = new MultiTenantConfig();
        config.setBid(bid);
        config.setBgroup(bgroup);
        config.setUsername(userName);
        config.setPassword(password);
        config.setRoute(route);
        return config;
    }
}
