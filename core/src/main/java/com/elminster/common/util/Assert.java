package com.elminster.common.util;

public class Assert {
  public static void notNull(Object object) {
    notNull(object, "Argument must not be null");
  }

  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new AssertException(message);
    }
  }
}
