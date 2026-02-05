---
title: Struct Type Resolution Failing in Variable Declarations
date: 2026-02-05
category: logic-errors
tags:
  - type-resolution
  - struct-types
  - compilation
  - context-management
module: jcp-core
component: VariableDeclarationCompiler
severity: medium
status: resolved
symptoms:
  - "Cannot determine struct type for field access"
  - Variables with struct types defaulting to ANY type
  - Field access compilation failing to look up struct type from variable
---

# Struct Type Resolution Failing in Variable Declarations

## Problem Summary

When compiling struct variable declarations, `VariableDeclarationCompiler` was not properly resolving custom struct types from the compile context. Variables were being typed as `SystemDataType.ANY` instead of the actual `StructType`, causing downstream field access operations to fail with "Cannot determine struct type for field access" errors.

## Symptoms

**Error Message:**
```
java.lang.IllegalArgumentException: Cannot determine struct type for field access: x
    at com.elminster.jcp.compile.struct.FieldAccessCompiler.compile(FieldAccessCompiler.java:45)
```

**Observable Behavior:**
- Struct instantiation worked correctly
- Variable declaration accepted the struct instance
- Field access on that variable failed
- Error occurred during compilation, not at runtime

**Example Failing Code:**
```java
// This compiled successfully
struct Point { int x; int y; }
Point p = Point(10, 20);

// This failed with "Cannot determine struct type"
int xVal = p.x;  // ❌ Compilation error
```

## Investigation Steps

### 1. Initial Debugging
**Hypothesis:** Field access compiler couldn't determine struct type from expression

**Tried:** Added handling for `VariableExpression` in `getStructTypeFromExpression()`
```java
if (expr instanceof com.elminster.jcp.ast.expression.base.VariableExpression) {
    String varName = ((VariableExpression) expr).getId().getId();
    CompileContext.LocalVariable local = ctx.getLocal(varName);
    if (local != null && local.getType() instanceof StructType) {
        return (StructType) local.getType();
    }
}
```

**Result:** Still failed - `local.getType()` was not a `StructType`

### 2. Context Investigation
**Checked:** What type was actually stored for the variable?

```java
CompileContext.LocalVariable local = ctx.getLocal("p");
System.out.println(local.getType());  // Prints: ANY
```

**Discovery:** Variable was stored with `SystemDataType.ANY` instead of `StructType`!

### 3. Root Cause Discovery
**Traced back to:** `VariableDeclarationCompiler.resolveDataType()`

```java
private DataType resolveDataType(String typeName) {
    // Try to match system data types
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }
    // Default to ANY
    return SystemDataType.ANY;  // ❌ No custom type lookup!
}
```

**The bug:** Method only checked system types, never looked up custom types (structs) from context.

## Root Cause

**Technical Explanation:**

1. User code declares: `Point p = Point(10, 20);`
2. Parser creates `VariableDeclarationImpl` with `dataType = new DataTypeImpl("Point")`
3. `VariableDeclarationCompiler` calls `resolveDataType("Point")`
4. Method loops through `SystemDataType.values()` - no match for "Point"
5. Returns `SystemDataType.ANY` as default
6. Variable stored in context as: `{name: "p", type: ANY, index: 0}`
7. Later, `FieldAccessCompiler` tries to access `p.x`
8. Looks up variable, gets type `ANY`, can't cast to `StructType`
9. Returns null from `getStructTypeFromExpression()`
10. Throws "Cannot determine struct type for field access"

**Why struct declaration worked:**
- `StructDeclarationCompiler` properly registers the `StructType` in context
- The type EXISTS in context, just not being looked up during variable declaration

## Solution

### Fix: Look Up Custom Types from Context

**File:** `core/src/main/java/com/elminster/jcp/compile/declare/VariableDeclarationCompiler.java`

**Before (buggy):**
```java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    VariableDeclaration varDecl = (VariableDeclaration) astNode;
    Identifier id = varDecl.getId();
    String varName = id.getId();

    // Get the data type
    DataType dataType = resolveDataType(varDecl.getDataType().getName());  // ❌ No context

    // Allocate a local variable slot
    int localIndex = ctx.allocateLocal(varName, dataType);
    // ...
}

private DataType resolveDataType(String typeName) {
    // Try to match system data types
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }
    // Default to ANY
    return SystemDataType.ANY;  // ❌ Never checks custom types
}
```

