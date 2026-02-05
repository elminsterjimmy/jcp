---
title: "feat: Type System with Constructors and Methods"
type: feat
date: 2026-02-05
deepened: 2026-02-05
---

# Type System with Constructors and Methods

## Enhancement Summary

**Deepened on:** 2026-02-05
**Research agents used:** architecture-strategist, pattern-recognition-specialist, performance-oracle, code-simplicity-reviewer, security-sentinel, best-practices-researcher, framework-docs-researcher, learnings-researcher (3 docs)

### ⚠️ CRITICAL ARCHITECTURE DECISION (KISS)

**Type methods ARE functions.** They use the existing function system.

**Naming Pattern:**
- **Current (backward compatible):** `[module::]type.method#paramTypes`
  - `Assertions.assertTrue#Boolean` - base module (no prefix)
  - `user::Counter.getCount#Counter` - user module
- **New fully qualified (for future use):** `module::type::method#paramTypes`
  - `base::Assertions::assertTrue#Boolean`
  - `user::Counter::getCount#Counter`
  - `user::global::sum#int@int` - global function (no type)

**Key Principles:**
1. **No new classes for type methods** - use existing `AbstractFunction`
2. **Methods are registered as functions** with current pattern for backward compatibility
3. **Instance methods**: prepend 'this' (the type) as first parameter
4. **Static methods**: no 'this' parameter
5. **Use existing function lookup** via `FunctionUtils`

**This aligns with `ClassConverter.java`** which registers Java methods as functions using the same pattern.

**What NOT to do:**
- Do NOT create `TypeMethodFunction` or `ModuleMethodFunction` classes
- Do NOT store methods in `StructType`
- Do NOT create separate method lookup logic

### Key Improvements from Research
1. **Simplification**: Reduced from 11 new files to 6 by consolidating AST nodes
2. **Performance Fix**: Must add HashMap cache to `StructType.getField()` (currently O(n))
3. **Critical Bug Prevention**: Must test BOTH `compile()` and `compileWithReturn()` entry points
4. **Type Resolution**: Must resolve parameter/return types through context lookup
5. **Existing Code Reuse**: Use existing function system (`module::type.method` pattern)

### New Considerations Discovered
- Existing `MethodCallEvaluator` at `eval/function/` handles module method calls
- Methods should be registered as functions, not stored in StructType
- Context inheritance required for type lookups in method bodies
- Field lookup performance bottleneck must be fixed first
- Security validation needed for type/method names

---

## Overview

Extend JCP's current struct implementation to support explicit constructors, instance methods, and static methods. This transforms simple data-only structs into full "data + behavior" types (similar to Rust structs with impl blocks, but without inheritance).

**Replaces:** Current `struct` keyword with `type` keyword.

## Problem Statement / Motivation

The current struct implementation only supports fields. To enable object-oriented patterns and match the `ClassConverter.java` functionality (which converts Java classes with methods), JCP needs:

1. **Explicit constructors** - Custom initialization logic beyond simple field assignment
2. **Instance methods** - Behavior tied to specific instances with `this` access
3. **Static methods** - Type-level utility functions

This unlocks patterns like encapsulation, fluent APIs, and type-specific operations.

## Target Syntax

```jcp
type Point {
    int x;
    int y;

    constructor(int x, int y) {
        this.x = x;
        this.y = y;
    }

    func distance(Point other) -> int {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    static func origin() -> Point {
        return Point(0, 0);
    }
}

// Usage:
Point p1 = Point(10, 20);
Point p2 = Point.origin();
int dist = p1.distance(p2);
int xVal = p1.x;  // Direct field access allowed
```

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| `this` requirement | Required for field access | No ambiguity with parameters; explicit is better |
| Return types | Required with `-> type` | Type-safe, explicit |
| Default constructor | Auto-generated if none defined | Fields get default values (0, false, null) |
| Field access | Direct access allowed | Compatible with existing struct behavior |
| Name resolution | Types and functions in separate namespaces | Type constructors take precedence |
| Method dispatch | Hybrid (INVOKEVIRTUAL for instance, INVOKESTATIC for static) | True JVM methods for interop |
| Visibility | All public | YAGNI - defer visibility modifiers |

## Technical Approach

### Architecture Decision: Integrate with Existing Function System

**CRITICAL ARCHITECTURAL DECISION:** Type methods should NOT use a separate lookup mechanism in `StructType`. Instead, methods should be registered as functions using the existing `module::type.method` pattern already supported by `FunCallEvaluator` and `MethodCallEvaluator`.

**Pattern:** `module::type.method#paramType1@paramType2`

- For user-defined types, the module defaults to `user` (or can be user-specified)
- Instance methods: first parameter is implicitly `this` (the instance)
- Static methods: no `this` parameter

**Benefits:**
1. Reuses existing function resolution and overload logic (`FunCallEvaluator.hasSameParameterDefinition()`)
2. Consistent with existing module system for built-in types (e.g., `base::String.length`)
3. No duplicate method lookup logic
4. Supports type hierarchy via `isCastableTo()` checks already in place

**Example Function Registration:**
```java
// Instance method: Counter.getCount() -> int
// Registered as: user::Counter.getCount#Counter
// The first parameter (Counter) represents 'this'

// Static method: Math.add(int a, int b) -> int
// Registered as: user::Math.add#int@int
// No implicit 'this' parameter
```

### Phase 0: Prerequisites (MUST DO FIRST)

#### 0.1 Fix Field Lookup Performance

**Research Insight (Performance Oracle):** Current `StructType.getField()` uses O(n) linear search. This must be fixed before adding methods to avoid performance inconsistency.

