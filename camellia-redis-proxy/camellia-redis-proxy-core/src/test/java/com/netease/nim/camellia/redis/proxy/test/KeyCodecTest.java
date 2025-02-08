package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.ValueLocation;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._4k;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyCodecTest {

    @Test
    public void test() {
        Map<Key, KeyInfo> map = new HashMap<>();
        KeyInfo keyInfo1 = keyInfo("k1", 0, null, "v1");
        KeyInfo keyInfo2 = keyInfo("k2", System.currentTimeMillis() + 10000, null, "v2");
        KeyInfo keyInfo3 = keyInfo("k3", System.currentTimeMillis() + 20000, null, null);
        KeyInfo keyInfo4 = keyInfo("k4", 0, new ValueLocation(new BlockLocation(1, 10), (short) 1000), null);
        KeyInfo keyInfo5 = keyInfo("k5", System.currentTimeMillis() + 30000, new ValueLocation(new BlockLocation(2, 30), (short) 2000), "v5");
        KeyInfo keyInfo6 = keyInfo("k6", System.currentTimeMillis() + 40000, new ValueLocation(new BlockLocation(1, 50), (short) 3000), null);

        map.put(new Key(keyInfo1.getKey()), keyInfo1);
        map.put(new Key(keyInfo2.getKey()), keyInfo2);
        map.put(new Key(keyInfo3.getKey()), keyInfo3);
        map.put(new Key(keyInfo4.getKey()), keyInfo4);
        map.put(new Key(keyInfo5.getKey()), keyInfo5);
        map.put(new Key(keyInfo6.getKey()), keyInfo6);

        byte[] bytes = KeyCodec.encodeBucket(map);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(bytes.length, _4k);

        Map<Key, KeyInfo> decodeMap = KeyCodec.decodeBucket(bytes);
        Assert.assertEquals(decodeMap.size(), map.size());

        {
            KeyInfo keyInfo = decodeMap.get(new Key("k1".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v1");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new Key("k2".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v2");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new Key("k3".getBytes(StandardCharsets.UTF_8)));
            Assert.assertNull(keyInfo.getExtra());
        }
        {
            KeyInfo keyInfo = decodeMap.get(new Key("k4".getBytes(StandardCharsets.UTF_8)));
            Assert.assertNull(keyInfo.getExtra());
        }
        {
            KeyInfo keyInfo = decodeMap.get(new Key("k5".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(new String(keyInfo.getExtra(), StandardCharsets.UTF_8), "v5");
        }
        {
            KeyInfo keyInfo = decodeMap.get(new Key("k6".getBytes(StandardCharsets.UTF_8)));
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
