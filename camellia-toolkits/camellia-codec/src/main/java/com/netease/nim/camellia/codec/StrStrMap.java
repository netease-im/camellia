package com.netease.nim.camellia.codec;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StrStrMap implements Marshallable {
    public Map<String, String> m_map = new HashMap<String, String>();

    public static StrStrMap unpack(Unpack p) {
        StrStrMap map = new StrStrMap();
        map.unmarshal(p);
        return map;
    }

    public void marshal(Pack p) {
        p.putVarUint(m_map.size());
        for (Iterator<String> it = iterator(); it.hasNext();) {
            String str = it.next();
            p.putVarstr(str);
            p.putVarstr(get(str));
        }
    }

    public void unmarshal(Unpack p) {
        int cnt = p.popVarUint();
        for (int i = 0; i < cnt; ++i) {
            String str = p.popVarstr();
            put(str, p.popVarstr());
        }
    }

    public boolean equals(StrStrMap smap) {
        return this.m_map.equals(smap.m_map);
    }

    public Iterator<String> iterator() {
        return m_map.keySet().iterator();
    }

    public String get(String str) {
        return m_map.get(str);
    }

    public void put(String str, String value) {
        m_map.put(str, value == null ? "" : value);
    }

    public int size() {
        return m_map.size();
    }
}

