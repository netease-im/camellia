package com.netease.nim.camellia.hot.key.server.samples;

import com.netease.nim.camellia.hot.key.server.console.ConsoleResult;
import com.netease.nim.camellia.hot.key.server.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.zk.registry.springboot.CamelliaHotKeyServerZkRegisterBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2023/5/15
 */
@Component
public class MyConsoleService extends ConsoleServiceAdaptor {

    @Autowired(required = false)
    private CamelliaHotKeyServerZkRegisterBoot zkRegisterBoot;

    @Override
    public ConsoleResult online() {
        if (zkRegisterBoot != null) {
            zkRegisterBoot.register();
        }
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        if (zkRegisterBoot != null) {
            zkRegisterBoot.deregister();
        }
        return super.offline();
    }
}
