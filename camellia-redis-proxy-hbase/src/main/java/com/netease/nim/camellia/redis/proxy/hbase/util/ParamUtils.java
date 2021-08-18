package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;

import java.util.Collection;
import java.util.List;

/**
 *
 * Created by caojiajun on 2019/12/31.
 */
public class ParamUtils {

    public static SortingParams sortingParams(byte[][] args) {
        SortingParams sortingParams = new SortingParams();
        boolean limit = false;
        int limitStart = Integer.MIN_VALUE;
        int limitCount = Integer.MIN_VALUE;
        for (byte[] arg : args) {
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.LIMIT.name())) {
                limit = true;
                continue;
            }
            if (limit) {
                if (limitStart == Integer.MIN_VALUE) {
                    limitStart = (int) Utils.bytesToNum(arg);
                    continue;
                } else if (limitCount == Integer.MIN_VALUE) {
                    limitCount = (int) Utils.bytesToNum(arg);
                    limit = false;
                    sortingParams.limit(limitStart, limitCount);
                    continue;
                }
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.ASC.name())) {
                sortingParams.asc();
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.DESC.name())) {
                sortingParams.desc();
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.ALPHA.name())) {
                sortingParams.alpha();
                continue;
            }
            //其他参数涉及对多个key的操作，因此不支持
            throw new IllegalArgumentException(ErrorReply.NOT_SUPPORT.toString());
        }
        return sortingParams;
    }

    public static MultiBulkReply collection2MultiBulkReply(Collection<byte[]> collection) {
        if (collection == null || collection.isEmpty()) return MultiBulkReply.EMPTY;
        Reply[] replies = new Reply[collection.size()];
        int index = 0;
        for (byte[] bytes : collection) {
            replies[index] = new BulkReply(bytes);
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    public static MultiBulkReply geoList2MultiBulkReply(List<GeoCoordinate> geopos) {
        if (geopos == null || geopos.isEmpty()) return MultiBulkReply.EMPTY;
        Reply[] replies = new Reply[geopos.size()];
        int index = 0;
        for (GeoCoordinate geopo : geopos) {
            Reply[] subRelies = new Reply[2];
            subRelies[0] = new BulkReply(Utils.doubleToBytes(geopo.getLongitude()));
            subRelies[1] = new BulkReply(Utils.doubleToBytes(geopo.getLatitude()));
            replies[index] = new MultiBulkReply(subRelies);
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    public static GeoUnit parseGeoUnit(byte[] unit) {
        for (GeoUnit geoUnit : GeoUnit.values()) {
            if (Utils.checkStringIgnoreCase(unit, geoUnit.name())) {
                return geoUnit;
            }
        }
        throw new IllegalArgumentException("unsupported unit provided. please use m, km, ft, mi");
    }

    public static BitPosParams bitposParam(byte[][] args) {
        BitPosParams params;
        if (args.length == 1) {
            long start = Utils.bytesToNum(args[0]);
            params = new BitPosParams(start);
        } else if (args.length == 2) {
            long start = Utils.bytesToNum(args[0]);
            long end = Utils.bytesToNum(args[1]);
            params = new BitPosParams(start, end);
        } else {
            throw Utils.illegalArgumentException();
        }
        return params;
    }

    public static MultiBulkReply tuples2MultiBulkReply(Collection<Tuple> tuples) {
        if (tuples == null || tuples.isEmpty()) return MultiBulkReply.EMPTY;
        Reply[] replies = new Reply[tuples.size() * 2];
        int index = 0;
        for (Tuple tuple : tuples) {
            replies[index] = new BulkReply(tuple.getBinaryElement());
            index ++;
            replies[index] = new BulkReply(Utils.doubleToBytes(tuple.getScore()));
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    public static ZRangeParams parseZRangeParams(byte[][] args) {
        ZRangeParams params = new ZRangeParams();
        if (args == null || args.length == 0) {
            return params;
        } else if (args.length == 1) {
            boolean withScores = Utils.checkStringIgnoreCase(args[0], RedisKeyword.WITHSCORES.name());
            if (!withScores) {
                throw Utils.illegalArgumentException();
            }
            params.withScores = true;
            return params;
        } else if (args.length == 3) {
            boolean limit = Utils.checkStringIgnoreCase(args[0], RedisKeyword.LIMIT.name());
            if (!limit) {
                throw Utils.illegalArgumentException();
            }
            int offset = (int) Utils.bytesToNum(args[1]);
            int count = (int) Utils.bytesToNum(args[2]);
            params.withLimit = true;
            params.offset = offset;
            params.count = count;
            return params;
        } else if (args.length == 4) {
            boolean withScores = Utils.checkStringIgnoreCase(args[0], RedisKeyword.WITHSCORES.name());
            int offset;
            int count;
            if (withScores) {
                boolean limit = Utils.checkStringIgnoreCase(args[1], RedisKeyword.LIMIT.name());
                if (!limit) {
                    throw Utils.illegalArgumentException();
                }
                offset = (int) Utils.bytesToNum(args[2]);
                count = (int) Utils.bytesToNum(args[3]);
            } else {
                boolean limit = Utils.checkStringIgnoreCase(args[0], RedisKeyword.LIMIT.name());
                if (!limit) {
                    throw Utils.illegalArgumentException();
                }
                offset = (int) Utils.bytesToNum(args[1]);
                count = (int) Utils.bytesToNum(args[2]);
                withScores = Utils.checkStringIgnoreCase(args[3], RedisKeyword.WITHSCORES.name());
                if (!withScores) {
                    throw Utils.illegalArgumentException();
                }
            }
            params.withScores = true;
            params.withLimit = true;
            params.offset = offset;
            params.count = count;
            return params;
        }
        throw Utils.illegalArgumentException();
    }

    public static ScanParams parseScanParams(byte[][] args) {
        if (args.length % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        ScanParams scanParams = new ScanParams();
        boolean needMatch = false;
        boolean needCount = false;
        for (byte[] arg : args) {
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.MATCH.name())) {
                needMatch = true;
                continue;
            } else if (Utils.checkStringIgnoreCase(arg, RedisKeyword.COUNT.name())) {
                needCount = true;
                continue;
            }
            if (needCount) {
                scanParams.count((int) Utils.bytesToNum(arg));
                needCount = false;
                continue;
            } else if (needMatch) {
                scanParams.match(arg);
                needMatch = false;
                continue;
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        }
        return scanParams;
    }

    public static GeoRadiusParams parseGeoRadiusParams(byte[][] args) {
        GeoRadiusParams geoRadiusParams = new GeoRadiusParams();
        if (args == null || args.length == 0) return geoRadiusParams;
        GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
        boolean count = false;
        for (byte[] arg : args) {
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.WITHCOORD.name())) {
                param.withCoord();
                geoRadiusParams.withCoord = true;
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.WITHDIST.name())) {
                param.withDist();
                geoRadiusParams.withDist = true;
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.ASC.name())) {
                param.sortAscending();
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.DESC.name())) {
                param.sortDescending();
                continue;
            }
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.COUNT.name())) {
                count = true;
                continue;
            }
            if (count) {
                param.count((int) Utils.bytesToNum(arg));
                count = false;
                continue;
            }
            throw Utils.illegalArgumentException();
        }
        geoRadiusParams.param = param;
        return geoRadiusParams;
    }

    public static MultiBulkReply geoRadiusList(List<GeoRadiusResponse> list, ParamUtils.GeoRadiusParams geoRadiusParams) {
        Reply[] replies = new Reply[list.size()];
        int index = 0;
        for (GeoRadiusResponse georadiu : list) {
            replies[index] = ParamUtils.parseGeoRadiusResponse(georadiu, geoRadiusParams.withCoord, geoRadiusParams.withDist);
        }
        return new MultiBulkReply(replies);
    }

    public static MultiBulkReply parseGeoRadiusResponse(GeoRadiusResponse response, boolean withCoord, boolean withDist) {
        int size = 1;
        if (withCoord) size++;
        if (withDist) size++;
        int index = 0;
        Reply[] replies = new Reply[size];
        replies[index] = new BulkReply(response.getMember());
        index++;
        if (withDist) {
            replies[index] = new BulkReply(Utils.doubleToBytes(response.getDistance()));
            index++;
        }
        if (withCoord) {
            Reply[] subReplies = new Reply[2];
            GeoCoordinate coordinate = response.getCoordinate();
            subReplies[0] = new BulkReply(Utils.doubleToBytes(coordinate.getLongitude()));
            subReplies[1] = new BulkReply(Utils.doubleToBytes(coordinate.getLatitude()));
            replies[index] = new MultiBulkReply(subReplies);
        }
        return new MultiBulkReply(replies);
    }


    public static class ZRangeParams {
        public boolean withScores = false;
        public boolean withLimit = false;
        public Integer offset = null;
        public Integer count = null;
    }

    public static class GeoRadiusParams {
        public GeoRadiusParam param;
        public boolean withCoord = false;
        public boolean withDist = false;
    }

}