```java
// core/src/main/java/com/elminster/jcp/eval/data/StructType.java
public class StructType implements DataType {
    private final String name;
    private final List<StructFieldDef> fields;

    // ADD: O(1) lookup cache
    private final Map<String, StructFieldDef> fieldCache;

    public StructType(String name, List<StructFieldDef> fields) {
        this.name = name;
        this.fields = new ArrayList<>(fields);

        // Build field cache on construction (one-time cost)
        this.fieldCache = new HashMap<>(fields.size());
        for (StructFieldDef field : fields) {
            fieldCache.put(field.getName().getId(), field);
        }
    }

    public StructFieldDef getField(String fieldName) {
        return fieldCache.get(fieldName);  // O(1) instead of O(n)
    }
}
```

**Expected Performance Gain:** 5-50x faster field access depending on field count.

### Phase 1: AST Changes (Simplified)

**Research Insight (Code Simplicity Reviewer):** Consolidate AST nodes to reduce complexity by 35%.

#### 1.1 New AST Nodes (Consolidated)

**MethodDef** - Unified method/constructor definition
```java
// core/src/main/java/com/elminster/jcp/ast/statement/declaration/MethodDef.java
public class MethodDef {
    Identifier name;           // "<init>" for constructors, method name otherwise
    ParameterDef[] parameters;
    DataType returnType;       // VOID for constructors
    Block body;
    boolean isStatic;

    public boolean isConstructor() {
        return "<init>".equals(name.getId());
    }
}
```

**StaticMethodCallExpression** - Static method call (`Type.method(args)`)
```java
// core/src/main/java/com/elminster/jcp/ast/expression/StaticMethodCallExpression.java
public class StaticMethodCallExpression extends AbstractExpression {
    Identifier typeName;
    String methodName;
    Expression[] arguments;

    @Override
    public String getName() {
        return "STATIC_METHOD_CALL";
    }
}
```

**ThisExpression** - `this` keyword
```java
// core/src/main/java/com/elminster/jcp/ast/expression/ThisExpression.java
public class ThisExpression extends AbstractExpression {
    @Override
    public String getName() {
        return "THIS";
    }
}
```

#### 1.2 Extend StructDeclaration (DO NOT Rename Files)

**Research Insight (Code Simplicity Reviewer):** Renaming files provides zero functional value and breaks git history. Instead, extend the interface and add `getName()` return of `"TYPE_DECLARATION"`.

```java
// Extend existing StructDeclarationImpl - don't rename files
public class StructDeclarationImpl implements StructDeclaration {
    Identifier name;
    List<StructFieldDef> fields;
    MethodDef constructor;           // NEW: explicit constructor (nullable)
    List<MethodDef> instanceMethods; // NEW
    List<MethodDef> staticMethods;   // NEW

    @Override
    public String getName() {
        // Return new name for factory lookup while keeping file name
        return "TYPE_DECLARATION";
    }
}
```

#### 1.3 Extend StructType (Minimal Changes)

**IMPORTANT:** Methods are NOT stored in StructType. They are registered as functions in the context using `module::type.method` pattern.

```java
// Extend existing StructType - only store constructor (for instantiation)
public class StructType implements DataType {
    // Existing:
    private final String name;
    private final List<StructFieldDef> fields;
    private final Map<String, StructFieldDef> fieldCache;  // From Phase 0

    // NEW: Constructor only (methods registered as functions)
    private final MethodDef constructor;

    // Methods are NOT stored here - they are registered as functions
    // via TypeDeclarationEvaluator using pattern: user::TypeName.methodName

    public MethodDef getConstructor() { return constructor; }
}
```

#### 1.4 Extend StructDeclaration (Keep Method Definitions for AST)

```java
// StructDeclaration still holds method definitions in AST
// But these are registered as functions during evaluation, not stored in StructType
public class StructDeclarationImpl implements StructDeclaration {
    Identifier name;
    List<StructFieldDef> fields;
    MethodDef constructor;           // Explicit constructor (nullable)
    List<MethodDef> instanceMethods; // AST definitions
    List<MethodDef> staticMethods;   // AST definitions

    // These methods are used by TypeDeclarationEvaluator to register functions
}
```

### Phase 2: Interpreter Implementation (eval/)

#### 2.1 TypeDeclarationEvaluator (Register Methods as Functions)

**CRITICAL:** Methods should be registered as functions using `module::type.method` pattern, NOT stored in StructType.

