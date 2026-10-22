package com.elminster.jcp.module.base.dict;

import com.elminster.jcp.module.base.vb.ValueBuffer;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {

    private final HashMap<Object, Object> map;

    public Dictionary() {
        this.map = new HashMap<>();
    }

    public Dictionary(Map<?, ?> source) {
        this.map = new HashMap<>(source);
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
        vb.setHeader(new String[]{"value"});
        for (Object value : map.values()) {
            vb.appendRow(new Object[]{value});
        }
        return vb;
    }

    public ValueBuffer entries() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"key", "value"});
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            vb.appendRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        return vb;
    }

    public Map<Object, Object> toMap() {
        return new HashMap<>(map);
    }
}
