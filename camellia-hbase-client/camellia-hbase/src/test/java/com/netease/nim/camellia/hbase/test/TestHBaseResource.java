package com.netease.nim.camellia.hbase.test;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by caojiajun on 2024/12/23
 */
public class TestHBaseResource {

    @Test
    public void test1() {
        String url = "hbase://nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org/hbase1";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals(resource.getZk(), "nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org");
        Assert.assertEquals(resource.getZkParent(), "/hbase1");
        Assert.assertFalse(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());
        Assert.assertEquals(url, resource.getUrl());
    }

    @Test
    public void testObkv() {
        String url = "hbase://obkv%http://10.1.1.1:8080/services?Action=ObRootServiceInfo&ObRegion=nimtestob&database=obkv%obkvFullUserName=kvtest@testkv#nimtestob&obkvPassword=abcdef&obkvSysUserName=root&obkvSysPassword=111234abc";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertNull(resource.getZk());
        Assert.assertNull(resource.getZkParent());
        Assert.assertFalse(resource.isLindorm());
        Assert.assertTrue(resource.isObkv());
        Assert.assertEquals(resource.getObkvParamUrl(), "http://10.1.1.1:8080/services?Action=ObRootServiceInfo&ObRegion=nimtestob&database=obkv");
        Assert.assertEquals(resource.getObkvFullUserName(), "kvtest@testkv#nimtestob");
        Assert.assertEquals(resource.getObkvPassword(), "abcdef");
        Assert.assertEquals(resource.getObkvSysUserName(), "root");
        Assert.assertEquals(resource.getObkvSysPassword(), "111234abc");

        Assert.assertEquals(url, resource.getUrl());
    }

    @Test
    public void testLindorm() {
        String url = "hbase://ld-xxxx-lindorm.lindorm.rds.aliyuncs.com:30020/?userName=nim_lindorm_1&password=xxabc&lindorm=true";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals(resource.getZk(), "ld-xxxx-lindorm.lindorm.rds.aliyuncs.com:30020");
        Assert.assertEquals(resource.getZkParent(), "/");
        Assert.assertTrue(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());
        Assert.assertEquals(resource.getUserName(), "nim_lindorm_1");
        Assert.assertEquals(resource.getPassword(), "xxabc");

        Assert.assertEquals(url, resource.getUrl());

    }
}
