package com.elminster.jcp.eval.function;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DictionaryAutoConvertFixture {

    public static Map<String, Integer> getScores() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        return scores;
    }

    public static Map<Integer, String> getIndexedNames() {
        Map<Integer, String> names = new HashMap<>();
        names.put(1, "first");
        names.put(2, "second");
        return names;
    }

    public static int sumValues(Map<String, Integer> m) {
        int sum = 0;
        for (Integer v : m.values()) {
            sum += v;
        }
        return sum;
    }

    public static Map<String, Integer> emptyMap() {
        return new HashMap<>();
    }

    public static Map<String, Integer> nullMap() {
        return null;
    }
}
