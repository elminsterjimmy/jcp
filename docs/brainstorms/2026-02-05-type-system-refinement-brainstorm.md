# Type System Refinement Brainstorm

**Date:** 2026-02-05
**Status:** Ready for planning
**Related:** Current struct implementation, ClassConverter.java pattern

## What We're Building

A refined custom type system that supports:
- **Fields** (existing struct behavior)
- **Explicit constructors** with custom logic
- **Instance methods** (true methods on the class, called via dot notation)
- **Static methods** (called via `TypeName.method()`)

This replaces the current struct implementation, extending it rather than rewriting.

### Target Syntax

```
type A {
    int a;
    string s;

    constructor(int x, string y) {
        this.a = x;
        this.s = y;
    }

    func func1(int n, string m) {
        // instance method - has access to 'this'
    }

    static func sfunc1() {
        // static method - no 'this' access
    }
}

// Usage:
A instance = A(10, "hello");    // constructor call
instance.func1(5, "world");     // instance method call
A.sfunc1();                     // static method call
```

## Why This Approach

### Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Method dispatch** | Hybrid (static truly static, instance on class) | Instance methods need per-instance context; INVOKEVIRTUAL for instance, INVOKESTATIC for static |
| **Constructor style** | Explicit body | Custom initialization logic required (not just field assignment) |
| **Method call syntax** | Dot notation (`instance.method()`) | Standard OOP style, intuitive |
| **Static call syntax** | `TypeName.method()` | Java/C#-style, clear distinction from instance methods |
| **Visibility** | All public | YAGNI - add visibility modifiers later if needed |
| **Migration** | Replace struct entirely | One concept, simpler mental model |

### Reference Implementation: ClassConverter.java

The `ClassConverter.java` pattern shows how Java classes are registered:
- Static methods: registered as module functions
- Instance methods: registered with `this` as first parameter
- Constructors: registered as `TypeName.new()` functions

For compiled mode, we'll generate actual JVM methods instead of wrapper functions.

## Key Technical Details

### Interpreter Mode (eval/)

1. **TypeDeclaration** stores: fields, constructor body, methods (instance + static)
2. **TypeData** (evolved from StructData): holds field values + reference to type definition
3. **Method invocation**: Look up method in type definition, create local scope with `this` bound
4. **Static invocation**: Look up method, no `this` binding

### Compiler Mode (compile/)

1. **StructClassGenerator** extended to:
   - Generate constructor bytecode from explicit body (not just field init)
   - Generate instance method bytecode (non-static JVM methods)
   - Generate static method bytecode (static JVM methods)

2. **Method calls**:
   - Instance: `INVOKEVIRTUAL TypeName/methodName(descriptor)`
   - Static: `INVOKESTATIC TypeName/methodName(descriptor)`

3. **`this` keyword**: In instance methods, `this` is local variable slot 0 (automatic in JVM)

### AST Changes

| Current | Becomes |
|---------|---------|
| `StructDeclaration` | `TypeDeclaration` (or rename) |
| `StructFieldDef` | Keep as is |
| `StructInstantiation` | `TypeInstantiation` or constructor call |
| N/A | `MethodDef` (new) |
| N/A | `ConstructorDef` (new) |
| N/A | `MethodCall` (new, for `obj.method()`) |
| N/A | `StaticMethodCall` (new, for `Type.method()`) |

## MVP Scope

For the first implementation:
- ✅ Field definitions
- ✅ Explicit constructor with body
- ✅ Instance methods with `this` access
- ✅ Static methods
- ✅ Dot notation for method calls
- ❌ Method overloading (defer)
- ❌ Visibility modifiers (defer)
- ❌ Inheritance (out of scope - data + behavior only)

## Open Questions

1. **Parser changes**: How much parser work is needed for the new `type` syntax? (Assuming parser exists)
2. **`this` resolution**: Explicit `this` keyword needed, or implicit field access?
3. **Return types**: Should methods require explicit return type declaration?

## Risks

- **Breaking change**: Removing struct breaks existing code (if any)
- **Bytecode complexity**: Constructor with custom body needs careful local variable management
- **Method resolution**: Need to handle `obj.method()` differently from `obj.field`

## Next Steps

Run `/workflows:plan` to create implementation plan covering:
1. AST node changes/additions
2. Evaluator implementation
3. Compiler implementation (bytecode generation for methods)
4. Test coverage
