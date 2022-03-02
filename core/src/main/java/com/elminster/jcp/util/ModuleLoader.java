package com.elminster.jcp.util;

import com.elminster.jcp.module.ModuleFunction;
import com.elminster.jcp.ast.statement.function.Function;

import java.util.*;

public class ModuleLoader {
  public static final ModuleLoader INSTANCE = new ModuleLoader();

  private Map<String, List<Function>> cache = new HashMap<>();

  private ModuleLoader() {

  }

  public List<Function> loadModule(String moduleName) {
    List<Function> set = cache.get(moduleName);
    return Optional.ofNullable(set).orElse(Collections.emptyList());
  }

  public void register(ModuleFunction function) {
    String moduleName = function.getModule();
    List<Function> functions = cache.get(moduleName);
    if (null == functions) {
      functions = new LinkedList<>();
      cache.put(moduleName, functions);
    }
    if (!functions.contains(function)) {
      functions.add(function);
    }
  }

//  /**
//   * FIXME: could be not loaded yet.
//   */
//  private void initCache() {
//    ClassLoader cl = this.getClass().getClassLoader();
//    Vector<Class<?>> classes = null;
//    try {
//      classes = (Vector<Class<?>>) ReflectUtil.getFieldValue(cl, "classes");
//    } catch (IllegalAccessException e) {
//      e.printStackTrace();
//    }
//    for (int i = 0; i < classes.size(); i++) {
//      Class<?> clazz = classes.get(i);
//      if (ModuleFunction.class.isAssignableFrom(clazz)) {
//        try {
//          ModuleFunction function = (ModuleFunction) clazz.newInstance();
//          String module = function.getModule();
//          Set<Function> functions = cache.get(module);
//          if (null == functions) {
//            functions = new TreeSet<>();
//            cache.put(module, functions);
//          }
//          if (!functions.contains(function)) {
//            functions.add(function);
//          }
//        } catch (InstantiationException e) {
//          e.printStackTrace();
//        } catch (IllegalAccessException e) {
//          e.printStackTrace();
//        }
//      }
//    }
//  }
}
