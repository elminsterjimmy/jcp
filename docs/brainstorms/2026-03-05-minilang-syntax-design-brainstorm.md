# MiniLang Syntax Design Brainstorm

**Date:** 2026-03-05
**Status:** Brainstormed
**Related Issue:** #29 - feat: Reference DSL Sample for JCP Integration

## What We're Building

A reference DSL called **MiniLang** that demonstrates how to integrate with JCP. This serves as:
- **End-user documentation** by example
- **Onboarding guide** for DSL authors
- **Test bed** for JCP features like debugger expression evaluation

MiniLang will showcase ALL JCP capabilities:
- Primitives (int, double, boolean, string)
- Variables with explicit type declarations
- Arithmetic, comparison, and logical operators
- Control flow (if/else, while, break, continue, return)
- User-defined functions with overloading and recursion
- Custom structs with fields and methods
- Module integration (Java interop)
- Both interpreter AND compiler execution modes

## Syntax Philosophy

**Design Goal:** Minimalist Python/Ruby-inspired syntax that's clean, readable, and educational.

**Key Principles:**
1. **Explicit types** - always annotate variable and function types for learning clarity
2. **Newline-terminated** - no semicolons required
3. **Braces for blocks** - clear structure without indentation complexity
4. **Minimal keywords** - `let`, `func`, `struct`, `if`, `while`, `return`, `break`, `continue`
5. **Clean function syntax** - `func name(params) -> type { body }`

## Syntax Specification

### Variables

```minilang
# Declaration with initialization
let x: int = 10
let name: string = "MiniLang"
let flag: boolean = true
let pi: double = 3.14159

# Assignment
x = 20
name = "Updated"
```

### Functions

```minilang
# Function with return value
func add(a: int, b: int) -> int {
    return a + b
}

# Void function
func greet(name: string) -> void {
    print("Hello, " + name)
}

# Recursive function
func factorial(n: int) -> int {
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}

# Overloaded functions (same name, different signatures)
func max(a: int, b: int) -> int {
    if a > b {
        return a
    }
    return b
}

func max(a: double, b: double) -> double {
    if a > b {
        return a
    }
    return b
}
```

### Control Flow

```minilang
# If/else
if x > 5 {
    print("Large")
} else {
    print("Small")
}

# While loop
let i: int = 0
while i < 10 {
    print(i)
    i = i + 1
}

# Loop control
while true {
    if i > 100 {
        break
    }
    if i % 2 == 0 {
        i = i + 1
        continue
    }
    print(i)
    i = i + 1
}
```

### Arrays

```minilang
# Array declaration and initialization
let numbers: int[] = [1, 2, 3, 4, 5]
let names: string[] = ["Alice", "Bob", "Charlie"]
let flags: boolean[] = [true, false, true]

# Array access
let first: int = numbers[0]
let second: int = numbers[1]

# Array modification
numbers[0] = 10
names[1] = "Robert"

# Array in loops
let i: int = 0
while i < 5 {
    print(numbers[i])
    i = i + 1
}
```

### Structs (Custom Types)

```minilang
# Struct definition
struct Point {
    x: int,
    y: int
}

# Instantiation
let p: Point = Point(10, 20)

# Field access
let xVal: int = p.x

# Field assignment
p.x = 50
p.y = 100
```

### Methods (Type Functions)

```minilang
# Instance methods
struct Counter {
    count: int,

    func getValue() -> int {
        return this.count
    }

    func increment() -> void {
        this.count = this.count + 1
    }
}

let c: Counter = Counter(0)
c.increment()
print(c.getValue())

# Static methods
struct Math {
    static func square(x: int) -> int {
        return x * x
    }
}

let result: int = Math.square(5)
```

### Comments

```minilang
# This is a line comment

# Multi-line comments via multiple # lines
# Line 1
# Line 2
```

### Output

```minilang
# Built-in print function (wraps Logger.log)
print("Hello, World!")
print(42)
print(3.14)
print(true)

# Can also use module directly
Logger.log("Direct module call")
```

