# JCP Function Architecture

This document describes the function system architecture in JCP, covering both interpreter (eval) and compiler (compile) execution modes.

## Table of Contents

1. [Overview](#overview)
2. [Function Naming Pattern](#function-naming-pattern)
3. [Eval Mode - Interpreter Execution](#eval-mode---interpreter-execution)
4. [Compile Mode - Bytecode Generation](#compile-mode---bytecode-generation)
5. [Type Methods](#type-methods)
6. [Module System](#module-system)
7. [Key Design Decisions](#key-design-decisions)
8. [File Reference](#file-reference)

---

## Overview

JCP implements a **dual-mode function system** that works across both interpreter (eval) and compiler (compile) execution modes. Functions are unified through a consistent naming pattern and lookup mechanism that supports:

- User-defined functions
- Module-provided functions (Java class methods)
- Type methods (instance and static)
- Function overloading with type compatibility checking

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         JCP Function System                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐         ┌─────────────────┐                    │
│  │   Source Code   │         │   Source Code   │                    │
│  │  (JCP Script)   │         │  (JCP Script)   │                    │
│  └────────┬────────┘         └────────┬────────┘                    │
│           │                           │                              │
│           ▼                           ▼                              │
│  ┌─────────────────┐         ┌─────────────────┐                    │
│  │     Parser      │         │     Parser      │                    │
│  │  (Shared AST)   │         │  (Shared AST)   │                    │
│  └────────┬────────┘         └────────┬────────┘                    │
│           │                           │                              │
│           ▼                           ▼                              │
│  ┌─────────────────┐         ┌─────────────────┐                    │
│  │   EVAL MODE     │         │  COMPILE MODE   │                    │
│  │                 │         │                 │                    │
│  │ ┌─────────────┐ │         │ ┌─────────────┐ │                    │
│  │ │EvalContext  │ │         │ │CompileContext│ │                    │
│  │ │  functions: │ │         │ │ functionReg: │ │                    │
│  │ │  Map<name,  │ │         │ │ Map<key,     │ │                    │
│  │ │   Function> │ │         │ │  Signature>  │ │                    │
│  │ └─────────────┘ │         │ └─────────────┘ │                    │
│  │                 │         │                 │                    │
│  │ ┌─────────────┐ │         │ ┌─────────────┐ │                    │
│  │ │FunCall      │ │         │ │FunCall      │ │                    │
│  │ │Evaluator    │ │         │ │Compiler     │ │                    │
│  │ └──────┬──────┘ │         │ └──────┬──────┘ │                    │
│  │        │        │         │        │        │                    │
│  │        ▼        │         │        ▼        │                    │
│  │  Tree-Walking   │         │   INVOKESTATIC  │                    │
│  │   Execution     │         │   Bytecode      │                    │
│  └─────────────────┘         └─────────────────┘                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Function Naming Pattern

**Central utility:** `core/src/main/java/com/elminster/jcp/util/FunctionUtils.java`

### Naming Convention

Functions use a hierarchical naming pattern:

```
Pattern: [module::]type.function#paramType1@paramType2
```

**Examples:**

| JCP Code | Internal Name |
|----------|---------------|
| `Logger.log("hi")` | `base::Logger.log#Object` |
| `counter.get()` | `user::Counter.get#Counter` |
| `Math.add(1, 2)` | `user::Math.add#int@int` |
| `abs(-5)` | `abs#int` |

### Name Components

```
┌──────────┬───────────┬──────────────┬─────────────────────┐
│  Module  │   Type    │  Function    │  Parameter Types    │
│  (opt)   │   (opt)   │   Name       │  (for overloading)  │
├──────────┼───────────┼──────────────┼─────────────────────┤
│  base::  │  Logger.  │  log         │  #Object            │
│  user::  │  Counter. │  get         │  #Counter           │
│  (none)  │  (none)   │  abs         │  #int               │
└──────────┴───────────┴──────────────┴─────────────────────┘
```

### Key Constants

```java
public static final String USER_MODULE = "user";      // Default for user code
public static final String GLOBAL_TYPE = "global";    // Non-type functions
private static final String MODULE_SPLITTER = "::";
private static final String FUNCTION_FULLNAME_PARAMETER_SPLITTER = "@";
private static final String FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER = "#";
```

### Key Methods

| Method | Purpose | Example Output |
|--------|---------|----------------|
| `getModuleFunctionName(module, type, func)` | Qualified name without params | `Logger.log` |
| `generateFunctionFullName(...)` | Full name with param types | `Logger.log#Object` |
| `getGlobalFunctionName(module, func)` | Non-type functions | `base::global::abs` |

---

## Eval Mode - Interpreter Execution

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      EVAL MODE                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐      ┌──────────────────┐                 │
│  │  RootEvalContext │      │  Function        │                 │
│  │                  │      │  Interface       │                 │
│  │  functions: Map  │◄─────│                  │                 │
│  │  variables: Map  │      │  - getId()       │                 │
│  │  dataTypes: Map  │      │  - getFullName() │                 │
│  │  contextStack    │      │  - getParams()   │                 │
│  └────────┬─────────┘      └──────────────────┘                 │
│           │                         ▲                            │
│           │                         │                            │
│           │         ┌───────────────┴───────────────┐           │
│           │         │                               │           │
│           │  ┌──────┴──────┐             ┌─────────┴────────┐   │
│           │  │AbstractFunc │             │AbstractModuleFunc│   │
│           │  │(User Code)  │             │(Java Classes)    │   │
│           │  │             │             │                  │   │
│           │  │ - body      │             │ - doFunction()   │   │
│           │  │ - params    │             │ - reflection     │   │
│           │  └─────────────┘             └──────────────────┘   │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     Evaluators                            │   │
│  ├──────────────────┬───────────────────┬───────────────────┤   │
│  │ FunCallEvaluator │ MethodCallEvaluator│StaticMethodCall  │   │
│  │                  │                    │    Evaluator      │   │
│  │ funcName(args)   │ obj.method(args)   │ Type.method(args)│   │
│  │                  │                    │                   │   │
│  │ Resolves by name │ Prepends 'this'   │ No 'this' param   │   │
│  │ and param types  │ as first arg      │                   │   │
│  └──────────────────┴───────────────────┴───────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│                    ┌──────────────────┐                         │
│                    │FunctionEvaluator │                         │
│                    │                  │                         │
│                    │ - Creates scope  │                         │
│                    │ - Binds params   │                         │
│                    │ - Executes body  │                         │
│                    └──────────────────┘                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Function Registration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  Function Registration Flow                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. System Startup                                              │
│     ┌─────────────────┐                                         │
│     │RootEvalContext  │                                         │
│     │    .init()      │                                         │
│     └────────┬────────┘                                         │
│              │                                                   │
│              ▼                                                   │
│     ┌─────────────────┐                                         │
│     │registerSystem   │                                         │
│     │  Functions()    │                                         │
│     └────────┬────────┘                                         │
│              │                                                   │
│              ▼                                                   │
│     ┌─────────────────┐      ┌─────────────────┐               │
│     │BaseModule       │      │ [Logger,        │               │
│     │ Register       │─────►│  Assertions,    │               │
│     │.classToRegister│      │  ValueBuffer]   │               │
│     └────────┬────────┘      └─────────────────┘               │
│              │                                                   │
│              ▼                                                   │
│     ┌─────────────────────────────────────────┐                 │
│     │ ClassConverter.registerClass(clazz)     │                 │
│     │                                         │                 │
│     │ For each public method:                 │                 │
│     │   - Static → register as-is             │                 │
│     │   - Instance → prepend 'this' param     │                 │
│     │   - Constructor → register as Type.new  │                 │
│     │                                         │                 │
│     │ Wraps in AbstractModuleFunction         │                 │
│     │ (uses reflection to invoke)             │                 │
│     └────────┬────────────────────────────────┘                 │
│              │                                                   │
│              ▼                                                   │
│     ┌─────────────────┐                                         │
│     │context.addFunc  │                                         │
│     │ (fullName,      │                                         │
│     │  function)      │                                         │
│     └─────────────────┘                                         │
│                                                                  │
│  2. User Type Declaration                                       │
│     ┌─────────────────┐                                         │
│     │TypeDeclaration  │                                         │
│     │   Evaluator     │                                         │
│     └────────┬────────┘                                         │
│              │                                                   │
│              ├──► Create StructType                             │
│              │                                                   │
│              ├──► For each instance method:                     │
│              │      - Prepend 'this' to params                  │
│              │      - Create AbstractFunction                   │
│              │      - context.addFunction()                     │
│              │                                                   │
│              └──► For each static method:                       │
│                     - Create AbstractFunction                   │
│                     - context.addFunction()                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Function Lookup Algorithm

```java
// FunCallEvaluator - Overload Resolution
private List<Function> getFunctionCandidates(funcName, moduleName, args, ctx) {
    return ctx.getFunctions().values().stream()
        .filter(f -> hasSameFunctionName(funcName, f))      // Name match
        .filter(f -> hasSameModule(moduleName, f))          // Module match
        .filter(f -> hasSameParameterDefinition(args, f))   // Type compatibility
        .collect(Collectors.toList());
}

// Parameter compatibility check uses DataType.isCastableTo()
// e.g., INT.isCastableTo(NUMERIC) == true
```

### Method Call Flow

```
┌─────────────────────────────────────────────────────────────────┐
│               Method Call: obj.method(args)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. MethodCallEvaluator.eval()                                  │
│     ┌─────────────────────────────────────────┐                 │
│     │ // Evaluate target object               │                 │
│     │ Data data = expressionEval.eval(ctx);   │                 │
│     │                                         │                 │
│     │ // Build arguments with 'this' prepended│                 │
│     │ Data[] params = new Data[args.len + 1]; │                 │
│     │ params[0] = data;  // 'this'            │                 │
│     │ // copy remaining args...               │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  2. Determine module from target type                           │
│     ┌─────────────────────────────────────────┐                 │
│     │ if (data instanceof StructData)         │                 │
│     │     module = "user";                    │                 │
│     │ else if (type instanceof Modulable)     │                 │
│     │     module = type.getModule();          │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  3. Generate function lookup name                               │
│     ┌─────────────────────────────────────────┐                 │
│     │ // Example: counter.get() on Counter    │                 │
│     │ name = "user::Counter.get"              │                 │
│     │ fullName = "user::Counter.get#Counter"  │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  4. Lookup and execute                                          │
│     ┌─────────────────────────────────────────┐                 │
│     │ Function func = ctx.getFunction(fullName);               │
│     │ func.setArguments(params);              │                 │
│     │ return FunctionEvaluator.eval(func, ctx);│               │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Compile Mode - Bytecode Generation

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     COMPILE MODE                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐      ┌──────────────────────────┐         │
│  │  CompileContext  │      │  FunctionSignature       │         │
│  │                  │      │                          │         │
│  │  functionReg:    │◄─────│  - name                  │         │
│  │   Map<Key,Sig>   │      │  - parameters[]          │         │
│  │                  │      │  - returnType            │         │
│  │  functionsByName:│      │  - descriptor (JVM)      │         │
│  │   Map<name,Set>  │      │    e.g., "(II)I"         │         │
│  │                  │      └──────────────────────────┘         │
│  │  typeTable: Map  │                                           │
│  │  locals: Map     │                                           │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Compilers                            │   │
│  ├──────────────────┬───────────────────┬───────────────────┤   │
│  │ FunCallCompiler  │ MethodCallCompiler│StaticMethodCall   │   │
│  │                  │                    │    Compiler       │   │
│  │ funcName(args)   │ obj.method(args)   │ Type.method(args)│   │
│  │                  │                    │                   │   │
│  │ Emits:           │ Emits:             │ Emits:            │   │
│  │ INVOKESTATIC     │ INVOKEVIRTUAL      │ INVOKESTATIC      │   │
│  └──────────────────┴───────────────────┴───────────────────┘   │
│                                                                  │
│  Generated Bytecode Example:                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  // counter.get() where counter is type Counter          │   │
│  │  ALOAD 1           // Load counter reference             │   │
│  │  INVOKEVIRTUAL Counter.get ()I                           │   │
│  │                                                          │   │
│  │  // Math.add(1, 2) where Math is a type                  │   │
│  │  ICONST_1          // Push 1                             │   │
│  │  ICONST_2          // Push 2                             │   │
│  │  INVOKESTATIC Math.add (II)I                             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Function Signature Lookup

```java
// CompileContext - Two-tier lookup for overload resolution
public FunctionSignature lookupFunction(String name, DataType[] argTypes) {
    // 1. Try exact match first (O(1))
    String paramDescriptor = buildParamDescriptor(argTypes);
    FunctionSignatureKey key = new FunctionSignatureKey(name, paramDescriptor);
    FunctionSignature exact = functionRegistry.get(key);
    if (exact != null) return exact;

    // 2. Fallback: find compatible signature (type coercion)
    Set<FunctionSignature> candidates = functionsByName.get(name);
    for (FunctionSignature sig : candidates) {
        if (isCompatible(sig.getParameters(), argTypes)) {
            return sig;
        }
    }
    return null;
}
```

### Method Compilation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│           Instance Method Call: obj.method(args)                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  MethodCallCompiler.compile(mv, ctx)                            │
│                                                                  │
│  1. Compile target expression                                   │
│     ┌─────────────────────────────────────────┐                 │
│     │ targetCompiler.compile(mv, ctx);        │                 │
│     │ // Pushes instance reference onto stack │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  2. Get target type                                             │
│     ┌─────────────────────────────────────────┐                 │
│     │ StructType structType =                 │                 │
│     │   getStructTypeFromExpression(target);  │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  3. Lookup method with overload resolution                      │
│     ┌─────────────────────────────────────────┐                 │
│     │ DataType[] argTypes = inferTypes(args); │                 │
│     │ MethodDef method =                      │                 │
│     │   structType.getInstanceMethod(         │                 │
│     │     methodName, argTypes);              │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  4. Compile arguments                                           │
│     ┌─────────────────────────────────────────┐                 │
│     │ for (arg : args) {                      │                 │
│     │   argCompiler.compile(mv, ctx);         │                 │
│     │   // Type promotion if needed           │                 │
│     │ }                                       │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
│  5. Emit INVOKEVIRTUAL                                          │
│     ┌─────────────────────────────────────────┐                 │
│     │ String descriptor = buildDescriptor();  │                 │
│     │ mv.visitMethodInsn(                     │                 │
│     │   INVOKEVIRTUAL,                        │                 │
│     │   structType.getName(),  // "Counter"   │                 │
│     │   methodName,            // "get"       │                 │
│     │   descriptor,            // "()I"       │                 │
│     │   false                                 │                 │
│     │ );                                      │                 │
│     └─────────────────────────────────────────┘                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Type Methods

### Methods as Functions (Key Design)

In eval mode, type methods are registered as regular functions with a qualified name and `this` prepended as the first parameter for instance methods.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Type Method Registration                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  JCP Type Definition:                                           │
│  ┌─────────────────────────────────────────┐                    │
│  │  type Counter {                         │                    │
│  │    int count;                           │                    │
│  │    func get() -> int { return count; }  │   Instance method  │
│  │    static func zero() -> int { return 0; } Static method    │
│  │  }                                      │                    │
│  └─────────────────────────────────────────┘                    │
│                                                                  │
│  Registered Functions:                                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                                                              ││
│  │  Instance Method "get":                                      ││
│  │  ┌────────────────────────────────────────┐                 ││
│  │  │  Name: "user::Counter.get"             │                 ││
│  │  │  Full: "user::Counter.get#Counter"     │                 ││
│  │  │  Params: [ParameterDef("this", Counter)]│  ◄── 'this'    ││
│  │  │  Return: INT                           │      prepended  ││
│  │  │  Body: { return this.count; }          │                 ││
│  │  └────────────────────────────────────────┘                 ││
│  │                                                              ││
│  │  Static Method "zero":                                       ││
│  │  ┌────────────────────────────────────────┐                 ││
│  │  │  Name: "user::Counter.zero"            │                 ││
│  │  │  Full: "user::Counter.zero#"           │  ◄── No 'this'  ││
│  │  │  Params: []                            │                 ││
│  │  │  Return: INT                           │                 ││
│  │  │  Body: { return 0; }                   │                 ││
│  │  └────────────────────────────────────────┘                 ││
│  │                                                              ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### StructType Method Registry

In compile mode, `StructType` maintains method registries for bytecode generation:

```java
public class StructType implements DataType {
    private final Map<String, MethodDef> instanceMethods;  // signature → MethodDef
    private final Map<String, MethodDef> staticMethods;

    // Overload resolution with same algorithm as FunCallEvaluator
    public MethodDef getInstanceMethod(String name, DataType[] argTypes) {
        // 1. Filter by name
        // 2. Filter by param count
        // 3. Filter by type compatibility (isCastableTo)
        // 4. Return single match or throw ambiguity error
    }
}
```

---

## Module System

### Module Registration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Module Registration                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  BaseModuleRegister.classToRegister()                           │
│  ┌─────────────────────────────────────────┐                    │
│  │  return [                               │                    │
│  │    Logger.class,      // base module    │                    │
│  │    Assertions.class,  // base module    │                    │
│  │    ValueBuffer.class  // base module    │                    │
│  │  ];                                     │                    │
│  └─────────────────────────────────────────┘                    │
│                    │                                             │
│                    ▼                                             │
│  ClassConverter.registerClass(Logger.class, ctx, "base")        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                                                              ││
│  │  1. Create DataType("Logger")                                ││
│  │                                                              ││
│  │  2. For Logger.log(Object obj):                              ││
│  │     ┌─────────────────────────────────────────┐             ││
│  │     │  Static method → no 'this'              │             ││
│  │     │                                         │             ││
│  │     │  function = AbstractModuleFunction {   │             ││
│  │     │    getName() = "base::Logger.log"      │             ││
│  │     │    getFullName() = "base::Logger.log#Object"         ││
│  │     │    doFunction(args) {                  │             ││
│  │     │      return ReflectUtil.invoke(        │             ││
│  │     │        null, method, args);            │             ││
│  │     │    }                                   │             ││
│  │     │  }                                     │             ││
│  │     │                                         │             ││
│  │     │  ctx.addFunction(function);            │             ││
│  │     └─────────────────────────────────────────┘             ││
│  │                                                              ││
│  │  3. For instance methods (like ValueBuffer.set):             ││
│  │     ┌─────────────────────────────────────────┐             ││
│  │     │  Instance method → prepend 'this'       │             ││
│  │     │                                         │             ││
│  │     │  params = [ValueBuffer, String, ANY]   │             ││
│  │     │           ↑                             │             ││
│  │     │           'this' added as first param   │             ││
│  │     │                                         │             ││
│  │     │  function = AbstractModuleFunction {   │             ││
│  │     │    doFunction(params) {                │             ││
│  │     │      Object target = params[0].get();  │             ││
│  │     │      Object[] args = params[1..n];     │             ││
│  │     │      return ReflectUtil.invoke(        │             ││
│  │     │        target, method, args);          │             ││
│  │     │    }                                   │             ││
│  │     │  }                                     │             ││
│  │     └─────────────────────────────────────────┘             ││
│  │                                                              ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Java Type to JCP Type Mapping

| Java Type | JCP DataType |
|-----------|--------------|
| `int`, `Integer` | `SystemDataType.INT` |
| `double`, `Double` | `SystemDataType.DOUBLE` |
| `boolean`, `Boolean` | `SystemDataType.BOOLEAN` |
| `String` | `SystemDataType.STRING` |
| `void` | `SystemDataType.VOID` |
| `Object`, `T` | `SystemDataType.ANY` |
| User classes | `DataTypeImpl(className)` |

---

## Key Design Decisions

### 1. Methods as Functions

**Decision:** Type methods are registered as functions in the global function registry.

**Rationale:**
- Reuses existing function resolution system
- Supports overloading via `isCastableTo()`
- Consistent lookup pattern across all function types
- `this` parameter prepending unifies instance/static handling

```
┌─────────────────────────────────────────────────────────────────┐
│   Without this design          │   With this design             │
├────────────────────────────────┼────────────────────────────────┤
│                                │                                │
│  Separate method registry      │  Single function registry     │
│  Separate resolution logic     │  Unified resolution logic     │
│  Separate overload handling    │  Single overload system       │
│  Complex dispatch              │  Simple name-based lookup     │
│                                │                                │
└────────────────────────────────┴────────────────────────────────┘
```

### 2. Full Name with Parameters

**Decision:** Functions stored by full signature including param types.

**Format:** `name#paramType1@paramType2`

**Rationale:**
- Enables method overloading
- Unique key for function registry
- Parameter types are part of function identity

### 3. Dual Lookup Strategy (Compile Mode)

**Decision:** Two-tier lookup: exact match + compatibility fallback.

```java
// O(1) exact match for common case
FunctionSignatureKey key = new FunctionSignatureKey(name, paramDescriptor);
FunctionSignature exact = functionRegistry.get(key);

// Fallback: iterate candidates for type coercion
for (FunctionSignature sig : functionsByName.get(name)) {
    if (isCompatible(sig.getParameters(), argTypes)) return sig;
}
```

**Rationale:**
- O(1) for most lookups
- Type coercion support (int→double promotion)
- Consistent with eval mode behavior

### 4. 'this' as First Parameter

**Decision:** Instance methods receive `this` as first parameter.

**Benefits:**
- Same signature pattern for all functions
- No special-case handling in function evaluator
- Natural mapping to JVM (instance methods receive `this` in slot 0)

---

## File Reference

### Core Function Utilities

| File | Purpose |
|------|---------|
| `util/FunctionUtils.java` | Naming pattern utilities |
| `ast/statement/function/Function.java` | Function interface |
| `ast/statement/function/AbstractFunction.java` | User function base |
| `module/AbstractModuleFunction.java` | Java function wrapper |

### Eval Mode

| File | Purpose |
|------|---------|
| `eval/function/FunctionEvaluator.java` | Execute function body |
| `eval/function/FunCallEvaluator.java` | Resolve function calls |
| `eval/function/MethodCallEvaluator.java` | Instance method calls |
| `eval/function/StaticMethodCallEvaluator.java` | Static method calls |
| `eval/declare/TypeDeclarationEvaluator.java` | Type method registration |

### Context

| File | Purpose |
|------|---------|
| `eval/context/EvalContext.java` | Eval context interface |
| `eval/context/DefaultEvalContext.java` | Base implementation |
| `eval/context/RootEvalContext.java` | Entry context |
| `compile/context/CompileContext.java` | Compile context |

### Compile Mode

| File | Purpose |
|------|---------|
| `compile/function/FunCallCompiler.java` | Function call bytecode |
| `compile/function/MethodCallCompiler.java` | Instance method bytecode |
| `compile/function/StaticMethodCallCompiler.java` | Static method bytecode |

### Module System

| File | Purpose |
|------|---------|
| `module/base/BaseModuleRegister.java` | System module list |
| `util/ClassConverter.java` | Java class → JCP function |
| `module/base/logger/Logger.java` | Logger module |
| `module/base/assertions/Assertions.java` | Assertions module |
| `module/base/vb/ValueBuffer.java` | ValueBuffer module |

### Type System

| File | Purpose |
|------|---------|
| `eval/data/StructType.java` | Type metadata |
| `ast/statement/declaration/MethodDef.java` | Method definition |
| `ast/statement/function/ParameterDef.java` | Parameter definition |

---

## Summary

The JCP function architecture unifies user functions, module functions, and type methods through a consistent naming pattern (`[module::]type.method#paramTypes`) and lookup mechanism. The key insight is that **methods are registered as functions** in eval mode, allowing reuse of the function resolution system including overload handling.

Both modes support type-safe overloading through `DataType.isCastableTo()` compatibility checking, ensuring consistent behavior across interpreter and compiler execution paths.
