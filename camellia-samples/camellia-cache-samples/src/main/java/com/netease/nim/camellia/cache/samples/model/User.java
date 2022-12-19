package com.netease.nim.camellia.cache.samples.model;

/**
 *
 * Created by caojiajun on 2019/9/19.
 */
public class User {

    private long appid;
    private String accid;
    private int age;

    public User() {
    }

    public User(long appid, String accid, int age) {
        this.appid = appid;
        this.accid = accid;
        this.age = age;
    }

    public long getAppid() {
        return appid;
    }

    public void setAppid(long appid) {
        this.appid = appid;
    }

    public String getAccid() {
        return accid;
    }

    public void setAccid(String accid) {
        this.accid = accid;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "user[appid=" + appid + ",accid=" + accid + ",age=" + age + "]";
    }
}
