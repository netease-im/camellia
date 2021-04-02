package com.netease.nim.camellia.hbase.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/3/23.
 */
@ConfigurationProperties(prefix = "camellia-hbase")
public class CamelliaHBaseProperties {

    private Type type = Type.LOCAL;
    private Local local = new Local();
    private Remote remote;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }

    public static class Local {
        private ConfType confType = ConfType.XML;
        private XML xml = new XML();
        private YML yml;

        public ConfType getConfType() {
            return confType;
        }

        public void setConfType(ConfType confType) {
            this.confType = confType;
        }

        public XML getXml() {
            return xml;
        }

        public void setXml(XML xml) {
            this.xml = xml;
        }

        public YML getYml() {
            return yml;
        }

        public void setYml(YML yml) {
            this.yml = yml;
        }

        public static class XML {
            private String xmlFile = "hbase.xml";

            public String getXmlFile() {
                return xmlFile;
            }

            public void setXmlFile(String xmlFile) {
                this.xmlFile = xmlFile;
            }
        }

        public static class YML {
            private CamelliaHBaseProperties.Local.YML.Type type = Type.SIMPLE;
            private String resource;
            private String jsonFile;
            private boolean dynamic;
            private long checkIntervalMillis = 5000;
            private Map<String, String> conf = new HashMap<>();

            public Type getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type;
            }

            public String getJsonFile() {
                return jsonFile;
            }

            public void setJsonFile(String jsonFile) {
                this.jsonFile = jsonFile;
            }

            public String getResource() {
                return resource;
            }

            public void setResource(String resource) {
                this.resource = resource;
            }

            public Map<String, String> getConf() {
                return conf;
            }

            public void setConf(Map<String, String> conf) {
                this.conf = conf;
            }

            public boolean isDynamic() {
                return dynamic;
            }

            public void setDynamic(boolean dynamic) {
                this.dynamic = dynamic;
            }

            public long getCheckIntervalMillis() {
                return checkIntervalMillis;
            }

            public void setCheckIntervalMillis(long checkIntervalMillis) {
                this.checkIntervalMillis = checkIntervalMillis;
            }

            public static enum Type {
                SIMPLE,
                COMPLEX,
                ;
            }
        }

        public static enum ConfType {
            XML,
            YML,
            ;
        }
    }

    public static class Remote {
        private String url;
        private Long bid;
        private String bgroup;
        private boolean monitor = true;
        private long checkIntervalMillis = 5000;
        private int connectTimeoutMillis = 10000;
        private int readTimeoutMillis = 60000;
        private HBaseConf hBaseConf = new HBaseConf();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Long getBid() {
            return bid;
        }

        public void setBid(Long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public boolean isMonitor() {
            return monitor;
        }

        public void setMonitor(boolean monitor) {
            this.monitor = monitor;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public HBaseConf gethBaseConf() {
            return hBaseConf;
        }

        public void sethBaseConf(HBaseConf hBaseConf) {
            this.hBaseConf = hBaseConf;
        }

        public static class HBaseConf {
            private ConfType confType = ConfType.YML;
            private XML xml;
            private YML yml = new YML();

            public ConfType getConfType() {
                return confType;
            }

            public void setConfType(ConfType confType) {
                this.confType = confType;
            }

            public XML getXml() {
                return xml;
            }

            public void setXml(XML xml) {
                this.xml = xml;
            }

            public YML getYml() {
                return yml;
            }

            public void setYml(YML yml) {
                this.yml = yml;
            }

            public static class XML {
                private String xmlFile = "hbase.xml";

                public String getXmlFile() {
                    return xmlFile;
                }

                public void setXmlFile(String xmlFile) {
                    this.xmlFile = xmlFile;
                }
            }
            public static class YML {
                private Map<String, String> conf = new HashMap<>();

                public Map<String, String> getConf() {
                    return conf;
                }

                public void setConf(Map<String, String> conf) {
                    this.conf = conf;
                }
            }

            public static enum ConfType {
                XML,
                YML,
                ;
            }
        }
    }



    public static enum Type {
        LOCAL,
        REMOTE,
        ;
    }
}
