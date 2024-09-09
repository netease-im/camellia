package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by caojiajun on 2024/5/31
 */
public class RedisZSet implements EstimateSizeValue {

    private static final Comparator<BytesKey> rankComparator = (o1, o2) -> BytesUtils.compare(o1.getKey(), o2.getKey());
    private static final Comparator<ZSetTuple> scoreComparator = (o1, o2) -> {
        if (o1.getMember().equals(o2.getMember())) {
            return 0;
        }
        int compare = Double.compare(o1.getScore(), o2.getScore());
        if (compare != 0) {
            return compare;
        }
        return BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey());
    };

    private final ConcurrentSkipListMap<BytesKey, Double> memberMap = new ConcurrentSkipListMap<>(rankComparator);
    private final ConcurrentSkipListSet<ZSetTuple> scoreSet = new ConcurrentSkipListSet<>(scoreComparator);

    private long estimateSize = 0;

    public RedisZSet(Map<BytesKey, Double> memberMap) {
        List<ZSetTuple> list = new ArrayList<>(memberMap.size());
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            ZSetTuple zSetTuple = new ZSetTuple(entry.getKey(), entry.getValue());
            list.add(zSetTuple);
            estimateSize += entry.getKey().getKey().length;
            estimateSize += 8;
        }
        this.memberMap.putAll(memberMap);
        this.scoreSet.addAll(list);
    }

    public RedisZSet duplicate() {
        return new RedisZSet(new HashMap<>(memberMap));
    }

    public Map<BytesKey, Double> zadd(Map<BytesKey, Double> map) {
        Map<BytesKey, Double> existsMap = new HashMap<>();
        for (Map.Entry<BytesKey, Double> entry : map.entrySet()) {
            Double put = memberMap.put(entry.getKey(), entry.getValue());
            if (put != null) {
                existsMap.put(entry.getKey(), put);
            } else {
                estimateSize += 8;
                estimateSize += entry.getKey().getKey().length;
            }
            ZSetTuple zSetTuple = new ZSetTuple(entry.getKey(), entry.getValue());
            scoreSet.remove(zSetTuple);
            scoreSet.add(zSetTuple);
        }
        return existsMap;
    }

    public List<ZSetTuple> zrange(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return Collections.emptyList();
        }
        start = rank.getStart();
        stop = rank.getStop();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            if (count >= start) {
                result.add(new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            if (count >= stop) {
                return result;
            }
            count++;
        }
        return result;
    }

    public List<ZSetTuple> zrevrange(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return Collections.emptyList();
        }
        start = rank.getStart();
        stop = rank.getStop();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.descendingMap().entrySet()) {
            if (count >= start) {
                result.add(new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            if (count >= stop) {
                return result;
            }
            count++;
        }
        return result;
    }

    public int zcount(ZSetScore minScore, ZSetScore maxScore) {
        int count = 0;
        for (ZSetTuple member : scoreSet) {
            if (member.getScore() > maxScore.getScore()) {
                break;
            }
            boolean pass = ZSetScoreUtils.checkScore(member.getScore(), minScore, maxScore);
            if (!pass) {
                continue;
            }
            count ++;
        }
        return count;
    }

    public int zlexcount(ZSetLex minLex, ZSetLex maxLex) {
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            boolean pass = ZSetLexUtil.checkLex(entry.getKey().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            count ++;
        }
        return count;
    }

    public List<ZSetTuple> zrangebyscore(ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (ZSetTuple member : scoreSet) {
            if (member.getScore() > maxScore.getScore()) {
                break;
            }
            boolean pass = ZSetScoreUtils.checkScore(member.getScore(), minScore, maxScore);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                break;
            }
            count ++;
        }
        return result;
    }

    public List<ZSetTuple> zrevrangeByScore(ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        Iterator<ZSetTuple> iterator = scoreSet.descendingIterator();
        while (iterator.hasNext()) {
            ZSetTuple member = iterator.next();
            if (member.getScore() < minScore.getScore()) {
                break;
            }
            boolean pass = ZSetScoreUtils.checkScore(member.getScore(), minScore, maxScore);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                break;
            }
            count ++;
        }
        return result;
    }

    public List<ZSetTuple> zrangeByLex(ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            boolean pass = ZSetLexUtil.checkLex(entry.getKey().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                break;
            }
            count++;
        }
        return result;
    }

    public List<ZSetTuple> zrevrangeByLex(ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.descendingMap().entrySet()) {
            boolean pass = ZSetLexUtil.checkLex(entry.getKey().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                break;
            }
            count++;
        }
        return result;
    }

    public Double zscore(BytesKey member) {
        return memberMap.get(member);
    }

    public Pair<Integer, ZSetTuple> zrank(BytesKey member) {
        Double v = memberMap.get(member);
        if (v == null) {
            return null;
        }
        int i=0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            if (entry.getKey().equals(member)) {
                return new Pair<>(i, new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            i ++;
        }
        return null;
    }

    public Pair<Integer, ZSetTuple> zrevrank(BytesKey member) {
        Double v = memberMap.get(member);
        if (v == null) {
            return null;
        }
        int i=0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.descendingMap().entrySet()) {
            if (entry.getKey().equals(member)) {
                return new Pair<>(i, new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            i++;
        }
        return null;
    }


    public int zcard() {
        return memberMap.size();
    }

    public Map<BytesKey, Double> zrem(Collection<BytesKey> members) {
        Map<BytesKey, Double> map = new HashMap<>();
        for (BytesKey bytesKey : members) {
            Double remove = memberMap.remove(bytesKey);
            if (remove != null) {
                map.put(bytesKey, remove);
                //
                ZSetTuple tuple = new ZSetTuple(bytesKey, remove);
                scoreSet.remove(tuple);
                //
                estimateSize -= bytesKey.getKey().length;
                estimateSize -= 8;
            }
        }
        return map;
    }

    public List<Double> zmscore(List<BytesKey> members) {
        List<Double> scores = new ArrayList<>(members.size());
        for (BytesKey member : members) {
            scores.add(memberMap.get(member));
        }
        return scores;
    }

    public Map<BytesKey, Double> zremrangeByRank(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return new HashMap<>();
        }
        start = rank.getStart();
        stop = rank.getStop();
        Map<BytesKey, Double> map = new HashMap<>();
        List<ZSetTuple> removed = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            if (count >= start) {
                map.put(entry.getKey(), entry.getValue());
                removed.add(new ZSetTuple(entry.getKey(), entry.getValue()));
            }
            if (count >= stop) {
                break;
            }
            count++;
        }
        remove(removed);
        return map;
    }

    public Map<BytesKey, Double> zremrangeByScore(ZSetScore minScore, ZSetScore maxScore) {
        Map<BytesKey, Double> map = new HashMap<>();
        List<ZSetTuple> removed = new ArrayList<>();
        for (ZSetTuple member : scoreSet) {
            if (member.getScore() > maxScore.getScore()) {
                break;
            }
            boolean pass = ZSetScoreUtils.checkScore(member.getScore(), minScore, maxScore);
            if (!pass) {
                continue;
            }
            map.put(member.getMember(), member.getScore());
            removed.add(member);
        }
        remove(removed);
        return map;
    }

    public Map<BytesKey, Double> zremrangeByLex(ZSetLex minLex, ZSetLex maxLex) {
        Map<BytesKey, Double> map = new HashMap<>();
        List<ZSetTuple> removed = new ArrayList<>();
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            boolean pass = ZSetLexUtil.checkLex(entry.getKey().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            map.put(entry.getKey(), entry.getValue());
            removed.add(new ZSetTuple(entry.getKey(), entry.getValue()));
        }
        remove(removed);
        return map;
    }

    private void remove(List<ZSetTuple> list) {
        for (ZSetTuple tuple : list) {
            memberMap.remove(tuple.getMember());
            scoreSet.remove(tuple);
            //
            estimateSize -= tuple.getMember().getKey().length;
            estimateSize -= 8;
        }
    }

    @Override
    public long estimateSize() {
        return memberMap.size() * 16L + (estimateSize < 0 ? 0 : estimateSize);
    }
}