```java
// core/src/main/java/com/elminster/jcp/eval/declare/TypeDeclarationEvaluator.java
public class TypeDeclarationEvaluator extends AbstractAstEvaluator {

    private static final String USER_MODULE = "user";

    @Override
    public Data eval(EvalContext context) {
        StructDeclaration decl = (StructDeclaration) astNode;
        String typeName = decl.getId().getId();

        // 1. Register type (for field access, instantiation)
        StructType type = new StructType(
            typeName,
            decl.getFields(),
            decl.getConstructor()  // Only constructor, not methods
        );
        context.addDataType(type);

        // 2. Register instance methods as functions
        // Pattern: user::TypeName.methodName#paramTypes
        // Instance methods get 'this' (type) as first parameter
        for (MethodDef method : decl.getInstanceMethods()) {
            registerInstanceMethod(context, typeName, method, type);
        }

        // 3. Register static methods as functions
        // Pattern: user::TypeName.methodName#paramTypes
        // Static methods have NO 'this' parameter
        for (MethodDef method : decl.getStaticMethods()) {
            registerStaticMethod(context, typeName, method);
        }

        return AnyData.EMPTY;
    }

    /**
     * Register instance method as a function.
     * The method is wrapped in a Function that:
     * - Has the type as first parameter (representing 'this')
     * - Binds 'this' in the method scope when executed
     */
    private void registerInstanceMethod(EvalContext context, String typeName,
                                         MethodDef method, StructType type) {
        // Build parameter list: prepend 'this' (type) to method params
        ParameterDef[] methodParams = method.getParameters();
        ParameterDef[] funcParams = new ParameterDef[methodParams.length + 1];
        funcParams[0] = ParameterDef.of("this", type);  // 'this' is first param
        System.arraycopy(methodParams, 0, funcParams, 1, methodParams.length);

        // Create function with module pattern
        Function func = new TypeMethodFunction(
            USER_MODULE,
            typeName,
            method.getName().getId(),
            funcParams,
            method.getReturnType(),
            method.getBody(),
            true  // isInstance = true
        );

        // Register using full name: user::TypeName.methodName#paramTypes
        String fullName = FunctionUtils.generateFunctionFullName(
            USER_MODULE, typeName, method.getName().getId(), funcParams);
        context.addFunction(fullName, func);
    }

    /**
     * Register static method as a function.
     * No 'this' parameter for static methods.
     */
    private void registerStaticMethod(EvalContext context, String typeName,
                                       MethodDef method) {
        Function func = new TypeMethodFunction(
            USER_MODULE,
            typeName,
            method.getName().getId(),
            method.getParameters(),
            method.getReturnType(),
            method.getBody(),
            false  // isInstance = false
        );

        String fullName = FunctionUtils.generateFunctionFullName(
            USER_MODULE, typeName, method.getName().getId(), method.getParameters());
        context.addFunction(fullName, func);
    }
}
```

#### 2.2 Constructor Invocation (Extend StructInstantiationEvaluator)

```java
// Extend existing StructInstantiationEvaluator to handle explicit constructors
@Override
public Data eval(EvalContext context) {
    StructInstantiation inst = (StructInstantiation) astNode;
    StructType type = (StructType) context.getDataType(inst.getStructType().getId());

    if (type.getConstructor() != null) {
        // Explicit constructor path:
        // 1. Create StructData with default field values
        Map<String, Data> fieldValues = new HashMap<>();
        for (StructFieldDef field : type.getFields()) {
            fieldValues.put(field.getName().getId(), getDefaultValue(field.getDataType()));
        }
        StructData instance = new StructData(fieldValues, type);

        // 2. Create scope with 'this' bound to instance
        EvalContext ctorContext = context.createChildContext();
        ctorContext.addVariable("this", instance);

        // 3. Bind constructor parameters
        MethodDef ctor = type.getConstructor();
        Expression[] args = inst.getFieldValues().toArray(new Expression[0]);
        for (int i = 0; i < ctor.getParameters().length; i++) {
            Data argValue = visitor.visit(args[i]);
            ctorContext.addVariable(ctor.getParameters()[i].getName(), argValue);
        }

        // 4. Evaluate constructor body
        new EvalVisitor(ctorContext).visit(ctor.getBody());

        return instance;
    } else {
        // Auto-generated constructor: existing behavior (positional field assignment)
        // ... existing code ...
    }
}
```

#### 2.3 Extend Existing MethodCallEvaluator (Use Function Lookup)

**CRITICAL:** `MethodCallEvaluator` should use the EXISTING function lookup system via `FunCallEvaluator` pattern, NOT a separate method lookup.

```java
// core/src/main/java/com/elminster/jcp/eval/function/MethodCallEvaluator.java
// EXTEND existing evaluator to use function lookup for struct methods
@Override
public Data eval(EvalContext context) {
    MethodCallExpression expr = (MethodCallExpression) astNode;

    // 1. Evaluate target object
    Evaluable targetEval = AstEvaluatorFactory.getEvaluator(expr.getExpression());
    Data target = targetEval.eval(context);

    // 2. For StructData, use function lookup with module::type.method pattern
    if (target instanceof StructData) {
        return evalStructMethodViaFunctionLookup(context, expr, (StructData) target);
    }

    // Existing module method call handling...
    // ... existing code remains unchanged ...
}

/**
 * Handle struct instance method calls via the function system.
 * Methods are registered as: user::TypeName.methodName#thisType@paramTypes
 * 'this' is passed as the first argument.
 */
private Data evalStructMethodViaFunctionLookup(EvalContext context,
                                                MethodCallExpression expr,
                                                StructData instance) {
    String methodName = expr.getMethodName();
    StructType type = instance.getStructType();
    Expression[] argExprs = expr.getArguments();

    // Evaluate arguments and prepend 'this' as first argument
    Data[] allArgs = new Data[argExprs.length + 1];
    allArgs[0] = instance;  // 'this' is first parameter
    for (int i = 0; i < argExprs.length; i++) {
        allArgs[i + 1] = AstEvaluatorFactory.getEvaluator(argExprs[i]).eval(context);
    }

    // Use existing function lookup: user::TypeName.methodName
    String moduleName = "user";  // Default module for user-defined types
    String functionName = FunctionUtils.getModuleFunctionName(
        moduleName, type.getName(), methodName);

    // Find function candidates (handles overloading via isCastableTo)
    List<Function> candidates = context.getFunctions().values().stream()
        .filter(f -> f.getId().getId().equals(methodName))
        .filter(f -> isUserTypeMethod(f, type.getName()))
        .filter(f -> hasSameParameterDefinition(allArgs, f))
        .collect(Collectors.toList());

    if (candidates.isEmpty()) {
        DataType[] types = Arrays.stream(allArgs).map(Data::getDataType).toArray(DataType[]::new);
        UndeclaredException.throwFunctionUndeclaredException(
            Identifier.fromName(functionName), types);
    }
    if (candidates.size() > 1) {
        // Handle ambiguity
        DataType[] types = Arrays.stream(allArgs).map(Data::getDataType).toArray(DataType[]::new);
        throw new FunctionAmbiguityException(Identifier.fromName(functionName), types);
    }

    Function function = candidates.get(0);
    function.setArguments(allArgs);
    return AstEvaluatorFactory.getEvaluator(function).eval(context);
}

// Reuse FunCallEvaluator's type-compatible parameter matching
private boolean hasSameParameterDefinition(Data[] arguments, Function function) {
    ParameterDef[] parameterDefs = function.getParameterDefs();
    if (arguments.length != parameterDefs.length) return false;
    for (int i = 0; i < parameterDefs.length; i++) {
        if (!arguments[i].getDataType().isCastableTo(parameterDefs[i].getDataType())) {
            return false;
        }
    }
    return true;
}
```

