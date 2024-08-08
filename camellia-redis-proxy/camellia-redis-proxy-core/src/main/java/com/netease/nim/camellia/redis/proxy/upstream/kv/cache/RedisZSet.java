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

    private final Map<BytesKey, Double> memberMap;

    private boolean ranked;
    private boolean scored;
    private final List<ZSetTuple> rank = new ArrayList<>();
    private final List<ZSetTuple> score = new ArrayList<>();

    public RedisZSet(Map<BytesKey, Double> memberMap) {
        this.memberMap = memberMap;
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
        }
        onChange();
        return existsMap;
    }

    public List<ZSetTuple> zrange(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return Collections.emptyList();
        }
        checkRank();
        start = rank.getStart();
        stop = rank.getStop();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (ZSetTuple member : this.rank) {
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
        checkRank();
        start = rank.getStart();
        stop = rank.getStop();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (int i=this.rank.size() - 1; i>=0; i--) {
            ZSetTuple member = this.rank.get(i);
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
        checkScore();
        int count = 0;
        for (ZSetTuple member : score) {
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
        checkRank();
        int count = 0;
        for (ZSetTuple member : rank) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            count ++;
        }
        return count;
    }

    public List<ZSetTuple> zrangebyscore(ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        checkScore();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (ZSetTuple member : score) {
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
        checkScore();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (int i=this.score.size() - 1; i>=0; i--) {
            ZSetTuple member = this.score.get(i);
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
        checkRank();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (ZSetTuple member : this.rank) {
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
        checkRank();
        List<ZSetTuple> result = new ArrayList<>();
        int count = 0;
        for (int i=this.rank.size() - 1; i>=0; i--) {
            ZSetTuple member = this.rank.get(i);
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
        checkRank();
        for (int i=0; i<rank.size(); i++) {
            ZSetTuple tuple = rank.get(i);
            if (tuple.getMember().equals(member)) {
                return new Pair<>(i, tuple);
            }
        }
        return null;
    }

    public Pair<Integer, ZSetTuple> zrevrank(BytesKey member) {
        Double v = memberMap.get(member);
        if (v == null) {
            return null;
        }
        checkRank();
        for (int i=rank.size()-1; i>=0; i--) {
            ZSetTuple tuple = rank.get(i);
            if (tuple.getMember().equals(member)) {
                return new Pair<>(rank.size() - 1 - i, tuple);
            }
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
            }
        }
        if (!map.isEmpty()) {
            onChange();
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
        checkRank();
        start = rank.getStart();
        stop = rank.getStop();
        Map<BytesKey, Double> map = new HashMap<>();
        int count = 0;
        for (ZSetTuple member : this.rank) {
            if (count >= start) {
                map.put(member.getMember(), member.getScore());
                memberMap.remove(member.getMember());
            }
            if (count >= stop) {
                break;
            }
            count++;
        }
        if (!map.isEmpty()) {
            onChange();
        }
        return map;
    }

    public Map<BytesKey, Double> zremrangeByScore(ZSetScore minScore, ZSetScore maxScore) {
        checkScore();
        Map<BytesKey, Double> map = new HashMap<>();
        for (ZSetTuple member : score) {
            if (member.getScore() > maxScore.getScore()) {
                break;
            }
            boolean pass = ZSetScoreUtils.checkScore(member.getScore(), minScore, maxScore);
            if (!pass) {
                continue;
            }
            map.put(member.getMember(), member.getScore());
            memberMap.remove(member.getMember());
        }
        if (!map.isEmpty()) {
            onChange();
        }
        return map;
    }

    public Map<BytesKey, Double> zremrangeByLex(ZSetLex minLex, ZSetLex maxLex) {
        checkRank();
        Map<BytesKey, Double> map = new HashMap<>();
        for (ZSetTuple member : rank) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            map.put(member.getMember(), member.getScore());
            memberMap.remove(member.getMember());
        }
        if (!map.isEmpty()) {
            onChange();
        }
        return map;
    }

    private void onChange() {
        rank.clear();
        score.clear();
        scored = false;
        ranked = false;
    }

    private void checkRank() {
        if (!ranked) {
            initRank();
        }
    }

    private void checkScore() {
        if (!scored) {
            initScore();
        }
    }

    private void initRank() {
        this.rank.clear();
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            this.rank.add(new ZSetTuple(entry.getKey(), entry.getValue()));
        }
        this.rank.sort((o1, o2) -> BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey()));

        this.ranked = true;
    }

    private void initScore() {
        this.score.clear();
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            this.score.add(new ZSetTuple(entry.getKey(), entry.getValue()));
        }
        this.score.sort((o1, o2) -> {
            int compare = Double.compare(o1.getScore(), o2.getScore());
            if (compare != 0) {
                return compare;
            }
            return BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey());
        });

        this.scored = true;
    }
}
