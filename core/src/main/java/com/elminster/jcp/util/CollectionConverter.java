package com.elminster.jcp.util;

import com.elminster.jcp.module.base.dict.Dictionary;
import com.elminster.jcp.module.base.vb.ValueBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionConverter {

    public static Dictionary fromMap(Map<?, ?> source) {
        Dictionary dict = new Dictionary();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            dict.put(entry.getKey(), entry.getValue());
        }
        return dict;
    }

    public static Map<Object, Object> toMap(Dictionary dict) {
        Map<Object, Object> result = new HashMap<>();
        ValueBuffer entries = dict.entries();
        for (int i = 0; i < entries.length(); i++) {
            ValueBuffer row = (ValueBuffer) entries.getRow(i)[0];
            result.put(row.getRow(0)[0], row.getRow(0)[1]);
        }
        return result;
    }

    public static ValueBuffer fromList(List<?> list) {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"value"});
        for (Object item : list) {
            vb.appendRow(new Object[]{item});
        }
        return vb;
    }

    public static List<Object> toList(ValueBuffer vb) {
        List<Object> result = new ArrayList<>(vb.length());
        for (int i = 0; i < vb.length(); i++) {
            Object[] row = vb.getRow(i);
            result.add(row.length > 0 ? row[0] : null);
        }
        return result;
    }
}