**Key Insight:** The function registered for instance method `getCount()` on type `Counter` would be:
- Function name: `getCount`
- Module: `user`
- Type: `Counter`
- Parameters: `[Counter this]` (plus any other params)
- Full lookup key: `user::Counter.getCount#Counter`

#### 2.4 StaticMethodCallEvaluator (Use Function Lookup)

**CRITICAL:** Static methods should also use function lookup, NOT direct StructType access.

```java
// core/src/main/java/com/elminster/jcp/eval/function/StaticMethodCallEvaluator.java
public class StaticMethodCallEvaluator extends AbstractAstEvaluator {
    @Override
    public Data eval(EvalContext context) {
        StaticMethodCallExpression expr = (StaticMethodCallExpression) astNode;
        String typeName = expr.getTypeName().getId();
        String methodName = expr.getMethodName();
        Expression[] argExprs = expr.getArguments();

        // Verify type exists (for error messages)
        StructType type = (StructType) context.getDataType(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Type not found: " + typeName);
        }

        // Evaluate arguments (NO 'this' for static methods)
        Data[] args = new Data[argExprs.length];
        for (int i = 0; i < argExprs.length; i++) {
            args[i] = AstEvaluatorFactory.getEvaluator(argExprs[i]).eval(context);
        }

        // Use function lookup: user::TypeName.methodName#paramTypes
        String moduleName = "user";
        String functionName = FunctionUtils.getModuleFunctionName(
            moduleName, typeName, methodName);

        // Find function candidates (handles overloading)
        List<Function> candidates = context.getFunctions().values().stream()
            .filter(f -> f.getId().getId().equals(methodName))
            .filter(f -> isUserTypeStaticMethod(f, typeName))
            .filter(f -> hasSameParameterDefinition(args, f))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            DataType[] types = Arrays.stream(args).map(Data::getDataType).toArray(DataType[]::new);
            UndeclaredException.throwFunctionUndeclaredException(
                Identifier.fromName(functionName), types);
        }
        if (candidates.size() > 1) {
            DataType[] types = Arrays.stream(args).map(Data::getDataType).toArray(DataType[]::new);
            throw new FunctionAmbiguityException(Identifier.fromName(functionName), types);
        }

        Function function = candidates.get(0);
        function.setArguments(args);
        return AstEvaluatorFactory.getEvaluator(function).eval(context);
    }
}
```

**Key Insight:** Static method `Math.add(int a, int b)` is registered as:
- Function name: `add`
- Module: `user`
- Type: `Math`
- Parameters: `[int a, int b]` (no 'this')
- Full lookup key: `user::Math.add#int@int`

#### 2.5 TypeMethodFunction (NEW - Function Wrapper for Methods)

**CRITICAL:** This class wraps method definitions as Functions so they can be registered and invoked through the existing function system.

```java
// core/src/main/java/com/elminster/jcp/eval/function/TypeMethodFunction.java
/**
 * Function wrapper for type methods.
 * For instance methods, 'this' is bound from the first argument.
 * Implements Modulable for module::type.method pattern.
 */
public class TypeMethodFunction implements Function, Modulable {
    private final String moduleName;
    private final String typeName;
    private final String methodName;
    private final ParameterDef[] parameters;
    private final DataType returnType;
    private final Block body;
    private final boolean isInstance;
    private Data[] arguments;

    @Override
    public String getModule() {
        return moduleName;
    }

    @Override
    public Identifier getId() {
        return Identifier.fromName(methodName);
    }

    @Override
    public ParameterDef[] getParameterDefs() {
        return parameters;
    }

    // When evaluated by FunctionEvaluator:
    // - For instance methods: arguments[0] is bound as 'this'
    // - For static methods: no 'this' binding
}
```

#### 2.6 TypeMethodFunctionEvaluator (NEW - Execute Type Methods)

```java
// core/src/main/java/com/elminster/jcp/eval/function/TypeMethodFunctionEvaluator.java
/**
 * Evaluator for TypeMethodFunction.
 * Handles 'this' binding for instance methods.
 */
public class TypeMethodFunctionEvaluator extends AbstractAstEvaluator {
    @Override
    public Data eval(EvalContext evalContext) {
        TypeMethodFunction func = (TypeMethodFunction) astNode;
        Data[] args = func.getArguments();
        ParameterDef[] params = func.getParameterDefs();

        // Create method scope
        FastStack<EvalContext> contextStack = evalContext.getContextStack();
        DefaultEvalContext methodContext = new DefaultEvalContext();
        contextStack.push(methodContext);

        try {
            // For instance methods, bind 'this' from first argument
            if (func.isInstance()) {
                // First argument IS the instance (StructData)
                StructData instance = (StructData) args[0];
                methodContext.getVariables().put("this", instance);
            }

            // Bind remaining parameters
            int argOffset = func.isInstance() ? 1 : 0;
            int paramOffset = func.isInstance() ? 1 : 0;  // Skip 'this' param
            for (int i = paramOffset; i < params.length; i++) {
                Data argData = new AnyData<>(
                    Identifier.fromName(params[i].getId()),
                    params[i].getDataType(),
                    args[i].get(),
                    false
                );
                methodContext.getVariables().put(params[i].getId(), argData);
            }

            // Execute method body
            Evaluable blockEval = AstEvaluatorFactory.getEvaluator(func.getBody());
            return blockEval.eval(evalContext);
        } finally {
            contextStack.pop();
        }
    }
}
```

