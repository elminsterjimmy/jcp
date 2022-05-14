package com.elminster.jcp.module.base;

import java.util.ArrayList;
import java.util.List;

public class BaseModuleRegister {

    public static List<Class<?>> classToRegister() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(Assertions.class);
        classes.add(Logger.class);
        classes.add(ValueBuffer.class);

        return classes;
    }
}
