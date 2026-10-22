[← Back to README](../../README.md)

# Adding Custom Module Functions to JCP

This guide explains how to expose custom Java logic as globally-callable functions in JCP, working in both eval (interpreter) and compile (bytecode) modes.

**Audience:** DSL authors who have built their parse tree on top of JCP and want to call Java methods from DSL code.

**Prerequisites:** A working JCP project with `jcp-core` on the classpath. Familiarity with the [Integration Guide](../integration-guide.md) (building AST, running eval/compile).

---

## Quick Start

Register a Java class and call its methods from DSL code in under a minute.

**Eval mode:**

```java
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.module.JcpModuleRegistry;

// 1. Register your class
JcpModuleRegistry registry = new JcpModuleRegistry();
registry.registerClass(MyMath.class, "mymod");

// 2. Initialize the eval context
EvalContext ctx = new RootEvalContext();
JcpModuleRegistry.DEFAULT.initEval(ctx);  // base module
registry.initEval(ctx);                    // your module

// 3. Build AST and execute (MyMath.square(5) → 25)
new EvalVisitor(ctx).visit(program);
```

**Compile mode:**

```java
import com.elminster.jcp.compile.JcpCompiler;
import com.elminster.jcp.compile.BytecodeGenerator;

// 1. Create generator and register your class
BytecodeGenerator generator = new BytecodeGenerator("MyProgram");
generator.registerExternalClass(MyMath.class, "mymod");

// 2. Compile and run (MyMath.square(5) → 25)
// The base module is registered automatically by JcpCompiler.
byte[] bytecode = generator.compile(program);
```

The rest of this guide explains each step in detail.

---

## 1. What Is a Module Function?

A **module function** is a public Java method that JCP makes callable from DSL code. JCP uses reflection to discover the method's name, parameter types, and return type, then makes it available as `Type.method(args)` in the DSL.

Module functions are grouped by **module name** — a string label like `"base"` or `"guava"`. The base module ships with JCP and is registered automatically. You create your own modules by registering additional Java classes.

### Call Syntax at a Glance

| Syntax | When to use |
|--------|-------------|
| `Math.abs(-5)` | Unambiguous — only one `Math` is registered |
| `base::Math.abs(-5)` | Explicit module prefix — disambiguates when needed |
| `com.elminster.jcp.module.base.math.Math.abs(-5)` | Fully-qualified — always unambiguous |

---

## 2. Creating a Module Class

A module class is a plain Java class with public methods. No annotation, interface, or base class is required.

### Rules for Module Classes

