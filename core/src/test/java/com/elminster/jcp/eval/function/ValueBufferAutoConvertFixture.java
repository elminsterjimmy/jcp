package com.elminster.jcp.eval.function;

import java.util.ArrayList;
import java.util.List;

public class ValueBufferAutoConvertFixture {

    public static List<String> getNames() {
        List<String> names = new ArrayList<>();
        names.add("Alice");
        names.add("Bob");
        names.add("Charlie");
        return names;
    }

    public static List<Integer> getNumbers() {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(10);
        numbers.add(20);
        numbers.add(30);
        return numbers;
    }

    public static int sumList(List<Integer> numbers) {
        int sum = 0;
        for (Integer n : numbers) {
            sum += n;
        }
        return sum;
    }

    public static List<String> emptyList() {
        return new ArrayList<>();
    }

    public static List<String> nullList() {
        return null;
    }
}
