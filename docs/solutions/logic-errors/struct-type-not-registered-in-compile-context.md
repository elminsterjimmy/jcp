---
title: Struct Types Not Registered in Compile Context During Declaration
date: 2026-02-05
category: logic-errors
tags:
  - struct-declaration
  - type-registration
  - compilation
  - context-management
module: jcp-core
component: StructDeclarationCompiler
severity: high
status: resolved
symptoms:
  - "Unknown struct type: Point"
  - Struct instantiation failing immediately after declaration
  - Type exists in eval mode but not in compile mode
---

# Struct Types Not Registered in Compile Context During Declaration

## Problem Summary

`StructDeclarationCompiler` was generating struct class bytecode but not registering the `StructType` in the compile context. This caused immediate failures when trying to instantiate the struct, as `StructInstantiationCompiler` couldn't look up the type to get field definitions.

## Symptoms

**Error Message:**
```
java.lang.IllegalArgumentException: Unknown struct type: Point
    at com.elminster.jcp.compile.struct.StructInstantiationCompiler.compile(StructInstantiationCompiler.java:43)
```

**Observable Behavior:**
- Struct declaration appeared to succeed
- Struct class bytecode was generated
- Immediate instantiation failed with "Unknown struct type"
- Problem occurred at compile time, not runtime

**Example Failing Code:**
```java
struct Point { int x; int y; }    // ✅ Declares successfully
Point p = Point(10, 20);          // ❌ Fails: Unknown struct type: Point
```

## Investigation Steps

### 1. Initial Error Analysis
**First hypothesis:** StructInstantiationCompiler not implemented correctly

**Checked:** `StructInstantiationCompiler.compile()` implementation:
```java
public void compile(MethodVisitor mv, CompileContext ctx) {
    StructInstantiation structInst = (StructInstantiation) astNode;
    String structName = structInst.getStructType().getId();

    // Look up struct type to get field definitions
    StructType structType = (StructType) ctx.getDataType(structName);  // ❌ Returns null
    if (structType == null) {
        throw new IllegalArgumentException("Unknown struct type: " + structName);
    }
    // ...
}
```

**Discovery:** `ctx.getDataType(structName)` returned `null` - type not registered!

### 2. Comparison with Eval Mode
**Checked:** How eval mode handles this

```java
// StructDeclarationEvaluator.java
public Data eval(EvalContext evalContext) {
    StructDeclaration structDecl = (StructDeclaration) astNode;
    String structName = structDecl.getId().getId();

    // Create and register the struct type
    StructType structType = new StructType(structName, structDecl.getFields());
    evalContext.addDataType(structType);  // ✅ Registers type in context

    return AnyData.EMPTY;
}
```

**Discovery:** Eval mode properly calls `addDataType()`, compile mode doesn't!

### 3. Root Cause Discovery
**Checked:** What `StructDeclarationCompiler` was actually doing

```java
// StructDeclarationCompiler.java (BEFORE FIX)
public void compile(MethodVisitor mv, CompileContext ctx) {
    StructDeclaration structDecl = (StructDeclaration) astNode;
    String structName = structDecl.getId().getId();

    // Generate the struct class bytecode
    StructClassGenerator generator = new StructClassGenerator();
    byte[] structBytecode = generator.generateStructClass(structName, structDecl.getFields());

    // Register the struct class in the context for later loading
    ctx.addGeneratedClass(structName, structBytecode);  // ✅ Adds bytecode

    // No bytecode needed in the main method for the declaration itself
    // The struct class will be loaded alongside the main class
}
```

**The bug:** Code only called `addGeneratedClass()` (stores bytecode) but never called `addDataType()` (registers type metadata).

## Root Cause

**Technical Explanation:**

The compiler maintains TWO separate data structures:

1. **Generated Classes Map** (`ctx.addGeneratedClass()`)
   - Stores bytecode of generated classes
   - Used for loading classes at runtime
   - Maps: `String className → byte[] bytecode`

2. **Type Registry** (`ctx.addDataType()`)
   - Stores type metadata during compilation
   - Used for type checking and field lookup
   - Maps: `String typeName → DataType instance`

**The disconnect:**

1. `StructDeclarationCompiler` only updated #1 (generated classes)
2. Didn't update #2 (type registry)
3. Later compilers (`StructInstantiationCompiler`, `VariableDeclarationCompiler`, `FieldAccessCompiler`) need #2 to:
   - Look up field definitions
   - Type check field values
   - Generate correct bytecode descriptors
4. Without type in registry, all downstream operations fail

**Why eval mode worked:**
- `StructDeclarationEvaluator` properly calls `evalContext.addDataType()`
- Type is available for all subsequent evaluation
- This pattern wasn't carried over to compile mode

## Solution

### Fix: Register Type During Declaration

**File:** `core/src/main/java/com/elminster/jcp/compile/declare/StructDeclarationCompiler.java`

