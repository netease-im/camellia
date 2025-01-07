package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.ValueLocation;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants._4k;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyCodecTest {

    @Test
    public void test() {
        Map<BytesKey, KeyInfo> map = new HashMap<>();
        KeyInfo keyInfo1 = keyInfo("k1", 0, null, "v1");
        KeyInfo keyInfo2 = keyInfo("k2", 10000, null, "v2");
        KeyInfo keyInfo3 = keyInfo("k3", 20000, null, null);
        KeyInfo keyInfo4 = keyInfo("k4", 0, new ValueLocation(new BlockLocation(1, 10), (short) 1000), null);
        KeyInfo keyInfo5 = keyInfo("k5", 30000, new ValueLocation(new BlockLocation(2, 30), (short) 2000), "v5");
        KeyInfo keyInfo6 = keyInfo("k6", 40000, new ValueLocation(new BlockLocation(1, 50), (short) 3000), null);

        map.put(new BytesKey(keyInfo1.getKey()), keyInfo1);
        map.put(new BytesKey(keyInfo2.getKey()), keyInfo2);
        map.put(new BytesKey(keyInfo3.getKey()), keyInfo3);
        map.put(new BytesKey(keyInfo4.getKey()), keyInfo4);
        map.put(new BytesKey(keyInfo5.getKey()), keyInfo5);
        map.put(new BytesKey(keyInfo6.getKey()), keyInfo6);

        byte[] bytes = KeyCodec.encodeBucket(map);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(bytes.length, _4k);

        Map<BytesKey, KeyInfo> decodeMap = KeyCodec.decodeBucket(bytes);
        Assert.assertEquals(decodeMap.size(), map.size());

        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k1".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v1");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k2".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v2");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k3".getBytes(StandardCharsets.UTF_8)));
            Assert.assertNull(keyInfo.getExtra());
        }
        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k4".getBytes(StandardCharsets.UTF_8)));
            Assert.assertNull(keyInfo.getExtra());
        }
        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k5".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v5");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new BytesKey("k6".getBytes(StandardCharsets.UTF_8)));
            Assert.assertNull(keyInfo.getExtra());
        }
    }

    private KeyInfo keyInfo(String key, long expireTime, ValueLocation location, String extra) {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setDataType(DataType.string);
        keyInfo.setKey(key.getBytes(StandardCharsets.UTF_8));
        keyInfo.setExpireTime(expireTime);
        keyInfo.setValueLocation(location);
        if (extra != null) {
            keyInfo.setExtra(extra.getBytes(StandardCharsets.UTF_8));
        }
        return keyInfo;
    }
}
