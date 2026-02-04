package com.elminster.jcp.compile;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for compile tests with shared utilities.
 */
public abstract class AbstractCompileTest {

    protected final JcpCompiler compiler = new JcpCompiler();

    private static final AtomicInteger testCounter = new AtomicInteger(0);

    protected String uniqueClassName(String base) {
        return base + "_" + testCounter.incrementAndGet();
    }
}