#### 2.7 ThisEvaluator (NEW)

```java
// core/src/main/java/com/elminster/jcp/eval/struct/ThisEvaluator.java
// Place in /struct/ package (it's tied to struct instance context)
public class ThisEvaluator extends AbstractAstEvaluator {
    @Override
    public Data eval(EvalContext context) {
        Data thisRef = context.getVariable("this");
        if (thisRef == null) {
            throw new IllegalStateException(
                "'this' can only be used in instance methods or constructors");
        }
        return thisRef;
    }
}
```

### Phase 3: Compiler Implementation (compile/)

#### 3.1 TypeDeclarationCompiler

**Research Insight (Learnings):** BOTH registrations are REQUIRED per documented learning.

```java
// core/src/main/java/com/elminster/jcp/compile/declare/TypeDeclarationCompiler.java
// (Rename existing StructDeclarationCompiler)
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    StructDeclaration decl = (StructDeclaration) astNode;
    String typeName = decl.getId().getId();

    /**
     * CRITICAL: Dual registration pattern (see docs/solutions/.../struct-type-not-registered...)
     *
     * Both addDataType() and addGeneratedClass() are REQUIRED:
     * 1. addDataType(structType) - Registers type metadata for:
     *    - StructInstantiationCompiler to look up constructor/fields
     *    - MethodCallCompiler to look up instance methods
     *    - StaticMethodCallCompiler to look up static methods
     *    - FieldAccessCompiler to resolve field types
     *
     * 2. addGeneratedClass(name, bytecode) - Enables runtime loading via MultiClassLoader
     *
     * Omitting addDataType() causes: "Unknown struct type: TypeName" at compile time
     * Omitting addGeneratedClass() causes: NoClassDefFoundError at runtime
     */

    // 1. Create and register StructType (for compile-time lookups)
    StructType structType = new StructType(
        typeName,
        decl.getFields(),
        decl.getConstructor(),
        decl.getInstanceMethods(),
        decl.getStaticMethods()
    );
    ctx.addDataType(structType);

    // 2. Generate class bytecode with methods
    byte[] bytecode = generateTypeClass(decl, structType, ctx);

    // 3. Register for runtime loading
    ctx.addGeneratedClass(typeName, bytecode);
}
```

#### 3.2 Type Class Generation (Extend StructClassGenerator)

**Research Insight (Best Practices):** Follow ASM patterns exactly - super() must be called first in constructors.

```java
// Extend existing StructClassGenerator - don't create separate file
public byte[] generateTypeClass(StructDeclaration decl, StructType structType, CompileContext parentCtx) {
    String typeName = decl.getId().getId();
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // 1. Class header (public class TypeName extends Object)
    cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, typeName, null, "java/lang/Object", null);

    // 2. Generate fields (existing struct behavior)
    for (StructFieldDef field : decl.getFields()) {
        String descriptor = TypeMapper.getDescriptor(field.getDataType());
        cw.visitField(ACC_PUBLIC, field.getName().getId(), descriptor, null, null).visitEnd();
    }

    // 3. Generate constructor
    if (decl.getConstructor() != null) {
        generateExplicitConstructor(cw, decl, structType, parentCtx);
    } else {
        generateDefaultConstructor(cw, decl, structType);
    }

    // 4. Generate instance methods
    for (MethodDef method : decl.getInstanceMethods()) {
        generateInstanceMethod(cw, method, typeName, structType, parentCtx);
    }

    // 5. Generate static methods
    for (MethodDef method : decl.getStaticMethods()) {
        generateStaticMethod(cw, method, typeName, parentCtx);
    }

    cw.visitEnd();
    return cw.toByteArray();
}

private void generateExplicitConstructor(ClassWriter cw, StructDeclaration decl,
                                          StructType structType, CompileContext parentCtx) {
    MethodDef ctor = decl.getConstructor();
    String descriptor = buildConstructorDescriptor(ctor.getParameters(), parentCtx);
    String typeName = decl.getId().getId();

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
    mv.visitCode();

    // CRITICAL: super() MUST be called first before any field access (JVM requirement)
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

    // Create context that INHERITS type registrations from parent
    CompileContext ctorContext = parentCtx.createChildContext();
    ctorContext.setClassName(typeName);
    ctorContext.addLocalVariable("this", structType, 0);

    // Resolve and register parameters with proper type lookup
    int slot = 1;
    for (ParameterDef param : ctor.getParameters()) {
        DataType resolvedType = resolveDataType(param.getDataType().getName(), parentCtx);
        ctorContext.addLocalVariable(param.getName(), resolvedType, slot);
        slot += TypeMapper.getSlotSize(resolvedType);
    }

    // Compile constructor body
    CompileVisitor visitor = new CompileVisitor(mv, ctorContext);
    visitor.visit(ctor.getBody());

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);  // Auto-computed with COMPUTE_FRAMES
    mv.visitEnd();
}

private void generateInstanceMethod(ClassWriter cw, MethodDef method, String typeName,
                                     StructType structType, CompileContext parentCtx) {
    String descriptor = buildMethodDescriptor(method, parentCtx);

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, method.getName().getId(), descriptor, null, null);
    mv.visitCode();

    // Create context: 'this' is slot 0, params start at slot 1
    CompileContext methodContext = parentCtx.createChildContext();
    methodContext.setClassName(typeName);
    methodContext.addLocalVariable("this", structType, 0);

    int slot = 1;
    for (ParameterDef param : method.getParameters()) {
        DataType resolvedType = resolveDataType(param.getDataType().getName(), parentCtx);
        methodContext.addLocalVariable(param.getName(), resolvedType, slot);
        slot += TypeMapper.getSlotSize(resolvedType);
    }

    // Compile method body
    CompileVisitor visitor = new CompileVisitor(mv, methodContext);
    visitor.visit(method.getBody());

    // Return instruction based on return type
    DataType returnType = resolveDataType(method.getReturnType().getName(), parentCtx);
    mv.visitInsn(TypeMapper.getReturnOpcode(returnType));

    mv.visitMaxs(0, 0);
    mv.visitEnd();
}

private void generateStaticMethod(ClassWriter cw, MethodDef method, String typeName,
                                   CompileContext parentCtx) {
    String descriptor = buildMethodDescriptor(method, parentCtx);

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, method.getName().getId(),
                                       descriptor, null, null);
    mv.visitCode();

    // No 'this' for static methods - params start at slot 0
    CompileContext methodContext = parentCtx.createChildContext();
    methodContext.setClassName(typeName);

    int slot = 0;
    for (ParameterDef param : method.getParameters()) {
        DataType resolvedType = resolveDataType(param.getDataType().getName(), parentCtx);
        methodContext.addLocalVariable(param.getName(), resolvedType, slot);
        slot += TypeMapper.getSlotSize(resolvedType);
    }

    CompileVisitor visitor = new CompileVisitor(mv, methodContext);
    visitor.visit(method.getBody());

    DataType returnType = resolveDataType(method.getReturnType().getName(), parentCtx);
    mv.visitInsn(TypeMapper.getReturnOpcode(returnType));

    mv.visitMaxs(0, 0);
    mv.visitEnd();
}
```

