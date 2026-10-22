package com.elminster.jcp.module.base.dict;

import com.elminster.jcp.module.base.ContentEquality;
import com.elminster.jcp.module.base.vb.ValueBuffer;

import java.util.HashMap;

public class Dictionary {

    private final HashMap<Object, Object> map;

    public Dictionary() {
        this.map = new HashMap<>();
    }

    public Dictionary(Dictionary source) {
        this.map = new HashMap<>();
        ValueBuffer entries = source.entries();
        for (int i = 0; i < entries.length(); i++) {
            Object[] row = entries.getRow(i);
            this.map.put(row[0], row[1]);
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
            vb.appendRow(new Object[]{key});
        }
        return vb;
    }

    public ValueBuffer values() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key", "value"});
        for (Object key : map.keySet()) {
            vb.appendRow(new Object[]{key, map.get(key)});
        }
        return vb;
    }

    public ValueBuffer entries() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key", "value"});
        for (Object key : map.keySet()) {
            vb.appendRow(new Object[]{key, map.get(key)});
        }
        return vb;
    }

    public boolean contentEquals(Dictionary other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (map.size() != other.map.size()) {
            return false;
        }
        for (Object key : map.keySet()) {
            if (!other.map.containsKey(key)) {
                return false;
            }
            if (!ContentEquality.elementEquals(map.get(key), other.map.get(key))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
