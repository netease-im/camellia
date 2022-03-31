package com.netease.nim.camellia.feign.samples.test;

import com.netease.nim.camellia.core.client.annotation.RouteKey;
import com.netease.nim.camellia.core.util.AnnotationValueGetterCache;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Created by caojiajun on 2022/3/31
 */
public class TestRouteKey {

    private static class Account1 {
        @RouteKey
        long uid1;
        String name1;
    }

    private static class Account2 {
        Account1 account1;
        long uid2;
        String name2;
    }

    private static class Account3 extends Account1 {
        long uid3;
        String name3;
    }

    private static interface TestService {
        int test1(Account1 account1);
        int test2(Account2 account2);
        int test3(Account3 account3);
        int test4(@RouteKey long uid, Account1 account1);
    }

    public static void main(String[] args) {
        AnnotationValueGetterCache cache = new AnnotationValueGetterCache();
        for (Method method : TestService.class.getMethods()) {
            switch (method.getName()) {
                case "test1": {
                    Account1 account1 = new Account1();
                    account1.uid1 = 1;
                    Object value = cache.getAnnotationValueByParameterField(RouteKey.class, method, new Object[]{account1});
                    System.out.println("method=" + method.getName() + ",value=" + value + ",result=" + Objects.equals(value, account1.uid1));
                    break;
                }
                case "test2": {
                    Account2 account2 = new Account2();
                    account2.uid2 = 2;
                    Account1 account1 = new Account1();
                    account1.uid1 = 3;
                    account2.account1 = account1;
                    Object value = cache.getAnnotationValueByParameterField(RouteKey.class, method, new Object[]{account2});
                    System.out.println("method=" + method.getName() + ",value=" + value + ",result=" + Objects.equals(value, account2.account1.uid1));
                    break;
                }
                case "test3": {
                    Account3 account3 = new Account3();
                    account3.uid1 = 4;
                    Object value = cache.getAnnotationValueByParameterField(RouteKey.class, method, new Object[]{account3});
                    System.out.println("method=" + method.getName() + ",value=" + value + ",result=" + Objects.equals(value, account3.uid1));
                    break;
                }
                case "test4": {
                    Object value = cache.getAnnotationValueByParameterField(RouteKey.class, method, new Object[]{5, new Account1()});
                    System.out.println("method=" + method.getName() + ",value=" + value + ",result=" + Objects.equals(value, 5));
                    break;
                }
            }
        }
    }
}