#### 3.3 Type Resolution Helper

**Research Insight (Learnings):** Must resolve parameter/return types through context lookup. Never use raw type names directly.

```java
// Add to StructClassGenerator or create TypeResolver utility
/**
 * CRITICAL: Standard type resolution pattern (see docs/solutions/.../struct-type-resolution...)
 *
 * All type resolution during compilation must follow this lookup order:
 * 1. System types (fast path for primitives)
 * 2. Custom types (structs/types from context)
 * 3. Error on unknown type
 */
private DataType resolveDataType(String typeName, CompileContext ctx) {
    // 1. Check system types
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }

    // 2. Check custom types
    DataType customType = ctx.getDataType(typeName);
    if (customType != null) {
        return customType;
    }

    // 3. Unknown type - throw error (not silent fallback)
    throw new CompileException("Unknown type: " + typeName);
}

/**
 * Build method descriptor with proper type resolution.
 * Example: func distance(Point other) -> int produces (LPoint;)I
 */
private String buildMethodDescriptor(MethodDef method, CompileContext ctx) {
    StringBuilder desc = new StringBuilder("(");

    for (ParameterDef param : method.getParameters()) {
        DataType paramType = resolveDataType(param.getDataType().getName(), ctx);
        desc.append(TypeMapper.getDescriptor(paramType));
    }

    desc.append(")");

    DataType returnType = resolveDataType(method.getReturnType().getName(), ctx);
    desc.append(TypeMapper.getDescriptor(returnType));

    return desc.toString();
}
```

#### 3.4 Extend TypeMapper for Custom Types

**Research Insight (Framework Docs):** Custom types are reference types - use ALOAD/ASTORE/ARETURN.

```java
// Add to core/src/main/java/com/elminster/jcp/compile/util/TypeMapper.java

public static String getDescriptor(DataType type) {
    if (type instanceof SystemDataType) {
        return getSystemTypeDescriptor((SystemDataType) type);
    } else if (type instanceof StructType) {
        // Custom types: L<classname>;
        return "L" + type.getName() + ";";
    } else {
        return "Ljava/lang/Object;";
    }
}

public static int getLoadOpcode(DataType type) {
    if (type instanceof StructType) {
        return Opcodes.ALOAD;  // All custom types are references
    }
    // ... existing system type logic
}

public static int getStoreOpcode(DataType type) {
    if (type instanceof StructType) {
        return Opcodes.ASTORE;
    }
    // ... existing system type logic
}

public static int getReturnOpcode(DataType type) {
    if (type instanceof StructType) {
        return Opcodes.ARETURN;
    }
    // ... existing system type logic
}

public static int getSlotSize(DataType type) {
    if (type instanceof StructType) {
        return 1;  // References take 1 slot
    }
    // double/long take 2 slots, others take 1
    return (type == SystemDataType.DOUBLE) ? 2 : 1;
}
```

#### 3.5 MethodCallCompiler (Extend Existing)

**Research Insight (Pattern Recognition):** Existing `MethodCallCompiler` may exist - extend it.

