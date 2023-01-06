package com.netease.nim.camellia.hbase.springboot;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ReloadableLocalFileCamelliaApi;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.tools.utils.FileUtil;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.hbase.CamelliaHBaseEnv;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.conf.CamelliaHBaseConf;
import com.netease.nim.camellia.hbase.connection.CamelliaHBaseConnectionFactory;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.CamelliaHBaseInitUtil;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/3/23.
 */
@Configuration
@EnableConfigurationProperties({CamelliaHBaseProperties.class})
public class CamelliaHBaseConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = {ProxyEnv.class})
    public ProxyEnv proxyEnv() {
        return ProxyEnv.defaultProxyEnv();
    }

    @Bean
    public CamelliaHBaseTemplate hBaseTemplate(CamelliaHBaseProperties properties) {
        CamelliaHBaseProperties.Type type = properties.getType();
        if (type == CamelliaHBaseProperties.Type.LOCAL) {
            CamelliaHBaseProperties.Local local = properties.getLocal();
            CamelliaHBaseProperties.Local.ConfType confType = local.getConfType();
            if (confType == CamelliaHBaseProperties.Local.ConfType.XML) {
                CamelliaHBaseProperties.Local.XML xml = local.getXml();
                String xmlFile = xml.getXmlFile();
                return new CamelliaHBaseTemplate(xmlFile);
            } else if (confType == CamelliaHBaseProperties.Local.ConfType.YML) {
                CamelliaHBaseProperties.Local.YML yml = local.getYml();
                CamelliaHBaseProperties.Local.YML.Type subType = yml.getType();
                Map<String, String> conf = yml.getConf();
                CamelliaHBaseConf camelliaHBaseConf = new CamelliaHBaseConf();
                for (Map.Entry<String, String> entry : conf.entrySet()) {
                    camelliaHBaseConf.addConf(entry.getKey(), entry.getValue());
                }
                CamelliaHBaseEnv env = new CamelliaHBaseEnv.Builder()
                        .connectionFactory(new CamelliaHBaseConnectionFactory.DefaultHBaseConnectionFactory(camelliaHBaseConf))
                        .proxyEnv(proxyEnv())
                        .build();
                ResourceTable resourceTable;
                if (subType == CamelliaHBaseProperties.Local.YML.Type.SIMPLE) {
                    HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(yml.getResource()));
                    resourceTable = ResourceTableUtil.simpleTable(hBaseResource);
                    HBaseResourceUtil.checkResourceTable(resourceTable);
                    return new CamelliaHBaseTemplate(env, resourceTable);
                } else if (subType == CamelliaHBaseProperties.Local.YML.Type.COMPLEX) {
                    String jsonFile = yml.getJsonFile();
                    if (jsonFile == null) {
                        throw new IllegalArgumentException("missing jsonFile");
                    }
                    String fileContent = FileUtil.readFileByName(jsonFile);
                    if (fileContent == null) {
                        throw new IllegalArgumentException(jsonFile + " read fail");
                    }
                    resourceTable = ReadableResourceTableUtil.parseTable(fileContent);
                    HBaseResourceUtil.checkResourceTable(resourceTable);
                    if (!yml.isDynamic()) {
                        return new CamelliaHBaseTemplate(env, resourceTable);
                    } else {
                        String filePath = FileUtil.getAbsoluteFilePath(jsonFile);
                        if (filePath == null) {
                            return new CamelliaHBaseTemplate(env, resourceTable);
                        }
                        long checkIntervalMillis = yml.getCheckIntervalMillis();
                        if (checkIntervalMillis <= 0) {
                            throw new IllegalArgumentException("checkIntervalMillis <= 0");
                        }
                        ReloadableLocalFileCamelliaApi camelliaApi = new ReloadableLocalFileCamelliaApi(filePath, HBaseResourceUtil.HBaseResourceTableChecker);
                        return new CamelliaHBaseTemplate(env, camelliaApi, checkIntervalMillis);
                    }
                } else {
                    throw new IllegalArgumentException("only support simple/complex");
                }

            } else {
                throw new IllegalArgumentException("only support xml/yml");
            }
        } else if (type == CamelliaHBaseProperties.Type.REMOTE) {
            CamelliaHBaseProperties.Remote remote = properties.getRemote();
            CamelliaApi camelliaApi = CamelliaApiUtil.init(remote.getUrl(), remote.getConnectTimeoutMillis(), remote.getReadTimeoutMillis(), remote.getHeaderMap());
            CamelliaHBaseProperties.Remote.HBaseConf hBaseConf = remote.gethBaseConf();
            CamelliaHBaseProperties.Remote.HBaseConf.ConfType confType = hBaseConf.getConfType();
            CamelliaHBaseEnv env;
            if (confType == CamelliaHBaseProperties.Remote.HBaseConf.ConfType.YML) {
                CamelliaHBaseProperties.Remote.HBaseConf.YML yml = hBaseConf.getYml();
                Map<String, String> conf = yml.getConf();
                CamelliaHBaseConf camelliaHBaseConf = new CamelliaHBaseConf();
                for (Map.Entry<String, String> entry : conf.entrySet()) {
                    camelliaHBaseConf.addConf(entry.getKey(), entry.getValue());
                }
                env = new CamelliaHBaseEnv.Builder()
                        .connectionFactory(new CamelliaHBaseConnectionFactory.DefaultHBaseConnectionFactory(camelliaHBaseConf))
                        .proxyEnv(proxyEnv())
                        .build();
            } else if (confType == CamelliaHBaseProperties.Remote.HBaseConf.ConfType.XML) {
                CamelliaHBaseProperties.Remote.HBaseConf.XML xml = hBaseConf.getXml();
                String xmlFile = xml.getXmlFile();
                env = CamelliaHBaseInitUtil.initHBaseEnvFromHBaseFile(xmlFile);
            } else {
                throw new IllegalArgumentException("only support xml/yml");
            }
            return new CamelliaHBaseTemplate(env, camelliaApi, remote.getBid(), remote.getBgroup(), remote.isMonitor(), remote.getCheckIntervalMillis());
        } else {
            throw new IllegalArgumentException("only support local/remote");
        }
    }
}
