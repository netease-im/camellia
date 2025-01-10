package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public class RedisResourceUtil {

    public static final ResourceTableChecker RedisResourceTableChecker = resourceTable -> {
        try {
            checkResourceTable(resourceTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    };

    public static void checkResourceTable(ResourceTable resourceTable) {
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource redisResource : allResources) {
            RedisResourceUtil.parseResourceByUrl(redisResource);
        }
    }

    public static Resource parseResourceByUrl(Resource resource) {
        try {
            if (resource == null) return null;
            String url = resource.getUrl();
            if (url == null) {
                throw new CamelliaRedisException("url is null");
            }
            RedisType redisType = RedisType.parseRedisType(resource);
            if (redisType == RedisType.Redis || redisType == RedisType.Rediss) {
                String substring = url.substring(redisType.getPrefix().length());

                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }

                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                int db = 0;
                String split = substring.substring(index + 1);
                String hostAndPortString;
                if (split.contains("?")) {
                    String[] strings = split.split("\\?");
                    if (strings.length == 2) {
                        Map<String, String> params = getParams(strings[1]);
                        String dbStr = params.get("db");
                        if (dbStr != null) {
                            db = Integer.parseInt(dbStr);
                        }
                    }
                    hostAndPortString = split.split("\\?")[0];
                } else {
                    hostAndPortString = split;
                }
                String[] split2 = hostAndPortString.split(":");
                String host = split2[0];
                int port = Integer.parseInt(split2[1]);
                if (redisType == RedisType.Rediss) {
                    return new RedissResource(host, port, userName, password, db);
                } else {
                    return new RedisResource(host, port, userName, password, db);
                }
            } else if (redisType == RedisType.RedisSentinel || redisType == RedisType.RedissSentinel) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }
                if (!substring.contains("/")) {
                    throw new CamelliaRedisException("missing /");
                }

                int index = substring.lastIndexOf("@");

                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                int index2 = split.indexOf("/");
                String hostPorts = split.substring(0, index2);
                String masterWithParams = split.substring(index2 + 1);

                String[] split2 = hostPorts.split(",");
                List<RedisSentinelResource.Node> nodeList = new ArrayList<>();
                for (String node : split2) {
                    String[] split3 = node.split(":");
                    String host = split3[0];
                    int port = Integer.parseInt(split3[1]);
                    nodeList.add(new RedisSentinelResource.Node(host, port));
                }

                String master;
                int db = 0;
                String sentinelUserName = null;
                String sentinelPassword = null;
                boolean sentinelSSL = false;
                if (masterWithParams.contains("?")) {
                    int i = masterWithParams.indexOf("?");
                    master = masterWithParams.substring(0, i);
                    String queryString = masterWithParams.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String dbStr = params.get("db");
                    if (dbStr != null) {
                        db = Integer.parseInt(dbStr);
                    }
                    String sentinelSSLStr = params.get("sentinelSSL");
                    if (sentinelSSLStr != null && sentinelSSLStr.trim().length() > 0) {
                        if (!sentinelSSLStr.equals("true") && !sentinelSSLStr.equals("false")) {
                            throw new CamelliaRedisException("sentinelSSL only support true/false");
                        }
                        sentinelSSL = Boolean.parseBoolean(sentinelSSLStr);
                    }
                    sentinelUserName = params.get("sentinelUserName");
                    sentinelPassword = params.get("sentinelPassword");
                } else {
                    master = masterWithParams;
                }
                if (redisType == RedisType.RedissSentinel) {
                    return new RedissSentinelResource(master, nodeList, userName, password, db, sentinelUserName, sentinelPassword, sentinelSSL);
                } else {
                    return new RedisSentinelResource(master, nodeList, userName, password, db, sentinelUserName, sentinelPassword, sentinelSSL);
                }
            } else if (redisType == RedisType.RedisCluster || redisType == RedisType.RedissCluster) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }
                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                String[] split2 = split.split(",");
                List<RedisClusterResource.Node> nodeList = new ArrayList<>();
                for (String node : split2) {
                    String[] split1 = node.split(":");
                    String ip = split1[0];
                    int port = Integer.parseInt(split1[1]);
                    nodeList.add(new RedisClusterResource.Node(ip, port));
                }
                if (redisType == RedisType.RedisCluster) {
                    RedisClusterResource redisClusterResource = new RedisClusterResource(nodeList, userName, password);
                    if (!redisClusterResource.getUrl().equals(resource.getUrl())) {
                        throw new CamelliaRedisException("resource url not equals");
                    }
                    return redisClusterResource;
                } else {
                    RedissClusterResource redissClusterResource = new RedissClusterResource(nodeList, userName, password);
                    if (!redissClusterResource.getUrl().equals(resource.getUrl())) {
                        throw new CamelliaRedisException("resource url not equals");
                    }
                    return redissClusterResource;
                }
            } else if (redisType == RedisType.RedisSentinelSlaves || redisType == RedisType.RedissSentinelSlaves) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }
                if (!substring.contains("/")) {
                    throw new CamelliaRedisException("missing /");
                }

                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                int index2 = split.indexOf("/");
                String hostPorts = split.substring(0, index2);
                String masterWithParams = split.substring(index2 + 1);

                String[] split2 = hostPorts.split(",");
                List<RedisSentinelResource.Node> nodeList = new ArrayList<>();
                for (String node : split2) {
                    String[] split3 = node.split(":");
                    String host = split3[0];
                    int port = Integer.parseInt(split3[1]);
                    nodeList.add(new RedisSentinelResource.Node(host, port));
                }
                String master;
                boolean withMaster = false;
                int db = 0;
                String sentinelUserName = null;
                String sentinelPassword = null;
                boolean sentinelSSL = false;
                if (masterWithParams.contains("?")) {
                    int i = masterWithParams.indexOf("?");
                    master = masterWithParams.substring(0, i);
                    String queryString = masterWithParams.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String withMasterStr = params.get("withMaster");
                    if (withMasterStr != null && withMasterStr.trim().length() > 0) {
                        if (!withMasterStr.equals("true") && !withMasterStr.equals("false")) {
                            throw new CamelliaRedisException("withMaster only support true/false");
                        }
                        withMaster = Boolean.parseBoolean(withMasterStr);
                    }
                    String sentinelSSLStr = params.get("sentinelSSL");
                    if (sentinelSSLStr != null && sentinelSSLStr.trim().length() > 0) {
                        if (!sentinelSSLStr.equals("true") && !sentinelSSLStr.equals("false")) {
                            throw new CamelliaRedisException("sentinelSSL only support true/false");
                        }
                        sentinelSSL = Boolean.parseBoolean(sentinelSSLStr);
                    }
                    String dbStr = params.get("db");
                    if (dbStr != null) {
                        db = Integer.parseInt(dbStr);
                    }
                    sentinelUserName = params.get("sentinelUserName");
                    sentinelPassword = params.get("sentinelPassword");
                } else {
                    master = masterWithParams;
                }
                if (redisType == RedisType.RedisSentinelSlaves) {
                    return new RedisSentinelSlavesResource(master, nodeList, userName, password, withMaster, db, sentinelUserName, sentinelPassword, sentinelSSL);
                } else {
                    return new RedissSentinelSlavesResource(master, nodeList, userName, password, withMaster, db, sentinelUserName, sentinelPassword, sentinelSSL);
                }
            } else if (redisType == RedisType.RedisClusterSlaves || redisType == RedisType.RedissClusterSlaves) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }

                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                String hostPortStr;
                boolean withMaster = false;
                if (split.contains("?")) {
                    int i = split.indexOf("?");
                    String queryString = split.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String withMasterStr = params.get("withMaster");
                    if (withMasterStr != null && withMasterStr.trim().length() > 0) {
                        if (!withMasterStr.equals("true") && !withMasterStr.equals("false")) {
                            throw new CamelliaRedisException("withMaster only support true/false");
                        }
                        withMaster = Boolean.parseBoolean(withMasterStr);
                    }
                    hostPortStr = split.substring(0, i);
                } else {
                    hostPortStr = split;
                }
                List<RedisClusterResource.Node> nodeList = new ArrayList<>();
                String[] split2 = hostPortStr.split(",");
                for (String node : split2) {
                    String[] split1 = node.split(":");
                    String ip = split1[0];
                    int port = Integer.parseInt(split1[1]);
                    nodeList.add(new RedisClusterResource.Node(ip, port));
                }
                if (redisType == RedisType.RedisClusterSlaves) {
                    return new RedisClusterSlavesResource(nodeList, userName, password, withMaster);
                } else {
                    return new RedissClusterSlavesResource(nodeList, userName, password, withMaster);
                }
            } else if (redisType == RedisType.RedisProxies || redisType == RedisType.RedissProxies) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }

                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                String nodesStr;
                int db = 0;
                if (split.contains("?")) {
                    int i = split.indexOf("?");
                    String queryString = split.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String dbStr = params.get("db");
                    if (dbStr != null) {
                        db = Integer.parseInt(dbStr);
                    }
                    nodesStr = split.substring(0, i);
                } else {
                    nodesStr = split;
                }

                String[] split2 = nodesStr.split(",");
                List<RedisProxiesResource.Node> nodeList = new ArrayList<>();
                for (String node : split2) {
                    String[] split1 = node.split(":");
                    String ip = split1[0];
                    int port = Integer.parseInt(split1[1]);
                    nodeList.add(new RedisProxiesResource.Node(ip, port));
                }
                if (redisType == RedisType.RedisProxies) {
                    return new RedisProxiesResource(nodeList, userName, password, db);
                } else {
                    return new RedissProxiesResource(nodeList, userName, password, db);
                }
            } else if (redisType == RedisType.RedisProxiesDiscovery || redisType == RedisType.RedissProxiesDiscovery) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }

                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String str = substring.substring(index + 1);

                String proxyName;
                int db = 0;
                if (str.contains("?")) {
                    int i = str.indexOf("?");
                    String queryString = str.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String dbStr = params.get("db");
                    if (dbStr != null) {
                        db = Integer.parseInt(dbStr);
                    }
                    proxyName = str.substring(0, i);
                } else {
                    proxyName = str;
                }
                if (redisType == RedisType.RedisProxiesDiscovery) {
                    return new RedisProxiesDiscoveryResource(userName, password, proxyName, db);
                } else {
                    return new RedissProxiesDiscoveryResource(userName, password, proxyName, db);
                }
            } else if (redisType == RedisType.Sentinel || redisType == RedisType.SSentinel) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }
                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String split = substring.substring(index + 1);

                String[] split2 = split.split(",");
                List<RedisSentinelResource.Node> nodeList = new ArrayList<>();
                for (String node : split2) {
                    String[] split1 = node.split(":");
                    String ip = split1[0];
                    int port = Integer.parseInt(split1[1]);
                    nodeList.add(new RedisSentinelResource.Node(ip, port));
                }
                if (redisType == RedisType.Sentinel) {
                    return new SentinelResource(nodeList, userName, password);
                } else {
                    return new SSentinelResource(nodeList, userName, password);
                }
            } else if (redisType == RedisType.UnixDomainSocket) {
                String substring = url.substring(redisType.getPrefix().length());
                if (!substring.contains("@")) {
                    throw new CamelliaRedisException("missing @");
                }
                int index = substring.lastIndexOf("@");
                String[] userNameAndPassword = getUserNameAndPassword(substring.substring(0, index));
                String userName = userNameAndPassword[0];
                String password = userNameAndPassword[1];

                String str = substring.substring(index + 1);

                String udsPath;
                int db = 0;
                if (str.contains("?")) {
                    int i = str.indexOf("?");
                    udsPath = str.substring(0, i);
                    String queryString = str.substring(i + 1);
                    Map<String, String> params = getParams(queryString);
                    String dbStr = params.get("db");
                    if (dbStr != null) {
                        db = Integer.parseInt(dbStr);
                    }
                } else {
                    udsPath = str;
                }
                return new RedisUnixDomainSocketResource(udsPath, userName, password, db);
            } else if (redisType == RedisType.RedisKV) {
                String namespace = url.substring(redisType.getPrefix().length());
                return new RedisKvResource(namespace);
            } else if (redisType == RedisType.LocalStorage) {
                String dir = url.substring(redisType.getPrefix().length());
                return new RedisLocalStorageResource(dir);
            }
            throw new CamelliaRedisException("not redis resource");
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public static Map<String, String> getParams(String queryString) {
        String[] split1 = queryString.split("&");
        Map<String, String> map = new HashMap<>();
        for (String s : split1) {
            String[] split3 = s.split("=");
            if (split3.length != 2) continue;
            String k = split3[0];
            String v = split3[1];
            map.put(k, v);
        }
        return map;
    }

    public static String[] getUserNameAndPassword(String str) {
        if (str == null) {
            return new String[2];
        }
        if (str.length() == 0) {
            return new String[2];
        }
        int i = str.indexOf(":");
        if (i == -1) {
            return new String[] {null, str};
        }
        String userName = str.substring(0, i);
        String password = str.substring(i+1);
        if (userName.length() == 0) {
            userName = null;
        }
        if (password.length() == 0) {
            password = null;
        }
        return new String[] {userName, password};
    }
}