```java
// core/src/main/java/com/elminster/jcp/compile/function/MethodCallCompiler.java
// Extend existing or create new in /function/ package to match evaluator location
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    MethodCallExpression expr = (MethodCallExpression) astNode;

    // 1. Compile target object (pushes reference onto stack)
    compileExpression(expr.getExpression(), mv, ctx);

    // 2. Determine struct type from expression
    StructType type = getStructTypeFromExpression(expr.getExpression(), ctx);
    if (type == null) {
        throw new CompileException("Cannot call method on non-struct type");
    }

    // 3. Look up method with validation
    String methodName = expr.getMethodName();
    MethodDef method = type.getInstanceMethod(methodName);
    if (method == null) {
        throw new CompileException("Method '" + methodName + "' not found in type '" + type.getName() + "'");
    }

    // 4. Compile arguments (push each onto stack)
    for (Expression arg : expr.getArguments()) {
        compileExpression(arg, mv, ctx);
    }

    // 5. Emit INVOKEVIRTUAL
    String descriptor = buildMethodDescriptor(method, ctx);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, type.getName(), methodName, descriptor, false);
}
```

#### 3.6 StaticMethodCallCompiler (NEW)

```java
// core/src/main/java/com/elminster/jcp/compile/function/StaticMethodCallCompiler.java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    StaticMethodCallExpression expr = (StaticMethodCallExpression) astNode;

    // 1. Look up type with validation
    String typeName = expr.getTypeName().getId();
    StructType type = (StructType) ctx.getDataType(typeName);
    if (type == null) {
        throw new CompileException("Type not found: " + typeName);
    }

    // 2. Look up static method with validation
    String methodName = expr.getMethodName();
    MethodDef method = type.getStaticMethod(methodName);
    if (method == null) {
        throw new CompileException("Static method '" + methodName + "' not found in type '" + typeName + "'");
    }

    // 3. Compile arguments
    for (Expression arg : expr.getArguments()) {
        compileExpression(arg, mv, ctx);
    }

    // 4. Emit INVOKESTATIC
    String descriptor = buildMethodDescriptor(method, ctx);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, typeName, methodName, descriptor, false);
}
```

#### 3.7 ThisCompiler (NEW)

```java
// core/src/main/java/com/elminster/jcp/compile/struct/ThisCompiler.java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    // 'this' is always local variable slot 0 in instance methods/constructors
    // This is a JVM convention - slot 0 holds the receiver reference
    mv.visitVarInsn(Opcodes.ALOAD, 0);
}
```

### Phase 4: Testing

#### 4.1 Interpreter Tests

```java
// core/src/test/java/com/elminster/jcp/eval/type/TypeEvaluatorTest.java

@Test
void testTypeWithConstructor() {
    // type Point { int x; constructor(int x) { this.x = x; } }
    // Point p = Point(10);
    // assert p.x == 10
}

@Test
void testInstanceMethod() {
    // type Counter { int count; func increment() -> void { this.count = this.count + 1; } }
    // Counter c = Counter(); c.increment(); assert c.count == 1
}

@Test
void testStaticMethod() {
    // type Math { static func add(int a, int b) -> int { return a + b; } }
    // assert Math.add(2, 3) == 5
}

@Test
void testMethodWithCustomTypeParameter() {
    // type Point { int x; int y; }
    // type Line { func contains(Point p) -> boolean { return true; } }
    // Verifies parameter type resolution through context
}

@Test
void testMethodReturningCustomType() {
    // type Point { int x; int y;
    //     static func origin() -> Point { return Point(0, 0); }
    // }
    // Verifies return type resolution and ARETURN opcode
}
```

#### 4.2 Compiler Tests

```java
// core/src/test/java/com/elminster/jcp/compile/type/TypeCompileTest.java

@Test
void testTypeDeclarationGeneratesClass() {
    // Verify class bytecode is generated with methods
}

@Test
void testConstructorBytecode() {
    // Verify constructor creates instance with fields initialized
}

@Test
void testInstanceMethodInvocation() {
    // Verify INVOKEVIRTUAL is emitted
}

@Test
void testStaticMethodInvocation() {
    // Verify INVOKESTATIC is emitted
}
```

#### 4.3 Class Loading Tests (CRITICAL)

**Research Insight (Learnings):** Must test BOTH entry points to prevent rootContext regression.

```java
@Test
void testTypeClassLoadedViaCompile() {
    // Use compile() entry point
    Block program = buildTypeWithMethod();
    JcpCompiler compiler = new JcpCompiler();
    Class<?> clazz = compiler.compileAndLoad(program, "TestProgram");

    // Verify type class was loaded
    Map<String, byte[]> generated = compiler.getGeneratedClasses();
    assertTrue(generated.containsKey("Point"), "Type class must be in generated classes");
}

@Test
void testTypeClassLoadedViaCompileWithReturn() {
    // Use compileWithReturn() entry point - THE ONE THAT HISTORICALLY FAILED
    Block program = buildTypeWithMethod();
    Expression expr = buildMethodCallExpression();
    JcpCompiler compiler = new JcpCompiler();
    Class<?> clazz = compiler.compileWithReturn(program, expr, SystemDataType.INT, "TestReturn");

    // CRITICAL: Verify type class was tracked
    Map<String, byte[]> generated = compiler.getGeneratedClasses();
    assertTrue(generated.containsKey("Point"),
        "BUG: Type class missing from generated classes - rootContext not set!");
}
```

## Acceptance Criteria

### Functional Requirements

- [ ] `type` keyword declares types with fields, constructor, and methods
- [ ] Constructor body executes with `this` bound to new instance
- [ ] Default no-arg constructor auto-generated if none defined
- [ ] Instance methods accessible via `instance.method(args)`
- [ ] Static methods accessible via `TypeName.method(args)`
- [ ] `this` keyword references current instance in methods/constructors
- [ ] `this.field` accesses instance fields
- [ ] Direct field access via `instance.field` still works
- [ ] Return types required with `-> type` syntax
- [ ] Both interpreter and compiler modes produce identical results

