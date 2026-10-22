package com.elminster.jcp.module.base.dict;

import com.elminster.jcp.module.base.vb.ValueBuffer;

import java.util.HashMap;
import java.util.Objects;

public class Dictionary {

    private final HashMap<Object, Object> map;

    public Dictionary() {
        this.map = new HashMap<>();
    }

    public Dictionary(Dictionary source) {
        this.map = new HashMap<>();
        ValueBuffer entries = source.entries();
        for (int i = 0; i < entries.length(); i++) {
            ValueBuffer row = (ValueBuffer) entries.getRow(i)[0];
            this.map.put(row.getRow(0)[0], row.getRow(0)[1]);
        }
    }

    public void put(Object key, Object value) {
        map.put(key, value);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public ValueBuffer keys() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key"});
        for (Object key : map.keySet()) {
            ValueBuffer row = new ValueBuffer();
            row.setHeader(new String[]{"key"});
            row.appendRow(new Object[]{key});
            vb.appendRow(new Object[]{row});
        }
        return vb;
    }

    public ValueBuffer values() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key", "value"});
        for (Object key : map.keySet()) {
            ValueBuffer row = new ValueBuffer();
            row.setHeader(new String[]{"key", "value"});
            row.appendRow(new Object[]{key, map.get(key)});
            vb.appendRow(new Object[]{row});
        }
        return vb;
    }

    public ValueBuffer entries() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key", "value"});
        for (Object key : map.keySet()) {
            ValueBuffer row = new ValueBuffer();
            row.setHeader(new String[]{"key", "value"});
            row.appendRow(new Object[]{key, map.get(key)});
            vb.appendRow(new Object[]{row});
        }
        return vb;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dictionary)) return false;
        return map.equals(((Dictionary) o).map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}
