package com.netease.nim.camellia.console.util;

import io.netty.util.internal.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class UrlCheckUtil {
    public static boolean checkUrl(String url){
        if(StringUtil.isNullOrEmpty(url)){
            return false;
        }
        else{
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        checkUrl("http://localhost:880");
    }
}
