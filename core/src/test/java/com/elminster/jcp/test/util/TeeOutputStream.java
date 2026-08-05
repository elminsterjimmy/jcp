package com.elminster.jcp.test.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Duplicates every write to two underlying streams simultaneously.
 * Used in IO tests to capture output while still forwarding to the real stdout.
 */
public class TeeOutputStream extends OutputStream {

    private final OutputStream primary;
    private final OutputStream secondary;

    public TeeOutputStream(OutputStream primary, OutputStream secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public void write(int b) throws IOException {
        primary.write(b);
        secondary.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        primary.write(b, off, len);
        secondary.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        primary.flush();
        secondary.flush();
    }

    @Override
    public void close() throws IOException {
        try { primary.close(); } finally { secondary.close(); }
    }
}
