package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * Created by caojiajun on 2024/5/31
 */
public class ZSet {

    private final Map<BytesKey, Double> memberMap;
    private List<Member> rank = new ArrayList<>();
    private List<Member> score = new ArrayList<>();

    public ZSet(Map<BytesKey, Double> memberMap) {
        this.memberMap = memberMap;
        refresh();
    }

    private void refresh() {
        List<Member> list = new ArrayList<>(memberMap.size());
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            list.add(new Member(entry.getKey(), entry.getValue()));
        }

        List<Member> rank = new ArrayList<>(list);
        rank.sort((o1, o2) -> BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey()));
        this.rank = rank;

        List<Member> score = new ArrayList<>(list);
        score.sort((o1, o2) -> {
            int compare = Double.compare(o1.getScore(), o2.getScore());
            if (compare != 0) {
                return compare;
            }
            return BytesUtils.compare(o1.getMember().getKey(), o2.getMember().getKey());
        });
        this.score = score;
    }

    public Map<BytesKey, Double> zadd(Map<BytesKey, Double> map) {
        Map<BytesKey, Double> existsMap = new HashMap<>();
        for (Map.Entry<BytesKey, Double> entry : map.entrySet()) {
            Double put = memberMap.put(entry.getKey(), entry.getValue());
            if (put != null) {
                existsMap.put(entry.getKey(), put);
            }
        }
        refresh();
        return existsMap;
    }

    public List<Member> zrange(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return Collections.emptyList();
        }
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (Member member : this.rank) {
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

    public List<Member> zrevrange(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return Collections.emptyList();
        }
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (int i=this.rank.size() - 1; i>0; i--) {
            Member member = this.rank.get(i);
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

    public List<Member> zrangebyscore(ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (Member member : score) {
            boolean pass = ZSetScoreUtils.checkScore(member.score, minScore, maxScore);
            if (!pass) {
               continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (result.size() >= limit.getCount()) {
                break;
            }
            count ++;
        }
        return result;
    }

    public List<Member> zrevrangeByScore(ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit) {
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (int i=this.score.size() - 1; i>0; i--) {
            Member member = this.score.get(i);
            boolean pass = ZSetScoreUtils.checkScore(member.score, minScore, maxScore);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (result.size() >= limit.getCount()) {
                break;
            }
            count ++;
        }
        return result;
    }

    public List<Member> zrangeByLex(ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (Member member : this.rank) {
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (result.size() >= limit.getCount()) {
                break;
            }
            count++;
        }
        return result;
    }

    public List<Member> zrevrangeByLex(ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        List<Member> result = new ArrayList<>();
        int count = 0;
        for (int i=this.rank.size() - 1; i>0; i--) {
            Member member = this.rank.get(i);
            boolean pass = ZSetLexUtil.checkLex(member.getMember().getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            if (count >= limit.getOffset()) {
                result.add(member);
            }
            if (result.size() >= limit.getCount()) {
                break;
            }
            count++;
        }
        return result;
    }

    public Double zscore(BytesKey member) {
        return memberMap.get(member);
    }

    public int zcard() {
        return memberMap.size();
    }

    public Map<BytesKey, Double> zrem(List<BytesKey> members) {
        Map<BytesKey, Double> map = new HashMap<>();
        for (BytesKey bytesKey : members) {
            Double remove = memberMap.remove(bytesKey);
            if (remove != null) {
                map.put(bytesKey, remove);
            }
        }
        if (!map.isEmpty()) {
            refresh();
        }
        return map;
    }

    public Map<BytesKey, Double> zremrangeByRank(int start, int stop) {
        ZSetRank rank = new ZSetRank(start, stop, memberMap.size());
        if (rank.isEmptyRank()) {
            return new HashMap<>();
        }
        Map<BytesKey, Double> map = new HashMap<>();
        int count = 0;
        for (Member member : this.rank) {
            if (count >= start) {
                map.put(member.member, member.score);
                memberMap.remove(member.member);
            }
            if (count >= stop) {
                break;
            }
            count++;
        }
        if (!map.isEmpty()) {
            refresh();
        }
        return map;
    }

    public Map<BytesKey, Double> zremrangeByScore(ZSetScore minScore, ZSetScore maxScore) {
        Map<BytesKey, Double> map = new HashMap<>();
        for (Member member : score) {
            boolean pass = ZSetScoreUtils.checkScore(member.score, minScore, maxScore);
            if (!pass) {
                continue;
            }
            map.put(member.member, member.score);
            memberMap.remove(member.member);
        }
        if (!map.isEmpty()) {
            refresh();
        }
        return map;
    }

    public Map<BytesKey, Double> zremrangeByLex(ZSetLex minLex, ZSetLex maxLex) {
        Map<BytesKey, Double> map = new HashMap<>();
        for (Member member : rank) {
            boolean pass = ZSetLexUtil.checkLex(member.member.getKey(), minLex, maxLex);
            if (!pass) {
                continue;
            }
            map.put(member.member, member.score);
            memberMap.remove(member.member);
        }
        if (!map.isEmpty()) {
            refresh();
        }
        return map;
    }

    public static class Member {
        private final BytesKey member;
        private final double score;

        public Member(BytesKey member, double score) {
            this.member = member;
            this.score = score;
        }

        public BytesKey getMember() {
            return member;
        }

        public double getScore() {
            return score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Member member = (Member) o;
            return Objects.equals(this.member, member.member);
        }

        @Override
        public int hashCode() {
            return member.hashCode();
        }
    }

}
