package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.Pair;

import java.util.*;

/**
 * Created by caojiajun on 2024/5/31
 */
public class RedisZSet {

    private static final Comparator<ZSetTuple> rankComparator = (o1, o2) -> BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey());
    private static final Comparator<ZSetTuple> scoreComparator = (o1, o2) -> {
        int compare = Double.compare(o1.getScore(), o2.getScore());
        if (compare != 0) {
            return compare;
        }
        return BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey());
    };

    //
    private final Map<BytesKey, Double> memberMap;
    private final TreeSet<ZSetTuple> rankSet = new TreeSet<>(rankComparator);
    private final TreeSet<ZSetTuple> scoreSet = new TreeSet<>(scoreComparator);

    public RedisZSet(Map<BytesKey, Double> memberMap) {
        this.memberMap = memberMap;
        List<ZSetTuple> list = new ArrayList<>(memberMap.size());
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            ZSetTuple zSetTuple = new ZSetTuple(entry.getKey(), entry.getValue());
            list.add(zSetTuple);
        }
        rankSet.addAll(list);
        scoreSet.addAll(list);
    }

    public RedisZSet duplicate() {
        Map<BytesKey, Double> map = new HashMap<>(memberMap);
        return new RedisZSet(map);
    }

    public Map<BytesKey, Double> zadd(Map<BytesKey, Double> map) {
        Map<BytesKey, Double> existsMap = new HashMap<>();
        for (Map.Entry<BytesKey, Double> entry : map.entrySet()) {
            Double put = memberMap.put(entry.getKey(), entry.getValue());
            if (put != null) {
                existsMap.put(entry.getKey(), put);
            }
            ZSetTuple zSetTuple = new ZSetTuple(entry.getKey(), entry.getValue());
            rankSet.add(zSetTuple);
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
        for (ZSetTuple member : rankSet) {
            if (count >= start) {
                result.add(member);
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
        Iterator<ZSetTuple> iterator = rankSet.descendingIterator();
        while (iterator.hasNext()) {
            ZSetTuple member = iterator.next();
            if (count >= start) {
                result.add(member);
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
        for (ZSetTuple member : rankSet) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
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
        for (ZSetTuple member : this.rankSet) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
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
        Iterator<ZSetTuple> iterator = rankSet.descendingIterator();
        while (iterator.hasNext()) {
            ZSetTuple member = iterator.next();
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
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
        for (ZSetTuple tuple : rankSet) {
            if (tuple.getMember().equals(member)) {
                return new Pair<>(i, tuple);
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
        Iterator<ZSetTuple> iterator = rankSet.descendingIterator();
        while (iterator.hasNext()) {
            ZSetTuple tuple = iterator.next();
            if (tuple.getMember().equals(member)) {
                return new Pair<>(i, tuple);
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
                rankSet.remove(tuple);
                scoreSet.remove(tuple);
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
        for (ZSetTuple member : this.rankSet) {
            if (count >= start) {
                map.put(member.getMember(), member.getScore());
                memberMap.remove(member.getMember());
                removed.add(member);
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
            memberMap.remove(member.getMember());
            removed.add(member);
        }
        remove(removed);
        return map;
    }

    public Map<BytesKey, Double> zremrangeByLex(ZSetLex minLex, ZSetLex maxLex) {
        Map<BytesKey, Double> map = new HashMap<>();
        List<ZSetTuple> removed = new ArrayList<>();
        for (ZSetTuple member : rankSet) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            map.put(member.getMember(), member.getScore());
            memberMap.remove(member.getMember());
            removed.add(member);
        }
        remove(removed);
        return map;
    }

    private void remove(List<ZSetTuple> list) {
        for (ZSetTuple tuple : list) {
            rankSet.remove(tuple);
            scoreSet.remove(tuple);
        }
    }
}
