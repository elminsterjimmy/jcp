[← Back to README](../README.md)

# Language Reference

This document describes the syntax and features of JCP's DSL layer — the language constructs you use when writing programs that JCP interprets or compiles to JVM bytecode.

For how to connect your own parser to JCP and execute programs, see the [Integration Guide](integration-guide.md).

## Variables and Types

JCP supports a hierarchical type system with the following primitive types:

```
int x = 10;
string name = "JCP";
boolean flag = true;
```

The built-in type hierarchy:
- `ANY` (root) → `STRING`, `BOOLEAN`, `VOID`
- `ANY` → `NUMERIC` → `INT`, `DOUBLE`
- Array variants: `INT_ARRAY`, `BOOLEAN_ARRAY`, `STRING_ARRAY`

Type compatibility is checked via `isCastableTo()` — for example, `INT` is castable to `NUMERIC` and `ANY`.

## Control Flow

### If/Else

```
if (x > 5) {
    // ...
} else {
    // ...
}
```

### While Loop

```
while (x > 0) {
    x = x - 1;
}
```

### For Loop

```
for (int i = 0; i < 10; i = i + 1) {
    // ...
}
```

## Operators

### Arithmetic

`+`, `-`, `*`, `/`, `%` (modulo)

### Relational

`==`, `!=`, `<`, `<=`, `>`, `>=`

### Logical

`&&` (and), `||` (or), `!` (not)

## Functions

Functions are declared with `func`, parameter types, and a return type:

```
func add(int a, int b) -> int {
    return a + b;
}

int result = add(3, 5);
```

JCP supports **function overloading** — multiple functions can share the same name if their parameter types differ. Resolution uses type compatibility: an exact type match is preferred over widening (e.g. `foo(int)` is preferred over `foo(double)` when called with an `int` argument).

## Structs

Custom data structures with fields, instance methods, and static methods:

```
struct Point {
    int x;
    int y;
}

Point p = Point(10, 20);
int xVal = p.x;
```

Structs can also define methods:

```
struct Counter {
    int count;

    func get() -> int {
        return count;
    }

    static func zero() -> int {
        return 0;
    }
}
```

## Modules and Type Disambiguation

JCP's module system lets you register external Java classes as callable types. When two modules expose the same simple name (e.g. both the base module and Guava provide a `Strings` class), use the `module::Type` prefix to disambiguate:

```
// Unambiguous — only one "Math" is registered
int abs = Math.abs(-5);

// Ambiguous — two "Strings" classes exist, use module:: prefix
string repeated = guava::Strings.repeat("ab", 3);
string len = base::Strings.length("hello");
```

Resolution uses a three-tier strategy:
1. **Simple name** — works when unambiguous (only one class has that name)
2. **`module::SimpleName`** — e.g. `guava::Strings` disambiguates by module
3. **Fully-qualified name** — e.g. `com.google.common.base.Strings`

For the Java-side API to register modules, see the [Integration Guide](integration-guide.md#module-registration) and the [Custom Module Functions Guide](guides/custom-module-functions.md).

## See Also

- [Integration Guide](integration-guide.md) — Connect your DSL to JCP, execute programs
- [Custom Module Functions](guides/custom-module-functions.md) — Register Java classes, type mapping, JAR imports
- [Architecture](architecture/jcp-function-architecture.md) — Function system internals
- [Debugger API](debugger.md) — Breakpoints, stepping, variable inspection
