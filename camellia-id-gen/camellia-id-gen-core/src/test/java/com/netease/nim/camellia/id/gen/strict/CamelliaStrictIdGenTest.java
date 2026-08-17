package com.netease.nim.camellia.id.gen.strict;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CamelliaStrictIdGenTest {

    @Test
    public void genIdIfCachedShouldOnlyConsumeRedisCache() {
        CamelliaRedisTemplate template = mock(CamelliaRedisTemplate.class);
        when(template.rpop("test|tagA")).thenReturn("101").thenReturn(null);
        CamelliaStrictIdGen idGen = newIdGen(template);

        Assert.assertEquals(Long.valueOf(101L), idGen.genIdIfCached("tagA"));
        Assert.assertNull(idGen.genIdIfCached("tagA"));
    }

    @Test
    public void peekIdIfCachedShouldReturnOldestCachedIdWithoutFallbackLoad() {
        CamelliaRedisTemplate template = mock(CamelliaRedisTemplate.class);
        when(template.lindex("test|tagA", -1)).thenReturn("102").thenReturn(null);
        CamelliaStrictIdGen idGen = newIdGen(template);

        Assert.assertEquals(Long.valueOf(102L), idGen.peekIdIfCached("tagA"));
        Assert.assertNull(idGen.peekIdIfCached("tagA"));
    }

    private CamelliaStrictIdGen newIdGen(CamelliaRedisTemplate template) {
        return new CamelliaStrictIdGen(baseConfig(template, cacheOnlyLoader()));
    }

    private CamelliaStrictIdGenConfig baseConfig(CamelliaRedisTemplate template, IDLoader idLoader) {
        CamelliaStrictIdGenConfig config = new CamelliaStrictIdGenConfig();
        config.setTemplate(template);
        config.setCacheKeyPrefix("test");
        config.setCacheExpireSeconds(60);
        config.setIdLoader(idLoader);
        return config;
    }

    private IDLoader cacheOnlyLoader() {
        return new IDLoader() {
            @Override
            public IDRange load(String tag, int step) {
                throw new AssertionError("cache-only method should not load ids");
            }
        };
    }
}
