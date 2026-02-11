package com.elminster.jcp.compile.debug;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SourceMapping class.
 */
class SourceMappingTest {

  private SourceMapping mapping;

  @BeforeEach
  void setUp() {
    mapping = new SourceMapping();
  }

  @Test
  void registerAndGetClass() {
    mapping.registerClass("com/example/Main", "main.jcp");

    assertEquals("main.jcp", mapping.getFilename("com/example/Main"));
  }

  @Test
  void getFilenameReturnsNullForUnregistered() {
    assertNull(mapping.getFilename("com/example/Unknown"));
  }

  @Test
  void registerAndGetFunction() {
    mapping.registerFunction("com/example/Main", "calculate", "calculate");

    assertEquals("calculate", mapping.getFunctionName("com/example/Main", "calculate"));
  }

  @Test
  void getFunctionReturnsNullForUnregistered() {
    assertNull(mapping.getFunctionName("com/example/Main", "unknown"));
  }

  @Test
  void hasClassReturnsTrueForRegistered() {
    mapping.registerClass("com/example/Main", "main.jcp");

    assertTrue(mapping.hasClass("com/example/Main"));
  }

  @Test
  void hasClassReturnsFalseForUnregistered() {
    assertFalse(mapping.hasClass("com/example/Unknown"));
  }

  @Test
  void clearRemovesAllMappings() {
    mapping.registerClass("com/example/Main", "main.jcp");
    mapping.registerFunction("com/example/Main", "calc", "calc");

    mapping.clear();

    assertNull(mapping.getFilename("com/example/Main"));
    assertNull(mapping.getFunctionName("com/example/Main", "calc"));
    assertFalse(mapping.hasClass("com/example/Main"));
  }

  @Test
  void multipleClassesAndFunctions() {
    mapping.registerClass("com/example/Main", "main.jcp");
    mapping.registerClass("com/example/Utils", "utils.jcp");
    mapping.registerFunction("com/example/Main", "main", "main");
    mapping.registerFunction("com/example/Main", "helper", "helperFunc");
    mapping.registerFunction("com/example/Utils", "format", "formatString");

    assertEquals("main.jcp", mapping.getFilename("com/example/Main"));
    assertEquals("utils.jcp", mapping.getFilename("com/example/Utils"));
    assertEquals("main", mapping.getFunctionName("com/example/Main", "main"));
    assertEquals("helperFunc", mapping.getFunctionName("com/example/Main", "helper"));
    assertEquals("formatString", mapping.getFunctionName("com/example/Utils", "format"));
  }

  @Test
  void overwriteExistingMapping() {
    mapping.registerClass("com/example/Main", "old.jcp");
    mapping.registerClass("com/example/Main", "new.jcp");

    assertEquals("new.jcp", mapping.getFilename("com/example/Main"));
  }
}