### Complete Example

```minilang
# Comprehensive MiniLang demo showing all features

# Variables with all primitive types
let count: int = 0
let pi: double = 3.14159
let greeting: string = "Hello, MiniLang!\n"  # String escape
let active: boolean = true

# Arrays
let fibonacci: int[] = [0, 1, 1, 2, 3, 5, 8, 13]
let scores: double[] = [95.5, 87.3, 92.1]

# Custom struct with fields
struct Point {
    x: double,
    y: double
}

# Struct with instance and static methods
struct Calculator {
    result: double,

    func add(value: double) -> void {
        this.result = this.result + value
    }

    func getResult() -> double {
        return this.result
    }

    static func create() -> Calculator {
        return Calculator(0.0)
    }
}

# Recursive function with type promotion (int -> double)
func factorial(n: int) -> int {
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}

# Function demonstrating operators and control flow
func processArray(arr: int[], threshold: int) -> int {
    let sum: int = 0
    let i: int = 0

    while i < 5 {
        # Comparison and logical operators
        if arr[i] > threshold && arr[i] % 2 == 0 {
            sum = sum + arr[i]
        }
        i = i + 1
    }

    return sum
}

# Main program
func main() -> void {
    # Print with string escape
    print("=== MiniLang Demo ===\n")

    # Arithmetic operations
    let result: int = (5 + 3) * 2 - 1  # Standard C precedence
    print(result)  # 15

    # Array iteration
    print("\nFibonacci sequence:")
    let j: int = 0
    while j < 8 {
        print(fibonacci[j])
        j = j + 1
    }

    # Struct usage
    let p: Point = Point(10.5, 20.3)
    print("\nPoint coordinates:")
    print(p.x)
    print(p.y)

    # Method calls
    let calc: Calculator = Calculator.create()
    calc.add(10.5)
    calc.add(5.3)
    print("\nCalculator result:")
    print(calc.getResult())  # 15.8

    # Recursive function
    print("\nFactorial of 5:")
    print(factorial(5))  # 120

    # Array processing
    let nums: int[] = [10, 15, 20, 25, 30]
    let sum: int = processArray(nums, 12)
    print("\nSum of even numbers > 12:")
    print(sum)  # 50

    # Module function (auto-imported)
    Logger.log("Demo completed!")
}

main()
```

## Why This Approach

### Strengths

1. **Readable** - Minimal syntax noise, clean structure
2. **Educational** - Explicit types teach JCP's type system
3. **Familiar** - Feels like Python/Ruby with type annotations
4. **Complete** - Demonstrates every JCP capability
5. **Practical** - Newline-terminated + braces = best of both worlds

### Trade-offs

1. **Slightly verbose** - `let` keyword and type annotations add characters
2. **Newline parsing** - Grammar must handle multi-line expressions carefully
3. **No type inference** - Always require explicit types (good for learning, more typing)

### Why Not Other Approaches

- **Ultra-minimal (no `let`)** - Ambiguous whether `x: int` is declaration or annotation
- **Keyword-rich (`var`, `function`)** - Too verbose for minimalist philosophy
- **Semicolon-terminated** - Not truly minimal
- **Indentation-based** - Complex to parse, harder for ANTLR beginners

## Key Decisions

### 1. Keyword Choices
- **`let`** - Clearly marks variable declarations vs assignments
- **`func`** - Short but clear, widely recognized
- **`struct`** - Standard term for composite types
- **`static`** - Familiar from Java/C++/C#

### 2. Type Syntax
- **Colon notation** - `x: int` matches TypeScript/Python typing
- **Arrow for returns** - `-> int` is clean and conventional
- **Explicit everywhere** - No type inference to keep parser simple

### 3. Block Delimiters
- **Braces only** - Consistent, clear, no whitespace sensitivity
- **Newline-terminated** - Clean Python feel without semicolon clutter