- The class must be `public`.
- Only `public` methods are registered. Package-private, protected, and private methods are ignored.
- Static methods become callable as `Type.method(args)`.
- Instance methods are also registered (see [Section 5](#5-calling-module-functions-from-dsl-code) for how `this` is handled).
- Public constructors are registered as `Type.new(args)`.

### Example: MyMath

```java
package com.myapp.module.math;

public class MyMath {
    public static int square(int n) {
        return n * n;
    }

    public static double average(double a, double b) {
        return (a + b) / 2;
    }
}
```

From DSL code, these are called as:

```
MyMath.square(5)          // returns 25
MyMath.average(3.0, 7.0)  // returns 5.0
```

---

## 3. Registering Your Class

JCP provides four registration APIs. Use the decision table below to pick the right one.

### Which API to Use

| Scenario | Recommended API | Why |
|----------|----------------|-----|
| New project, both modes | `JcpModuleRegistry` | Single registration, works for eval + compile |
| Eval-only project | `JcpModuleRegistry` or `ClassConverter` | Registry is cleaner; ClassConverter is direct |
| Compile-only, one class | `BytecodeGenerator.registerExternalClass()` | Simplest for ad-hoc registration |
| JAR import | `JcpModuleRegistry.registerJar()` | Only API that handles JAR scanning |
| Post-registration customization | `JcpModuleRegistry.registerClass(clazz, module, customizer)` | Only API with customizer support |

### Unified Registration (JcpModuleRegistry) — Preferred

`JcpModuleRegistry` is the recommended API. You register classes once, then initialize whichever mode you need.

```java
import com.elminster.jcp.module.JcpModuleRegistry;

// Create a registry for your module
JcpModuleRegistry registry = new JcpModuleRegistry();
registry.registerClass(MyMath.class, "mymod");
registry.registerClass(MyUtils.class, "mymod");
```

#### Register-then-Init Pattern

Registration and initialization are separate steps. This lets you build up a registry once and apply it to multiple contexts:

```java
// Register (once)
JcpModuleRegistry registry = new JcpModuleRegistry();
registry.registerClass(MyMath.class, "mymod");

// Init for eval mode
EvalContext evalCtx = new RootEvalContext();
JcpModuleRegistry.DEFAULT.initEval(evalCtx);  // base module classes
registry.initEval(evalCtx);                    // your classes

// Init for compile mode
CompileContext compileCtx = new CompileContext();
JcpModuleRegistry.DEFAULT.initCompile(compileCtx);  // base module classes
registry.initCompile(compileCtx);                    // your classes
```

#### DEFAULT Singleton vs. New Instance

`JcpModuleRegistry.DEFAULT` is a pre-loaded singleton containing the base module classes (Math, Strings, Arrays, Convert, IO, Logger, Assertions, ValueBuffer, SortKey). It is intended for init-time use — do not mutate it after initialization, and do not close it.

For your own classes, create a new `JcpModuleRegistry` instance. This keeps your registrations isolated and allows proper cleanup (see below).

#### Lifecycle: close() for JAR Registrations

`JcpModuleRegistry` implements `Closeable`. When you register JAR files, the registry creates `URLClassLoader` instances that must be closed when you are done:

```java
try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
    registry.registerJar(Path.of("libs/mylib.jar"), "mymod");
    registry.initEval(ctx);
    // ... use the module ...
}  // classloaders are closed here
```

For class-only registrations (no JARs), closing is optional but harmless.

### Low-Level: Eval Mode Only (ClassConverter)

`ClassConverter.registerClass()` registers a Java class directly into an `EvalContext`:

```java
import com.elminster.jcp.util.ClassConverter;

EvalContext ctx = new RootEvalContext();
ClassConverter.registerClass(MyMath.class, ctx, "mymod");
```

This creates:
- An `ExternalClassType` data type for `MyMath`
- Functions for each public static method (e.g. `MyMath.square`)
- Functions for each public instance method (with `this` as the first parameter)
- Constructor functions (e.g. `MyMath.new`)

### Low-Level: Compile Mode Only (CompileModeClassConverter)

`CompileModeClassConverter.registerClass()` registers a Java class directly into a `CompileContext`:

```java
import com.elminster.jcp.compile.util.CompileModeClassConverter;

CompileContext ctx = new CompileContext();
CompileModeClassConverter.registerClass(MyMath.class, ctx, "mymod");
```

This creates an `ExternalClassType` with `ExternalMethodDef` entries for each public method, along with JVM descriptors needed for bytecode generation.

### Compile Shortcut: BytecodeGenerator.registerExternalClass()

For compile-only use, `BytecodeGenerator` provides a convenience method that defers registration to compile time:

```java
BytecodeGenerator generator = new BytecodeGenerator("MyProgram");
generator.registerExternalClass(MyMath.class, "mymod");

// Or without a module name (defaults to "external"):
generator.registerExternalClass(MyMath.class);

byte[] bytecode = generator.compile(program);
```

This is the simplest path for ad-hoc compile-mode registration.

---

## 4. Importing an External JAR

`JcpModuleRegistry.registerJar()` scans a JAR file and registers every public, non-inner class it finds:

```java
try (JcpModuleRegistry registry = new JcpModuleRegistry()) {
    registry.registerJar(Path.of("libs/mylib.jar"), "mymod");

    // All public classes in the JAR are now callable as mymod::Type.method(args)
    registry.initEval(ctx);
}
```

Inner classes (names containing `$`) and non-public classes are skipped. Classes that fail to load (e.g. missing dependencies) are logged as warnings and skipped.

---

## 5. Calling Module Functions from DSL Code

### Static Methods: Type.method(args)

Static methods are called using the class simple name and method name:

```
MyMath.square(5)
MyMath.average(3.0, 7.0)
```

### Instance Methods: Eval vs. Compile Differences

Instance methods are supported in both modes, but the dispatch mechanism differs.

**Eval mode** registers instance methods as functions with `this` prepended as the first parameter:

```
// DSL code: create an instance and call a method
StringBuilder sb = StringBuilder.new()
StringBuilder.append(sb, "hello")  // 'sb' is passed as the 'this' argument
```

**Compile mode** uses JVM `INVOKEVIRTUAL` / `INVOKEINTERFACE` instructions via `MethodCallCompiler`, which handles the receiver object natively:

```
// Same DSL code works in compile mode, but dispatches via JVM bytecode
StringBuilder sb = StringBuilder.new()
sb.append("hello")  // compiled to INVOKEVIRTUAL
```

### Name Resolution: Three-Tier Strategy

When JCP encounters a type name in a method call, it resolves it using this priority:

1. **Simple name** — `Math.abs(-5)`. Works when only one class named `Math` is registered across all modules.

2. **Module prefix** — `base::Math.abs(-5)`. Required when two modules register classes with the same simple name. The syntax is `module::SimpleName`.

3. **Fully-qualified name** — `com.elminster.jcp.module.base.math.Math.abs(-5)`. Always unambiguous. Useful when even module prefixes collide (unlikely but possible).

### Module Prefix: module::Type.method(args)

The `module::` prefix works for all registered modules, not just the base module:

```
mymod::MyMath.square(5)
guava::Strings.repeat("ab", 3)
base::Math.abs(-5)
```

---

## 6. Type Mapping Reference

### JCP to Java Type Table

JCP maps between its type system and Java types. The mapping is used both when registering method signatures and when passing arguments at runtime.

| JCP Type | Java Types |
|----------|-----------|
| `Any` | `Object` (universal) |
| `Any[]` | `Object[]` |
| `String` | `String`, `CharSequence`, `char`, `Character` |
| `String[]` | `String[]`, `char[]` |
| `Boolean` | `boolean`, `Boolean` |
| `Boolean[]` | `boolean[]`, `Boolean[]` |
| `Numeric` | (abstract parent — no direct Java type) |
| `Numeric[]` | (abstract parent — no direct Java type) |
| `Integer` | `int`, `Integer`, `byte`, `Byte`, `short`, `Short` |
| `Integer[]` | `int[]`, `Integer[]`, `byte[]`, `short[]` |
| `Long` | `long`, `Long` |
| `Long[]` | `long[]` |
| `Double` | `double`, `Double`, `float`, `Float` |
| `Double[]` | `double[]`, `Double[]`, `float[]` |
| `Void` | `void`, `Void` |

### ExternalClassType for Unknown Java Types

When a method parameter or return type is a Java class not in the table above (e.g. `java.util.Date`), JCP creates an `ExternalClassType` for it automatically. This type:

- Has a simple name matching the Java class simple name (e.g. `Date`)
- Is registered in the context so other methods referencing the same class share the same type
- Supports Java-level assignability checks (e.g. a `Date` parameter accepts a `Timestamp` argument if `Date.isAssignableFrom(Timestamp)`)

---

## 7. Overload Resolution

JCP supports method overloading. When multiple methods share the same name, resolution works as follows:

1. **Filter by parameter count** — candidates with a different number of parameters are eliminated.
2. **Filter by type compatibility** — each argument must be compatible with the corresponding parameter type (hierarchy walk + numeric widening + Java assignability).
3. **Prefer exact match** — if multiple candidates survive, the one where all argument types match parameter types exactly (by name) wins.
4. **Ambiguity** — if zero or more than one exact match exists, an `IllegalArgumentException` (compile) or `FunctionAmbiguityException` (eval) is thrown.

Example:

```java
public class Math {
    public static int abs(int n) { return java.lang.Math.abs(n); }
    public static double abs(double n) { return java.lang.Math.abs(n); }
}
```

```
Math.abs(-5)    // resolves to abs(int) — exact match preferred over widening to double
Math.abs(-5.0)  // resolves to abs(double)
```

---

## 8. Advanced: ExternalClassType Customizer

When registering a class via `JcpModuleRegistry`, you can pass a customizer callback that modifies the `ExternalClassType` after creation. This is useful for adding additional compatible Java types:

```java
registry.registerClass(MyPlot.class, "charts", ect -> {
    // Allow a MyPlot parameter to accept a PiePlot argument at the JCP level
    ect.addCompatibleJavaType(PiePlot.class);
});
```

The customizer receives the `ExternalClassType` after its methods have been populated. It is invoked during `initEval()` or `initCompile()`.

---

## 9. Advanced: Circular Dependency Handling

When registering a class, JCP discovers and registers parameter/return types recursively. If class A has a method returning class B, and class B has a method taking class A, a cycle exists.

JCP breaks cycles using an **in-progress sentinel set** (the same pattern used by Spring's bean factory):

1. Before populating methods for class A, mark A as in-progress.
2. When B's method references A, find A already in-progress — leave the existing stub and stop recursing.
3. After A's methods are fully populated, remove A from the in-progress set.

This produces a fully populated type for A and a "stub" type for B (registered, but methods may be incomplete for the cycle edge). In practice, cycles are rare and the stub is sufficient for method dispatch.

---

## 10. Limitations and Known Constraints

- **Public methods only.** Only `public` methods on `public` classes are registered. Package-private, protected, and private methods are invisible to JCP.

- **Inner classes skipped during JAR scanning.** `registerJar()` skips classes whose names contain `$`. To register an inner class, use `registerClass()` directly.

- **Instance methods in eval mode.** Eval mode models instance methods as functions where `this` is the first argument. This works, but the call syntax differs from compile mode (see [Section 5](#5-calling-module-functions-from-dsl-code)).

- **Classloader isolation for JARs.** Each `registerJar()` call creates its own `URLClassLoader`. Classes from different JAR registrations cannot see each other. Close the registry when done to release classloader resources.

- **No generic type preservation.** Java generics are erased at runtime. A method `List<String> getNames()` is registered with return type `Any` (since `List` is not a built-in JCP type), not `String[]`.

- **`Object` parameters map to `Any`.** A Java method parameter of type `Object` becomes JCP type `Any`, which accepts any argument — there is no compile-time narrowing.

---

## End-to-End Example

A complete, runnable example that registers a custom class and calls it from both eval and compile modes.

### The Module Class

```java
package com.myapp.module;

public class Greeter {
    private final String prefix;

    public Greeter(String prefix) {
        this.prefix = prefix;
    }

    public static String hello(String name) {
        return "Hello, " + name + "!";
    }

    public String greet(String name) {
        return prefix + " " + name + "!";
    }
}
```

### Eval Mode

```java
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.impl.BlockImpl;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.FunctionCallExpression;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.module.JcpModuleRegistry;

// 1. Register
JcpModuleRegistry registry = new JcpModuleRegistry();
registry.registerClass(Greeter.class, "mymod");

// 2. Build context
EvalContext ctx = new RootEvalContext();
JcpModuleRegistry.DEFAULT.initEval(ctx);
registry.initEval(ctx);

// 3. Build AST for: string msg = Greeter.hello("World")
Block program = new BlockImpl();
// ... (build AST nodes for the variable declaration and function call)

// 4. Execute
new EvalVisitor(ctx).visit(program);

// 5. Read result
Data result = ctx.getVariable("msg");
// result.get() → "Hello, World!"
```

### Compile Mode

```java
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.impl.BlockImpl;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.JcpCompiler;

// 1. Build AST for: string msg = Greeter.hello("World")
Block program = new BlockImpl();
// ... (build AST nodes)

// 2. Create generator with registration
BytecodeGenerator generator = new BytecodeGenerator("GreeterDemo");
generator.registerExternalClass(Greeter.class, "mymod");

// 3. Compile and load
JcpCompiler compiler = new JcpCompiler();
Class<?> clazz = compiler.compileAndLoad(program, "GreeterDemo");

// 4. Execute
clazz.getMethod("main", String[].class)
     .invoke(null, (Object) new String[]{});
```

---

## See Also

- [Integration Guide](../integration-guide.md) — Connect your DSL to JCP, execute programs
- [Language Reference](../language-reference.md) — DSL syntax: types, control flow, functions, structs, modules
- [Architecture](../architecture/jcp-function-architecture.md) — Function system internals, method dispatch