**Before (buggy):**
```java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    StructDeclaration structDecl = (StructDeclaration) astNode;
    String structName = structDecl.getId().getId();

    // Generate the struct class bytecode
    StructClassGenerator generator = new StructClassGenerator();
    byte[] structBytecode = generator.generateStructClass(structName, structDecl.getFields());

    // Register the struct class in the context for later loading
    ctx.addGeneratedClass(structName, structBytecode);  // ✅ Bytecode stored

    // ❌ MISSING: Type not registered in context!
}
```

**After (fixed):**
```java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    StructDeclaration structDecl = (StructDeclaration) astNode;
    String structName = structDecl.getId().getId();

    // Create and register the struct type in the context
    // This is needed so that StructInstantiationCompiler can look up the type
    com.elminster.jcp.eval.data.StructType structType =
        new com.elminster.jcp.eval.data.StructType(structName, structDecl.getFields());
    ctx.addDataType(structType);  // ✅ Type registered for lookups

    // Generate the struct class bytecode
    StructClassGenerator generator = new StructClassGenerator();
    byte[] structBytecode = generator.generateStructClass(structName, structDecl.getFields());

    // Register the struct class in the context for later loading
    ctx.addGeneratedClass(structName, structBytecode);  // ✅ Bytecode stored

    // No bytecode needed in the main method for the declaration itself
    // The struct class will be loaded alongside the main class
}
```

**Key insight:** Both `addDataType()` AND `addGeneratedClass()` are needed:
- `addDataType()` → Compile-time type checking and lookups
- `addGeneratedClass()` → Runtime class loading

## Verification

**Before Fix:**
```
Tests run: 7, Failures: 0, Errors: 7, Skipped: 0
```
- All tests failing with "Unknown struct type"
- No struct operations worked

**After Fix:**
```
Tests run: 7, Failures: 0, Errors: 6, Skipped: 0
```
- Struct instantiation now works
- Failures moved to next issue (field access type resolution)

## Prevention

### 1. Dual Registration Pattern

**Rule:** Declaration compilers for custom types must update BOTH registries:

```java
// Pattern for custom type declarations
public void compile(MethodVisitor mv, CompileContext ctx) {
    // 1. Create type metadata
    CustomType typeMetadata = new CustomType(name, fields);

    // 2. Register for compile-time lookups
    ctx.addDataType(typeMetadata);  // ✅ Type registry

    // 3. Generate bytecode
    byte[] bytecode = generator.generate(typeMetadata);

    // 4. Register for runtime loading
    ctx.addGeneratedClass(name, bytecode);  // ✅ Class registry
}
```

### 2. Parallel with Eval Mode

**Lesson:** When implementing compile mode for a feature that exists in eval mode, check what the evaluator does and ensure parity.

**Checklist when porting eval → compile:**
- [ ] Does evaluator call `evalContext.addDataType()`?
- [ ] If yes, compiler must call `ctx.addDataType()` too
- [ ] Does evaluator update any other context state?
- [ ] Ensure compiler maintains same invariants

### 3. Integration Testing

**Test pattern:** Verify complete declaration → usage flow
```java
@Test
void testStructDeclarationAndInstantiation() {
    Block program = new BlockImpl();

    // Declare struct
    program.addStatement(new StructDeclarationImpl("Point", fields));

    // Immediately use it (this would fail without type registration)
    program.addStatement(new VariableDeclarationImpl("p",
        new DataTypeImpl("Point"),
        new StructInstantiation("Point", values)));

    // Should compile without errors
    compiler.compileToMultipleClasses(program, "Test");
}
```

### 4. Documentation

**Add comment at key decision points:**
```java
/**
 * IMPORTANT: Must register type in context before any usage.
 * Both addDataType() and addGeneratedClass() are required:
 * - addDataType(): Enables compile-time type checking and lookups
 * - addGeneratedClass(): Enables runtime class loading
 */
```

## Related Issues

**Similar Registration Issues:**
- Function declarations need type registration for call compilation
- Enum declarations need type registration for value access
- Any custom type needs this dual registration pattern

**Context State Management:**
- Declaration order matters: types must be registered before use
- Forward references require two-pass compilation
- Context inheritance for nested scopes

## Impact

**Severity:** High
- Complete blocker for struct compilation feature
- First issue encountered during testing
- Cascade failure: blocked all downstream testing

**Resolution Time:** ~10 minutes (once pattern from eval mode checked)

**Scope:** Affected all struct compilation:
- Struct instantiation
- Variable declarations with struct types
- Field access
- Field assignment

## Keywords

struct declaration, type registration, StructDeclarationCompiler, addDataType, compile context, type registry, custom types, eval mode parity

## See Also

- `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md` - Related issue
- `core/src/main/java/com/elminster/jcp/compile/declare/StructDeclarationCompiler.java:21` - Fix location
- `core/src/main/java/com/elminster/jcp/eval/declare/StructDeclarationEvaluator.java` - Eval mode reference
- `core/src/main/java/com/elminster/jcp/compile/context/CompileContext.java` - Context API
