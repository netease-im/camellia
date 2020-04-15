package com.netease.nim.camellia.hbase;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.hbase.connection.CamelliaHBaseConnectionFactory;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseEnv {

    private CamelliaHBaseConnectionFactory connectionFactory = CamelliaHBaseConnectionFactory.DEFAULT;
    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();

    private CamelliaHBaseEnv() {
    }

    public CamelliaHBaseEnv(CamelliaHBaseConnectionFactory camelliaHBaseConnectionFactory, ProxyEnv proxyEnv) {
        this.connectionFactory = camelliaHBaseConnectionFactory;
        this.proxyEnv = proxyEnv;
    }

    public static CamelliaHBaseEnv defaultHBaseEnv() {
        return new CamelliaHBaseEnv();
    }

    public CamelliaHBaseConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public ProxyEnv getProxyEnv() {
        return proxyEnv;
    }

    public static class Builder {
        private CamelliaHBaseEnv env;

        public Builder() {
            env = new CamelliaHBaseEnv();
        }

        public Builder(CamelliaHBaseEnv env) {
            this.env = new CamelliaHBaseEnv(env.connectionFactory, env.proxyEnv);
        }

        public Builder connectionFactory(CamelliaHBaseConnectionFactory connectionFactory) {
            if (connectionFactory == null) return this;
            env.connectionFactory = connectionFactory;
            return this;
        }

        public Builder proxyEnv(ProxyEnv proxyEnv) {
            if (proxyEnv == null) return this;
            if (env != null) {
                env.proxyEnv = proxyEnv;
            }
            return this;
        }

        public CamelliaHBaseEnv build() {
            return env;
        }
    }
}
