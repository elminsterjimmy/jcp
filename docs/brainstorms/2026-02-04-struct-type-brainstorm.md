# Brainstorm: Simple Struct Type

**Date:** 2026-02-04
**Status:** Ready for planning
**Feature:** 2 of 3 (independent)

## What We're Building

Add simple struct (record) types to JCP - named types with typed fields, no methods. Support both interpret and compile modes.

## Scope

- Struct type declaration (define a new type with fields)
- Struct instantiation (create instance with field values)
- Field access (read field value)
- Field assignment (write field value)

## Example (conceptual)

```
struct Point {
    int x;
    int y;
}

Point p = Point(10, 20);  // or new Point(10, 20)
int xVal = p.x;
p.y = 30;
```

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Field mutability | Mutable by default | Simpler, matches Java fields |
| Methods | Not supported | Keep it simple (records only) |
| Inheritance | Not supported | Keep it simple |
| Nested structs | Supported | Field type can be another struct |

## Implementation Notes

### AST Nodes Needed

- `StructDeclaration` - defines struct type and its fields
- `StructFieldDef` - single field definition (name + type)
- `StructInstantiation` - creates new struct instance
- `FieldAccessExpression` - reads `obj.field`
- `FieldAssignmentExpression` - writes `obj.field = value`

### Eval Mode

1. `StructType extends DataTypeImpl` - custom type for structs
2. `StructData extends AnyData<Map<String, Data>>` - holds field values
3. Register struct type in context on declaration
4. Validate field types on instantiation
5. Field access returns the stored `Data` value

### Compile Mode

**Approach:** Generate a JVM class for each struct type.

For struct `Point { int x; int y; }`:
```java
// Generated class
public class Point {
    public int x;
    public int y;
    public Point(int x, int y) { this.x = x; this.y = y; }
}
```

Bytecode operations:
- Declaration: `ClassWriter` generates new class bytes
- Instantiation: `NEW`, `DUP`, `INVOKESPECIAL <init>`
- Field read: `GETFIELD`
- Field write: `PUTFIELD`

**Challenge:** Need to generate multiple classes (main + struct classes).

## Files to Create/Modify

**New AST nodes:**
- [x] `ast/statement/declaration/StructDeclaration.java`
- [x] `ast/statement/declaration/StructFieldDef.java`
- [x] `ast/statement/declaration/StructDeclarationImpl.java`
- [x] `ast/expression/StructInstantiation.java`
- [x] `ast/expression/FieldAccessExpression.java`
- [x] `ast/expression/FieldAssignmentExpression.java`

**New eval classes:**
- [x] `eval/data/StructType.java`
- [x] `eval/data/StructData.java`
- [x] `eval/declare/StructDeclarationEvaluator.java`
- [x] `eval/struct/StructInstantiationEvaluator.java`
- [x] `eval/struct/FieldAccessEvaluator.java`
- [x] `eval/struct/FieldAssignmentEvaluator.java`

**New compile classes:**
- `compile/declaration/StructDeclarationCompiler.java`
- `compile/struct/StructInstantiationCompiler.java`
- `compile/struct/FieldAccessCompiler.java`
- `compile/struct/FieldAssignmentCompiler.java`

**Modified files:**
- `compile/BytecodeGenerator.java` - manage multiple class generation
- `compile/JcpCompiler.java` - return multiple class files

## Open Questions

1. How to handle struct type references before declaration (forward references)?
2. Should struct fields have default values?
3. How to manage generated class loading in compile mode?

## Progress

### Completed ✅
- [x] AST nodes for struct declaration, instantiation, field access, field assignment
- [x] StructType and StructData for eval mode
- [x] All evaluators for struct operations
- [x] Eval mode tests (5/5 passing)
- [x] Variable storage correctly preserves struct type

### Remaining Work (Compile Mode)

Compile mode requires significant additional infrastructure:

**StructDeclarationCompiler:**
- Use `ClassWriter` to generate a new JVM class for each struct
- Add public fields for each struct field
- Generate a constructor that initializes all fields
- Store generated class bytes for later loading

**JcpCompiler/BytecodeGenerator modifications:**
- Support generating multiple classes (main + struct classes)
- Implement class dependency resolution
- Create custom `ClassLoader` that can load multiple related classes
- Return `Map<String, byte[]>` instead of single `byte[]`

**StructInstantiationCompiler:**
- Emit `NEW` opcode for struct class
- Emit `DUP` for constructor call
- Compile field value expressions
- Emit `INVOKESPECIAL <init>` to call constructor

**FieldAccessCompiler:**
- Compile object expression
- Emit `GETFIELD` with correct descriptor

**FieldAssignmentCompiler:**
- Compile object expression
- Compile value expression
- Emit `PUTFIELD` with correct descriptor

**Challenges:**
1. Class generation order (structs must be generated before main class)
2. Type descriptors for custom struct types (e.g., "LPoint;")
3. Circular dependencies between struct types
4. ClassLoader that can find and load generated struct classes

This is a substantial undertaking that would best be done as a separate focused effort after eval mode is validated in production use.
