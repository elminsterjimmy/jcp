package com.elminster.jcp.module.base.vb;

import com.elminster.jcp.module.base.ContentEquality;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The value buffer is the system data for storing table data.
 *
 * @author jgu
 * @version 1.0
 */
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

  public boolean contentEquals(ValueBuffer other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    if (!Arrays.equals(header, other.header)) {
      return false;
    }
    if (buffer.size() != other.buffer.size()) {
      return false;
    }
    for (int i = 0; i < buffer.size(); i++) {
      Object[] rowA = buffer.get(i);
      Object[] rowB = other.buffer.get(i);
      if (rowA.length != rowB.length) {
        return false;
      }
      for (int j = 0; j < rowA.length; j++) {
        if (!ContentEquality.elementEquals(rowA[j], rowB[j])) {
          return false;
        }
      }
    }
    return true;
  }
}