**After (fixed):**
```java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    VariableDeclaration varDecl = (VariableDeclaration) astNode;
    Identifier id = varDecl.getId();
    String varName = id.getId();

    // Get the data type
    DataType dataType = resolveDataType(varDecl.getDataType().getName(), ctx);  // ✅ Pass context

    // Allocate a local variable slot
    int localIndex = ctx.allocateLocal(varName, dataType);
    // ...
}

private DataType resolveDataType(String typeName, CompileContext ctx) {
    // Try to match system data types
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }

    // Try to look up custom types (like structs) from context
    DataType customType = ctx.getDataType(typeName);  // ✅ Look up in context
    if (customType != null) {
        return customType;
    }

    // Default to ANY
    return SystemDataType.ANY;
}
```

## Verification

**Before Fix:**
```
Tests run: 7, Failures: 0, Errors: 6, Skipped: 0
```
- 6 tests failing with "Cannot determine struct type"

**After Fix:**
```
Tests run: 7, Failures: 0, Errors: 4, Skipped: 0
```
- Reduced to 4 failures (different issue with class loading)
- All struct type resolution issues resolved

## Prevention

### 1. Type Resolution Pattern

**Rule:** When resolving type names during compilation, always check custom types from context after checking system types.

**Standard pattern:**
```java
private DataType resolveDataType(String typeName, CompileContext ctx) {
    // 1. Check system types (fast path for primitives)
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }

    // 2. Check custom types (structs, functions, etc.)
    DataType customType = ctx.getDataType(typeName);
    if (customType != null) {
        return customType;
    }

    // 3. Default fallback
    return SystemDataType.ANY;
}
```

### 2. Context Passing

**Lesson:** Type resolution requires context access. Always pass `CompileContext` to resolution methods.

**Anti-pattern:**
```java
DataType resolveDataType(String name)  // ❌ No context access
```

**Correct pattern:**
```java
DataType resolveDataType(String name, CompileContext ctx)  // ✅ Can look up custom types
```

### 3. Test Coverage

**Add tests for custom type resolution:**
```java
@Test
void testCustomTypeResolution() {
    // Register custom type
    StructType pointType = new StructType("Point", fields);
    ctx.addDataType(pointType);

    // Declare variable with custom type
    VariableDeclaration decl = new VariableDeclarationImpl("p",
        new DataTypeImpl("Point"), initializer);

    // Compile and verify type stored correctly
    compiler.compile(mv, ctx);
    LocalVariable var = ctx.getLocal("p");
    assertTrue(var.getType() instanceof StructType);
    assertEquals("Point", var.getType().getName());
}
```

### 4. Consistent Type System

**Design principle:** All type resolution should follow the same lookup order:
1. System types (primitives)
2. Custom types (user-defined)
3. Fallback (ANY)

This pattern should be consistent across:
- Variable declarations
- Function parameters
- Return types
- Field types
- Any other type resolution

## Related Issues

**Similar Type Resolution Issues:**
- Custom function types would have same problem
- Array types of custom structs (`Point[]`) need special handling
- Generic types require type parameter resolution

**Context Management Patterns:**
- Always pass context when resolution involves runtime data
- Use context inheritance for nested scopes
- Register types during declaration phase, look up during usage phase

## Impact

**Severity:** Medium
- Blocked struct field access compilation
- Required to fix before field compilers could be tested
- Cascade failure: without this fix, all field operations failed

**Resolution Time:** ~15 minutes (quick once pattern identified)

**Dependencies:** Required before testing:
- FieldAccessCompiler
- FieldAssignmentCompiler
- Any code using struct variables

## Keywords

type resolution, custom types, struct types, VariableDeclarationCompiler, CompileContext, getDataType, field access, type system, context lookup

## See Also

- `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md` - Related class loading issue
- `core/src/main/java/com/elminster/jcp/compile/declare/VariableDeclarationCompiler.java:57` - Fix location
- `core/src/main/java/com/elminster/jcp/compile/context/CompileContext.java` - Context API