### 4. Output Strategy
- **`print()` as built-in** - Syntactic sugar for ease of use
- **Wraps `Logger.log()`** - Shows module integration under the hood
- **Both available** - Educational: simple usage + advanced integration

### 5. Comment Style
- **`#` line comments** - Single character, Python-familiar, minimal
- **No block comments** - YAGNI for initial version

### 6. Array Syntax
- **Syntax:** `let arr: int[] = [1, 2, 3]`
- **Access:** `arr[0]`, `arr[i]`
- **Why:** JCP fully supports arrays, demos this capability
- **Literals:** Array literal syntax `[1, 2, 3]` maps to JCP array AST nodes

### 7. String Escapes
- **Standard escapes:** `\n` (newline), `\t` (tab), `\"` (quote), `\\` (backslash)
- **Why:** Familiar to all developers, sufficient for demo programs
- **ANTLR handling:** Use standard string grammar rules

### 8. Operator Precedence
- **Standard C/Java precedence:**
  1. Parentheses `()`
  2. Unary `-`, `!`, `++`, `--`
  3. Multiplicative `*`, `/`, `%`
  4. Additive `+`, `-`
  5. Comparison `<`, `<=`, `>`, `>=`
  6. Equality `==`, `!=`
  7. Logical AND `&&`
  8. Logical OR `||`
- **Why:** Familiar, well-understood, matches most languages

### 9. Module Access
- **Auto-import base module** - `Logger.log()` works directly without import
- **Why:** Simplest for users, reduces boilerplate in examples
- **Implementation:** Pre-populate function registry with base:: functions
- **Future:** Can add explicit `import` syntax if needed for other modules

## Open Questions

### Resolved During Brainstorm
✅ Syntax philosophy → Minimalist (Python/Ruby-like)
✅ Type annotations → Explicit (required)
✅ Function keyword → `func` with arrow syntax
✅ Struct syntax → Braces with commas
✅ Statement terminators → Newline-terminated
✅ Block delimiters → Always use braces
✅ Comments → `#` line comments only
✅ Output → `print()` wraps `Logger.log()`
✅ Array syntax → Yes, include with literal syntax `[1, 2, 3]`
✅ String escapes → Standard escapes `\n`, `\t`, `\"`, `\\`
✅ Operator precedence → Standard C/Java precedence
✅ Module access → Auto-import base module (Logger.log directly)

### Remaining Open Questions
None - all syntax decisions finalized!

## Next Steps

1. **Create ANTLR4 grammar** (`MiniLang.g4`) based on this syntax
2. **Generate lexer/parser** from grammar
3. **Build AST converter** - Parse tree → JCP AST nodes
4. **Write example programs** - Showcase each feature
5. **Integration tests** - Verify both eval and compile modes
6. **Documentation** - README on using MiniLang as template

## Success Criteria

- [ ] Grammar compiles with ANTLR4 without conflicts
- [ ] All JCP AST node types have corresponding syntax
- [ ] Example programs demonstrate every feature
- [ ] Programs run correctly in interpreter mode
- [ ] Programs compile and run correctly via bytecode
- [ ] Debugger integration works (breakpoints, variable inspection)
- [ ] README explains how to customize for other DSLs
- [ ] Tests achieve 80%+ coverage

## Acceptance Criteria (from Issue #29)

- [ ] ANTLR4 grammar file (`.g4`)
- [ ] Lexer and parser generated
- [ ] AST builder: Parse tree → JCP AST nodes
- [ ] Example programs for each feature
- [ ] Integration tests for both eval and compile modes
- [ ] README with usage instructions
- [ ] Documentation on extending/customizing

## References

- **JCP Capabilities Research** - Agent analysis of core module (985 tests, all passing)
- **Type System** - `SystemDataType` enum (INT, DOUBLE, BOOLEAN, STRING, VOID, arrays, structs)
- **Factory Pattern** - Reflection-based: `getName()` → `*Evaluator` / `*Compiler`
- **Test Coverage** - 80%+ instruction and branch coverage enforced

---

**Status:** Ready for planning phase (`/workflows:plan`)
