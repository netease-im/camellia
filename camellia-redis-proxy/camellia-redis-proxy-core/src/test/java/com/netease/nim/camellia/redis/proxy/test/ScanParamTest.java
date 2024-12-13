package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParam;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParamUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Created by caojiajun on 2024/12/13
 */
public class ScanParamTest {

    @Test
    public void test() {
        KeyMeta keyMeta1 = new KeyMeta(EncodeVersion.version_0, KeyType.string, 0, 0);
        KeyMeta keyMeta2 = new KeyMeta(EncodeVersion.version_0, KeyType.zset, 0, 0);

        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("abc"), keyMeta1));
            assertFalse(scanParam.match(toBytes("abcd"), keyMeta1));
            assertFalse(scanParam.match(toBytes("123abc"), keyMeta1));
        }

        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("a*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("a1"), keyMeta1));
            assertFalse(scanParam.match(toBytes("b1"), keyMeta1));
            assertFalse(scanParam.match(toBytes("a1"), keyMeta2));
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("*abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("abc"), keyMeta1));
            assertTrue(scanParam.match(toBytes("111abc"), keyMeta1));
            assertFalse(scanParam.match(toBytes("111a1bc"), keyMeta1));
            assertFalse(scanParam.match(toBytes("abc"), keyMeta2));
            assertFalse(scanParam.match(toBytes("111abc"), keyMeta2));
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("*ab*cd*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("abcd"), keyMeta1));
            assertTrue(scanParam.match(toBytes("ab111cd"), keyMeta1));
            assertTrue(scanParam.match(toBytes("22abcd"), keyMeta1));
            assertTrue(scanParam.match(toBytes("abcd33"), keyMeta1));
            assertTrue(scanParam.match(toBytes("11ab22cd33"), keyMeta1));
            assertFalse(scanParam.match(toBytes("11ab22dc33"), keyMeta1));
            assertTrue(scanParam.match(toBytes("11ab22dc33cd"), keyMeta1));
            //
            assertFalse(scanParam.match(toBytes("abcd"), keyMeta2));
            assertFalse(scanParam.match(toBytes("ab111cd"), keyMeta2));
            assertFalse(scanParam.match(toBytes("22abcd"), keyMeta2));
            assertFalse(scanParam.match(toBytes("abcd33"), keyMeta2));
            assertFalse(scanParam.match(toBytes("11ab22cd33"), keyMeta2));
            assertFalse(scanParam.match(toBytes("11ab22dc33"), keyMeta2));
            assertFalse(scanParam.match(toBytes("11ab22dc33cd"), keyMeta2));
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("{hashtag}abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("{hashtag}abc"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hashtag}abcd"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hashtagabc"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hash}abc"), keyMeta1));
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("{hashtag}*abc*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertTrue(scanParam.match(toBytes("{hashtag}abc"), keyMeta1));
            assertTrue(scanParam.match(toBytes("{hashtag}12abc34"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hashtag}12ab2c34"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hashtag22}12ab2c34"), keyMeta1));
            assertFalse(scanParam.match(toBytes("{hashta2ab2c34"), keyMeta1));
        }
    }

    private static byte[] toBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
