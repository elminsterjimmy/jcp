package com.elminster.jcp.ast.data;

import java.util.List;

public class ValueBuffer {

  protected String[] header;
  protected List<Object[]> buffer;

  public void setHeader(String[] header) {
    this.header = header;
  }

  public Object[] get(int idx) {
    return buffer.get(idx);
  }

  public void append(Object[] row) {
    buffer.add(row);
  }

  public void set(int idx, Object[] row) {
    buffer.set(idx, row);
  }

  public void remove(int idx) {
    buffer.remove(idx);
  }

  public int length() {
    return buffer.size();
  }
}
