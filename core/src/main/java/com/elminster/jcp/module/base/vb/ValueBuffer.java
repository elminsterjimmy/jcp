package com.elminster.jcp.module.base.vb;

import java.util.LinkedList;
import java.util.List;

public class ValueBuffer {

  protected String[] header;
  protected List<Object[]> buffer = new LinkedList<>();

  public void setHeader(String[] header) {
    this.header = header;
  }

  public String[] getHeader() {
    return this.header;
  }

  public Object[] getRow(int idx) {
    return buffer.get(idx);
  }

  public void appendRow(Object[] row) {
    buffer.add(row);
  }

  public void setRow(int idx, Object[] row) {
    buffer.set(idx, row);
  }

  public void set(int row, int col, Object value) {
    buffer.get(row)[col] = value;
  }

  public void removeRow(int idx) {
    buffer.remove(idx);
  }

  public int length() {
    return buffer.size();
  }
}
