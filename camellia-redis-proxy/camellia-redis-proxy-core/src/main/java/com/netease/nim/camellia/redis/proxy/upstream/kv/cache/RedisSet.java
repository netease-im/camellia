package com.netease.nim.camellia.redis.proxy.upstream.kv.cache;

import com.netease.nim.camellia.redis.proxy.util.ConcurrentHashSet;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * Created by caojiajun on 2024/8/5
 */
public class RedisSet implements EstimateSizeValue {

    private final Set<BytesKey> set;
    private long estimateSize = 0;

    public RedisSet(ConcurrentHashSet<BytesKey> members) {
        this.set = members;
        for (BytesKey member : members) {
            estimateSize += member.getKey().length;
        }
    }

    public RedisSet duplicate() {
        return new RedisSet(new ConcurrentHashSet<>(set));
    }

    public Set<BytesKey> sadd(Set<BytesKey> members) {
        Set<BytesKey> existsMember = new HashSet<>();
        for (BytesKey member : members) {
            boolean add = set.add(member);
            if (!add) {
                existsMember.add(member);
            } else {
                estimateSize += member.getKey().length;
            }
        }
        return existsMember;
    }

    public int scard() {
        return set.size();
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public boolean sismeber(BytesKey bytesKey) {
        return set.contains(bytesKey);
    }

    public Set<BytesKey> smembers() {
        return set;
    }

    public Map<BytesKey, Boolean> smismember(List<BytesKey> bytesKeyList) {
        Map<BytesKey, Boolean> map = new HashMap<>();
        for (BytesKey bytesKey : bytesKeyList) {
            if (set.contains(bytesKey)) {
                map.put(bytesKey, true);
            } else {
                map.put(bytesKey, false);
            }
        }
        return map;
    }

    public Set<BytesKey> srandmember(int count) {
        Set<BytesKey> members = new HashSet<>();
        if (set.isEmpty()) {
            return members;
        }
        int size = 0;
        for (BytesKey next : set) {
            if (next == null) {
                break;
            }
            members.add(next);
            size++;
            if (size >= count) {
                break;
            }
        }
        return members;
    }

    public Set<BytesKey> srem(Collection<BytesKey> members) {
        Set<BytesKey> result = new HashSet<>();
        for (BytesKey member : members) {
            boolean remove = set.remove(member);
            if (remove) {
                result.add(member);
                estimateSize -= member.getKey().length;
            }
        }
        return result;
    }

    public Set<BytesKey> spop(int count) {
        Set<BytesKey> members = new HashSet<>();
        if (set.isEmpty()) {
            return members;
        }
        int size = 0;
        Iterator<BytesKey> iterator = set.iterator();
        while (iterator.hasNext()) {
            BytesKey next = iterator.next();
            if (next == null) {
                break;
            }
            iterator.remove();
            members.add(next);
            size ++;
            estimateSize -= next.getKey().length;
            if (size >= count) {
                break;
            }
        }
        return members;
    }

    @Override
    public long estimateSize() {
        return set.size() * 8L + (estimateSize < 0 ? 0 : estimateSize);
    }
}