### Non-Functional Requirements

- [ ] No performance regression from current struct implementation
- [ ] Field lookup is O(1) via HashMap cache
- [ ] Clear error messages for: method not found, wrong argument types, `this` in static context
- [ ] Bytecode verifiable via `javap`

### Quality Gates

- [ ] All existing struct tests pass (or migrated to type syntax)
- [ ] New tests for constructor, instance methods, static methods
- [ ] Tests for error conditions
- [ ] Both eval and compile modes tested
- [ ] **Type classes loaded via `compile()` entry point**
- [ ] **Type classes loaded via `compileWithReturn()` entry point (CRITICAL)**
- [ ] `getGeneratedClasses()` returns all type classes after compilation

## Dependencies & Prerequisites

- Current struct implementation working (✅ Complete)
- Multi-class loading via `MultiClassLoader` (✅ Complete)
- Function body compilation pattern (✅ Complete in `FunctionDeclarationCompiler`)

## Risk Analysis & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing struct code | High | Medium | Provide clear migration (just change `struct` to `type`) |
| Constructor body compilation complexity | Medium | High | Follow `FunctionDeclarationCompiler` pattern exactly |
| Method resolution conflicts with fields | Medium | Medium | Require `()` for method calls; fields have no parens |
| `this` scope leakage | Low | High | Strict validation that `this` only exists in method/constructor contexts |
| **Type class not loaded at runtime** | **HIGH** | **CRITICAL** | Verify `rootContext` set in ALL `BytecodeGenerator` entry points. Test BOTH `compile()` AND `compileWithReturn()` |
| O(n) field lookup performance | Medium | Medium | Fix in Phase 0 before adding methods |
| Forgetting dual registration | Medium | High | Add explicit comments; add integration tests |

## Migration Path

Existing struct code:
```jcp
struct Point { int x; int y; }
Point p = Point(10, 20);
```

Becomes:
```jcp
type Point { int x; int y; }
Point p = Point(10, 20);
```

The only required change is `struct` → `type`. Field-only types with positional constructor work identically.

## File Changes Summary (KISS)

### New Files (5 only)

| File | Purpose |
|------|---------|
| `ast/statement/declaration/MethodDef.java` | Unified method/constructor definition |
| `ast/expression/StaticMethodCallExpression.java` | Static method call AST node |
| `ast/expression/ThisExpression.java` | `this` keyword AST node |
| `eval/function/StaticMethodCallEvaluator.java` | Static method call evaluator (uses function lookup) |
| `eval/struct/ThisEvaluator.java` | `this` keyword evaluator |

### Modified Files (6)

| File | Change |
|------|--------|
| `ast/statement/declaration/StructDeclarationImpl.java` | Add method fields for AST representation |
| `eval/data/StructType.java` | Add field cache (HashMap), constructor storage |
| `eval/declare/TypeDeclarationEvaluator.java` | Register methods as `AbstractFunction` with qualified name |
| `eval/function/MethodCallEvaluator.java` | Simplified - use function lookup by qualified name |
| `eval/function/FunctionEvaluator.java` | Small fix - bind `this` directly (not clone) |
| `compile/StructClassGenerator.java` | Add method generation |

### Key Architecture (KISS)

**Type methods ARE functions.** No new wrapper classes needed.

**Registration Pattern:**
- Instance method `Counter.getCount()` → registered as `AbstractFunction` with name `user::Counter.getCount` and first param `this: Counter`
- Static method `Math.add(int, int)` → registered as `AbstractFunction` with name `user::Math.add`

**Lookup Pattern:**
- `MethodCallEvaluator` builds qualified name: `FunctionUtils.getModuleFunctionName("user", typeName, methodName)`
- Looks up via `evalContext.getFunction(fullName)`
- Same as existing module function lookup in `ClassConverter`

## References & Research

### Internal References

- Existing struct implementation: `core/src/main/java/com/elminster/jcp/ast/statement/declaration/StructDeclaration*.java`
- Struct class generation: `core/src/main/java/com/elminster/jcp/compile/StructClassGenerator.java:72-119`
- Function body compilation: `core/src/main/java/com/elminster/jcp/compile/declare/FunctionDeclarationCompiler.java:50-68`
- Type resolution pattern: `core/src/main/java/com/elminster/jcp/compile/struct/FieldAccessCompiler.java:75-125`
- Factory pattern: `core/src/main/java/com/elminster/jcp/eval/factory/AstEvaluatorFactory.java:52-70`
- **Existing MethodCallEvaluator**: `core/src/main/java/com/elminster/jcp/eval/function/MethodCallEvaluator.java`

### Institutional Learnings

- **Dual registration required**: `ctx.addDataType()` + `ctx.addGeneratedClass()` (docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md)
- **rootContext must be set**: All bytecode generation must use instance field (docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md)
- **Type resolution order**: System types → Custom types → Fallback (docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md)

### External Research (ASM Best Practices)

- **COMPUTE_FRAMES | COMPUTE_MAXS** is correct for code generation
- **super() must be called first** in constructors before any field access
- **Slot 0 = `this`** in instance methods, params start at slot 1
- **INVOKEVIRTUAL** for instance methods, **INVOKESTATIC** for static methods
- **visitMaxs(0, 0)** is correct when using COMPUTE_MAXS/COMPUTE_FRAMES

### Brainstorm Document

- `docs/brainstorms/2026-02-05-type-system-refinement-brainstorm.md`

### Security Considerations

**Research Insight (Security Sentinel):** Consider adding in future iterations:
- Input validation on type/method names
- Resource limits on class generation
- Circular reference detection
