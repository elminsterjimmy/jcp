package com.elminster.jcp.util;

import com.elminster.jcp.module.ModuleFunction;
import com.elminster.jcp.ast.statement.function.Function;

import java.util.*;

public class ModuleLoader {
  public static final ModuleLoader INSTANCE = new ModuleLoader();

  private Map<String, Set<Function>> moduleFunctions = new HashMap<>();
  private Map<String, Set<Class<?>>> moduleClasses = new HashMap<>();

  private ModuleLoader() {

  }

  public Set<Function> loadModuleFunctions(String moduleName) {
    return Optional.ofNullable(moduleFunctions.get(moduleName)).orElse(Collections.emptySet());
  }

  public Set<Class<?>> loadModuleClasses(String moduleName) {
    return Optional.ofNullable(moduleClasses.get(moduleName)).orElse(Collections.emptySet());
  }

  public synchronized void registerModuleClass(Class<?> clazz, String moduleName) {
    Set<Class<?>> moduleClass = moduleClasses.get(moduleName);
    if (null == moduleClass) {
      moduleClass = new HashSet<>();
      moduleClasses.put(moduleName, moduleClass);
    }
    moduleClass.add(clazz);
  }

  public synchronized void registerModuleFunction(ModuleFunction function) {
    String moduleName = function.getModule();
    Set<Function> functions = moduleFunctions.get(moduleName);
    if (null == functions) {
      functions = new HashSet<>();
      moduleFunctions.put(moduleName, functions);
    }
    functions.add(function);
  }
}
