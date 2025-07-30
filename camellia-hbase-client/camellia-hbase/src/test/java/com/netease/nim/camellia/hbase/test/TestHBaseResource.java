package com.netease.nim.camellia.hbase.test;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by caojiajun on 2024/12/23
 */
public class TestHBaseResource {

    @Test
    public void test1() {
        String url = "hbase://nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org/hbase1";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals("nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org", resource.getZk());
        Assert.assertEquals("/hbase1", resource.getZkParent());
        Assert.assertFalse(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());

        Assert.assertEquals(HBaseResourceUtil.parseResourceByUrl(new Resource(resource.getUrl())).getUrl(), resource.getUrl());
    }

    @Test
    public void testObkv() {
        String url = "hbase://obkv%http://10.1.1.1:8080/services?Action=ObRootServiceInfo&ObRegion=nimtestob&database=obkv%obkvFullUserName=kvtest@testkv#nimtestob&obkvPassword=abcdef&obkvSysUserName=root&obkvSysPassword=111234abc";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertNull(resource.getZk());
        Assert.assertNull(resource.getZkParent());
        Assert.assertFalse(resource.isLindorm());
        Assert.assertTrue(resource.isObkv());
        Assert.assertEquals("http://10.1.1.1:8080/services?Action=ObRootServiceInfo&ObRegion=nimtestob&database=obkv", resource.getObkvParamUrl());
        Assert.assertEquals("kvtest@testkv#nimtestob", resource.getObkvFullUserName());
        Assert.assertEquals("abcdef", resource.getObkvPassword());
        Assert.assertEquals("root", resource.getObkvSysUserName());
        Assert.assertEquals("111234abc", resource.getObkvSysPassword());

        Assert.assertEquals(HBaseResourceUtil.parseResourceByUrl(new Resource(resource.getUrl())).getUrl(), resource.getUrl());
    }

    @Test
    public void testLindorm() {
        String url = "hbase://ld-xxxx-lindorm.lindorm.rds.aliyuncs.com:30020/?userName=nim_lindorm_1&password=xxabc&lindorm=true";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals("ld-xxxx-lindorm.lindorm.rds.aliyuncs.com:30020", resource.getZk());
        Assert.assertEquals("/", resource.getZkParent());
        Assert.assertTrue(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());
        Assert.assertEquals("nim_lindorm_1", resource.getUserName());
        Assert.assertEquals("xxabc", resource.getPassword());

        Assert.assertEquals(HBaseResourceUtil.parseResourceByUrl(new Resource(resource.getUrl())).getUrl(), resource.getUrl());
    }

    @Test
    public void testUserName() {
        String url = "hbase://nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org/?userName=user1&password=passwd1";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals("nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org", resource.getZk());
        Assert.assertEquals("/", resource.getZkParent());
        Assert.assertFalse(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());
        Assert.assertEquals("user1", resource.getUserName());
        Assert.assertEquals("passwd1", resource.getPassword());

        Map<String, String> configMap = resource.getConfigMap();
        Assert.assertEquals(0, configMap.size());

        Assert.assertEquals(HBaseResourceUtil.parseResourceByUrl(new Resource(resource.getUrl())).getUrl(), resource.getUrl());
    }

    @Test
    public void testConfigMap() {
        String url = "hbase://nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org/hbase1?k1=v1&k2=v2&k3=v3";
        HBaseResource resource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        Assert.assertEquals("nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org", resource.getZk());
        Assert.assertEquals("/hbase1", resource.getZkParent());
        Assert.assertFalse(resource.isLindorm());
        Assert.assertFalse(resource.isObkv());
        Map<String, String> configMap = resource.getConfigMap();
        Assert.assertEquals(3, configMap.size());
        Assert.assertEquals("v1", configMap.get("k1"));
        Assert.assertEquals("v2", configMap.get("k2"));
        Assert.assertEquals("v3", configMap.get("k3"));

        Assert.assertEquals(HBaseResourceUtil.parseResourceByUrl(new Resource(resource.getUrl())).getUrl(), resource.getUrl());
    }
}
