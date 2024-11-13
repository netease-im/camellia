package com.netease.nim.camellia.tests.redis.proxy.kv;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParam;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils.ScanParamUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Created by caojiajun on 2024/8/22
 */
public class TestScanParam {

    public static void main(String[] args) {

        KeyMeta keyMeta1 = new KeyMeta(EncodeVersion.version_0, KeyType.string, 0, 0);
        KeyMeta keyMeta2 = new KeyMeta(EncodeVersion.version_0, KeyType.zset, 0, 0);

        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("abc"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("abcd"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("123abc"), keyMeta1), false);
        }

        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("a*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("a1"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("b1"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("a1"), keyMeta2), false);
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("*abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("abc"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("111abc"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("111a1bc"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("abc"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("111abc"), keyMeta2), false);
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("*ab*cd*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("abcd"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("ab111cd"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("22abcd"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("abcd33"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("11ab22cd33"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("11ab22dc33"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("11ab22dc33cd"), keyMeta1), true);
            //
            assertEquals(scanParam.match(toBytes("abcd"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("ab111cd"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("22abcd"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("abcd33"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("11ab22cd33"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("11ab22dc33"), keyMeta2), false);
            assertEquals(scanParam.match(toBytes("11ab22dc33cd"), keyMeta2), false);
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("{hashtag}abc"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("{hashtag}abc"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("{hashtag}abcd"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("{hashtagabc"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("{hash}abc"), keyMeta1), false);
        }
        {
            byte[][] cmd = {RedisCommand.SCAN.raw(), toBytes("0"), toBytes("MATCH"), toBytes("{hashtag}*abc*"), toBytes("type"), toBytes("string")};
            ScanParam scanParam = new ScanParam();
            ScanParamUtil.parseScanParam(scanParam, cmd);
            assertEquals(scanParam.match(toBytes("{hashtag}abc"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("{hashtag}12abc34"), keyMeta1), true);
            assertEquals(scanParam.match(toBytes("{hashtag}12ab2c34"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("{hashtag22}12ab2c34"), keyMeta1), false);
            assertEquals(scanParam.match(toBytes("{hashta2ab2c34"), keyMeta1), false);
        }
    }

    private static byte[] toBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}