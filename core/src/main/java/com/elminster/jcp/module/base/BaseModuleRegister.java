package com.elminster.jcp.module.base;

import com.elminster.jcp.module.base.assertions.Assertions;
import com.elminster.jcp.module.base.logger.Logger;
import com.elminster.jcp.module.base.vb.ValueBuffer;

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
