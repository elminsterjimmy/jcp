---
title: "feat: Add Double (64-bit Floating Point) Type"
type: feat
date: 2026-02-04
status: ready
---

# feat: Add Double (64-bit Floating Point) Type

## Overview

Add `double` (64-bit IEEE 754 floating point) type to JCP, supporting both interpreter (eval) and bytecode compiler modes. Users don't explicitly declare types - the system **infers INT or DOUBLE from literal syntax** (Python-style).

## Problem Statement / Motivation

JCP currently only supports `INT` for numeric operations. Real-world applications require floating-point arithmetic for:
- Mathematical calculations (percentages, averages, ratios)
- Scientific computations
- Financial calculations (though precision limitations apply)

## Proposed Solution

Implement `DOUBLE` type following the existing `INT` implementation pattern:
1. Add `DOUBLE` to type system with `NUMERIC` parent
2. Fix `INT` parent from `ANY` to `NUMERIC`
3. Create `DoubleData` wrapper for eval mode
4. Create `DoubleLiteral` AST node (single class, lambda pattern)
5. Update all arithmetic evaluators/compilers for double support
6. Update all comparison evaluators/compilers for double support
7. Handle automatic int→double promotion

**Key design:** Users never write `int` or `double` - type is **inferred from literal**:
- `a = 1` → INT (no decimal point)
- `a = 1.5` → DOUBLE (has decimal point)

## Technical Approach

### Type Hierarchy

Current (line 14 in DataType.java):
```
INT("Integer", ANY)  ← INT directly under ANY
```

New:
```
ANY (root)
└── NUMERIC
    ├── INT      ← Change parent from ANY to NUMERIC
    └── DOUBLE   ← New type
```

**Why this works for `fun(any)`:**
- `NUMERIC("Numeric", ANY)` - NUMERIC is child of ANY ✓
- `INT("Integer", NUMERIC)` - INT is child of NUMERIC
- `isCastableTo()` walks up: INT → NUMERIC → ANY ✓

### Type Inference

| Literal | Internal Type | Rationale |
|---------|---------------|-----------|
| `1` | INT | No decimal point |
| `1.5` | DOUBLE | Has decimal point |
| `1.0` | DOUBLE | Has decimal point |

**Variable Type Mutability:** Variables are dynamically typed. `a = 5` followed by `a = 5.5` is allowed - `a` changes from INT to DOUBLE. This matches Python semantics.

### Type Promotion Rules

When binary operations mix INT and DOUBLE:
1. If either operand is DOUBLE, promote INT operand to DOUBLE
2. Result type is DOUBLE
3. Promotion happens at operation time

```
a = 5      // INT
b = 2.5    // DOUBLE
c = a + b  // a promoted via I2D, result is DOUBLE
```

### JVM Bytecode Details

**Critical: Doubles use 2 local variable slots**

| Operation | INT Opcode | DOUBLE Opcode |
|-----------|------------|---------------|
| Load | ILOAD | DLOAD |
| Store | ISTORE | DSTORE |
| Const 0 | ICONST_0 | DCONST_0 |
| Const 1 | ICONST_1 | DCONST_1 |
| Other const | LDC | LDC (auto-handles double) |
| Add | IADD | DADD |
| Subtract | ISUB | DSUB |
| Multiply | IMUL | DMUL |
| Divide | IDIV | DDIV |
| Modulo | IREM | DREM |
| Return | IRETURN | DRETURN |
| Int→Double | - | I2D |

**Stack Depth:** Auto-computed by ASM's `COMPUTE_MAXS` flag (already enabled in `BytecodeGenerator.java:29`). No manual tracking needed.

**Comparison strategy:**
- Use `DCMPL` for `<`, `<=`, `==`, `!=` (NaN → -1)
- Use `DCMPG` for `>`, `>=` (NaN → +1)
- This ensures NaN comparisons return false (IEEE 754 compliant)

### Division Behavior

Follow IEEE 754 (matches JVM behavior):
- `5.0 / 0.0` → `Infinity`
- `-5.0 / 0.0` → `-Infinity`
- `0.0 / 0.0` → `NaN`

**Note:** Integer division `5 / 0` still throws `ArithmeticException`. This inconsistency matches Java behavior.

### String Concatenation

For `"value: " + 3.14`:
- If left operand is STRING, convert right operand to string via `String.valueOf()`
- Works for both INT and DOUBLE right operands
- Already handled by existing PlusEvaluator string logic

## Implementation Phases

### Phase 1: Type System & Eval Mode

**Goal:** Add DOUBLE to type system and get eval mode arithmetic working

**Files to create:**

#### DoubleData.java
`core/src/main/java/com/elminster/jcp/eval/data/DoubleData.java`

```java
public class DoubleData extends AnyData<Double> {
    public DoubleData(Double data) {
        super(data);
    }

    public DoubleData(Identifier identifier, Double data) {
        super(identifier, data);
    }

    @Override
    public DataType getDataType() {
        return DataType.SystemDataType.DOUBLE;
    }
}
```

#### DoubleLiteral.java
`core/src/main/java/com/elminster/jcp/ast/expression/literal/DoubleLiteral.java`

Match the IntLiteral lambda pattern:
```java
public interface DoubleLiteral extends Literal<Double> {
    static DoubleLiteral of(Double value) {
        return () -> value;
    }
}
```

No `DoubleLiteralImpl` needed - the lambda is sufficient.

**Files to modify:**

#### DataType.java
`core/src/main/java/com/elminster/jcp/eval/data/DataType.java`

```java
// Change INT parent from ANY to NUMERIC (line 14)
INT("Integer", NUMERIC),  // Was: INT("Integer", ANY)

// Add DOUBLE after INT
DOUBLE("Double", NUMERIC),
```

#### DataFactory.java
`core/src/main/java/com/elminster/jcp/eval/data/DataFactory.java`

Add DOUBLE case to switch:
```java
case DOUBLE:
    data = new DoubleData(id, (Double) value);
    break;
```

#### DataTypeUtils.java
`core/src/main/java/com/elminster/jcp/util/DataTypeUtils.java`

Add DoubleLiteral check AND shared conversion utility:
```java
// In getDataTypeAndCreateOnMissing():
if (literalExpression instanceof DoubleLiteral) {
    return DataType.SystemDataType.DOUBLE;
}

// NEW: Shared utility method for type conversion
public static double toDoubleValue(Data operand) {
    DataType type = operand.getDataType();
    if (type == SystemDataType.DOUBLE) {
        return (Double) operand.get();
    }
    if (type == SystemDataType.INT) {
        return ((Integer) operand.get()).doubleValue();
    }
    throw new IllegalArgumentException("Cannot convert to double: " + type);
}
```

#### Arithmetic Evaluators (5 files)
`core/src/main/java/com/elminster/jcp/eval/operator/arithmetic/`

Update `PlusEvaluator`, `MinusEvaluator`, `MultiplyEvaluator`, `DivideEvaluator`, `ModEvaluator`:

```java
@Override
protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    DataType leftType = leftOperand.getDataType();
    DataType rightType = rightOperand.getDataType();

    // Handle DOUBLE operations (including promotion)
    if (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE) {
        double left = DataTypeUtils.toDoubleValue(leftOperand);
        double right = DataTypeUtils.toDoubleValue(rightOperand);
        return new DoubleData(left + right);  // Change operator per evaluator
    }

    // Existing INT logic unchanged...
    if (leftType == SystemDataType.INT) {
        // ... existing code
    }

    // String concatenation (PlusEvaluator only)
    if (leftType == SystemDataType.STRING) {
        String leftValue = (String) leftOperand.get();
        String rightValue = String.valueOf(rightOperand.get());  // Works for DOUBLE
        return new StringData(leftValue + rightValue);
    }

    throw new UnsupportedOperationException(...);
}
```

**Tests:** Add to existing arithmetic tests or create `DoubleEvalTest.java`:
- Basic double arithmetic: `3.14 + 2.0`
- Mixed int/double: `5 + 2.5`
- String concatenation: `"x: " + 3.14`
- Division by zero: `5.0 / 0.0` → Infinity

---

### Phase 2: Compile Mode

**Goal:** Generate correct bytecode for double operations

**Files to modify:**

#### TypeMapper.java
`core/src/main/java/com/elminster/jcp/compile/util/TypeMapper.java`

Add DOUBLE to all switch statements:
```java
public static String toDescriptor(DataType type) {
    // Add case:
    case DOUBLE:
        return "D";
}

public static int getLoadOpcode(DataType type) {
    // Add case:
    case DOUBLE:
        return Opcodes.DLOAD;
}

public static int getStoreOpcode(DataType type) {
    // Add case:
    case DOUBLE:
        return Opcodes.DSTORE;
}

public static int getReturnOpcode(DataType type) {
    // Add case:
    case DOUBLE:
        return Opcodes.DRETURN;
}

public static boolean isPrimitive(DataType type) {
    // Add DOUBLE:
    return type == SystemDataType.INT || type == SystemDataType.BOOLEAN
           || type == SystemDataType.DOUBLE;
}

// NEW METHOD - Critical for slot allocation
public static int getSlotSize(DataType type) {
    return (type == SystemDataType.DOUBLE) ? 2 : 1;
}
```

#### CompileContext.java
`core/src/main/java/com/elminster/jcp/compile/context/CompileContext.java`

Fix `allocateLocal()` to handle double's 2-slot requirement:
```java
public int allocateLocal(String name, DataType type) {
    int index = nextLocalIndex;
    int slotSize = TypeMapper.getSlotSize(type);
    nextLocalIndex += slotSize;  // 2 for double, 1 for others
    locals.put(name, new LocalVariable(index, type, name));
    return index;
}
```

#### LiteralCompiler.java
`core/src/main/java/com/elminster/jcp/compile/base/LiteralCompiler.java`

Add double literal handling:
```java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    // Add check for DoubleLiteral
    if (astNode instanceof DoubleLiteral) {
        double value = ((DoubleLiteral) astNode).getValue();
        if (value == 0.0) {
            mv.visitInsn(Opcodes.DCONST_0);
        } else if (value == 1.0) {
            mv.visitInsn(Opcodes.DCONST_1);
        } else {
            mv.visitLdcInsn(value);  // ASM handles LDC2_W automatically
        }
        return;
    }
    // existing code...
}
```

#### Expression Type Resolution (NEW UTILITY)

Add to `TypeMapper.java` or create new utility:
```java
/**
 * Determine the result type of an expression.
 * Used by compilers to decide which opcodes to emit.
 */
public static DataType getExpressionType(Expression expr, CompileContext ctx) {
    if (expr instanceof LiteralExpression) {
        LiteralExpression lit = (LiteralExpression) expr;
        if (lit.getLiteral() instanceof IntLiteral) return SystemDataType.INT;
        if (lit.getLiteral() instanceof DoubleLiteral) return SystemDataType.DOUBLE;
        if (lit.getLiteral() instanceof BooleanLiteral) return SystemDataType.BOOLEAN;
        if (lit.getLiteral() instanceof StringLiteral) return SystemDataType.STRING;
    }
    if (expr instanceof Identifier) {
        CompileContext.LocalVariable local = ctx.getLocal(((Identifier) expr).getName());
        return local != null ? local.getType() : null;
    }
    // For binary expressions, determine result type based on operands
    if (expr instanceof BinaryExpression) {
        BinaryExpression bin = (BinaryExpression) expr;
        DataType leftType = getExpressionType(bin.getLeft(), ctx);
        DataType rightType = getExpressionType(bin.getRight(), ctx);
        // Numeric promotion: if either is DOUBLE, result is DOUBLE
        if (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE) {
            return SystemDataType.DOUBLE;
        }
        // Both INT → result is INT
        return SystemDataType.INT;
    }
    return null;  // Unknown
}
```

#### Arithmetic Compilers (5 files)
`core/src/main/java/com/elminster/jcp/compile/operator/arithmetic/`

Update each to handle double:

```java
// PlusCompiler.java
@Override
protected void emitOperation(MethodVisitor mv) {
    // Get types from the base class (set during compile())
    if (useDouble) {
        mv.visitInsn(Opcodes.DADD);
    } else {
        mv.visitInsn(Opcodes.IADD);
    }
}
```

Update `ArithmeticCompiler` base class:
```java
protected boolean useDouble;  // Set by compile()

@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    BinaryExpression binaryExpr = (BinaryExpression) astNode;
    Expression left = binaryExpr.getLeft();
    Expression right = binaryExpr.getRight();

    DataType leftType = TypeMapper.getExpressionType(left, ctx);
    DataType rightType = TypeMapper.getExpressionType(right, ctx);
    useDouble = (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE);

    // Compile left operand
    AstCompilerFactory.getCompiler(left).compile(mv, ctx);
    if (useDouble && leftType == SystemDataType.INT) {
        mv.visitInsn(Opcodes.I2D);  // Promote int to double
    }

    // Compile right operand
    AstCompilerFactory.getCompiler(right).compile(mv, ctx);
    if (useDouble && rightType == SystemDataType.INT) {
        mv.visitInsn(Opcodes.I2D);  // Promote int to double
    }

    emitOperation(mv);
}
```

#### Comparison Compilers (6 files)
`core/src/main/java/com/elminster/jcp/compile/operator/relational/`

Inline the double handling (no new abstract methods):

```java
// LessThanCompiler.java
@Override
public void compile(MethodVisitor mv, CompileContext ctx) {
    // ... compile operands with promotion ...

    Label trueLabel = new Label();
    Label endLabel = new Label();

    if (useDouble) {
        mv.visitInsn(Opcodes.DCMPL);  // NaN → -1 → not less than
        mv.visitJumpInsn(Opcodes.IFLT, trueLabel);
    } else {
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
    }

    // False path
    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitJumpInsn(Opcodes.GOTO, endLabel);

    // True path
    mv.visitLabel(trueLabel);
    mv.visitInsn(Opcodes.ICONST_1);

    mv.visitLabel(endLabel);
}
```

**Comparison opcode summary:**
| Operator | Double | Condition | Rationale |
|----------|--------|-----------|-----------|
| `<` | DCMPL | IFLT | NaN → -1 → false |
| `<=` | DCMPL | IFLE | NaN → -1 → false |
| `>` | DCMPG | IFGT | NaN → +1 → false |
| `>=` | DCMPG | IFGE | NaN → +1 → false |
| `==` | DCMPL | IFEQ | NaN → -1 → not zero → false |
| `!=` | DCMPL | IFNE | NaN → -1 → not zero → true |

**Tests:** Add to `BytecodeGeneratorTest.java`:
- Double variable declaration: `double x = 3.14`
- Double arithmetic: `double y = x + 2.0`
- Mixed operations: `int a = 5; double b = a + 2.5;`
- Slot sequence test (critical): `int a; double b; int c;` → verify slots 0, 1-2, 3
- Comparisons: `3.14 > 2.0`, `1.5 < 2.5`

---

### Phase 3: Integration & Edge Cases

**Goal:** Verify everything works together, test edge cases

**Tasks:**
- [ ] Test complex expressions: `a + b * c - d / e` with mixed types
- [ ] Test control flow: `if (temp > 100.0) { ... }`
- [ ] Test while loops: `while (x < 10.5) { ... }`
- [ ] Verify eval and compile modes produce identical results

**Edge Case Tests:**
- [ ] NaN comparisons: `Double.NaN > 0` → false
- [ ] Infinity: `1.0 / 0.0` → Infinity
- [ ] Negative zero: `-0.0 == 0.0` → true
- [ ] Large numbers: `1.7976931348623157E308`
- [ ] Precision limits: `0.1 + 0.2 != 0.3` (expected IEEE 754 behavior)

**Slot Allocation Stress Test (Critical):**
```java
// Test: int a; double b; int c; double d; int e;
// Expected slots: a=0, b=1-2, c=3, d=4-5, e=6
@Test
void testMixedTypeSlotAllocation() {
    Block program = new BlockImpl();
    program.addStatement(varDecl("a", INT, literal(1)));
    program.addStatement(varDecl("b", DOUBLE, doubleLiteral(2.5)));
    program.addStatement(varDecl("c", INT, literal(3)));
    program.addStatement(varDecl("d", DOUBLE, doubleLiteral(4.5)));
    program.addStatement(varDecl("e", INT, literal(5)));

    byte[] bytecode = compiler.compileToBytes(program, "SlotTest");
    // Print bytecode to verify slots
    compiler.printBytecode(bytecode);
    // Load and execute to verify JVM accepts it
    Class<?> clazz = loadClass("SlotTest", bytecode);
    assertNotNull(clazz);
}
```

## Acceptance Criteria

### Functional Requirements

- [ ] `DOUBLE` type added to `SystemDataType` with `NUMERIC` parent
- [ ] `INT` parent changed from `ANY` to `NUMERIC`
- [ ] `INT.isCastableTo(ANY)` still returns true (via NUMERIC)
- [ ] `DoubleData` wrapper stores and returns double values correctly
- [ ] `DoubleLiteral` AST node using lambda pattern (matches IntLiteral)
- [ ] All 5 arithmetic operations work with doubles (eval mode)
- [ ] All 5 arithmetic operations work with doubles (compile mode)
- [ ] All 6 comparison operations work with doubles (both modes)
- [ ] Int-to-double promotion works automatically in mixed expressions
- [ ] String concatenation works: `"x: " + 3.14`
- [ ] Division by zero produces Infinity/NaN (not exception)
- [ ] Local variable slots correctly allocate 2 slots for doubles
- [ ] Mixed int/double variable sequences compile correctly

### Non-Functional Requirements

- [ ] Both eval and compile modes produce identical results
- [ ] No regression in existing INT tests
- [ ] Bytecode passes JVM verification

### Quality Gates

- [ ] All existing tests pass
- [ ] New tests cover arithmetic, comparisons, and edge cases
- [ ] Code follows existing patterns (DoubleLiteral matches IntLiteral)
- [ ] Shared `toDoubleValue()` utility - no duplication across evaluators

## Files Summary

### New Files (2)
- `core/src/main/java/com/elminster/jcp/eval/data/DoubleData.java`
- `core/src/main/java/com/elminster/jcp/ast/expression/literal/DoubleLiteral.java`

### Modified Files (15+)
- `core/src/main/java/com/elminster/jcp/eval/data/DataType.java`
- `core/src/main/java/com/elminster/jcp/eval/data/DataFactory.java`
- `core/src/main/java/com/elminster/jcp/util/DataTypeUtils.java` (+ toDoubleValue utility)
- `core/src/main/java/com/elminster/jcp/compile/util/TypeMapper.java` (+ getSlotSize, getExpressionType)
- `core/src/main/java/com/elminster/jcp/compile/context/CompileContext.java`
- `core/src/main/java/com/elminster/jcp/compile/base/LiteralCompiler.java`
- 5 arithmetic evaluators in `eval/operator/arithmetic/`
- 5 arithmetic compilers in `compile/operator/arithmetic/`
- 6 comparison compilers in `compile/operator/relational/`

### Test Additions
- Add double test cases to existing `BytecodeGeneratorTest.java`
- Add double test cases to existing eval tests
- Add slot allocation stress test

## Design Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| User-facing type names | None (inferred) | Python-style: `a = 1` → INT, `a = 1.5` → DOUBLE |
| INT parent type | NUMERIC | Enables proper type hierarchy, keeps `isCastableTo(ANY)` working |
| Variable mutability | Allowed | `a = 5` then `a = 5.5` is valid (Python semantics) |
| DoubleLiteral pattern | Lambda (no Impl class) | Matches IntLiteral, simpler |
| Stack depth tracking | Auto (COMPUTE_MAXS) | Already enabled in BytecodeGenerator |
| Comparison abstraction | Inline in each compiler | Simpler than 3 abstract methods |
| Division by zero | IEEE 754 | Matches JVM behavior |
| String + double | String.valueOf() | Extends existing string concat logic |

## References

### Internal References
- Brainstorm: `docs/brainstorms/2026-02-04-double-type-brainstorm.md`
- INT implementation: `core/src/main/java/com/elminster/jcp/eval/data/IntegerData.java`
- IntLiteral pattern: `core/src/main/java/com/elminster/jcp/ast/expression/literal/IntLiteral.java`
- Type system: `core/src/main/java/com/elminster/jcp/eval/data/DataType.java`
- TypeMapper: `core/src/main/java/com/elminster/jcp/compile/util/TypeMapper.java`
- COMPUTE_MAXS usage: `core/src/main/java/com/elminster/jcp/compile/BytecodeGenerator.java:29`

### External References
- JVM Specification: Double type uses 2 slots
- ASM documentation: COMPUTE_MAXS handles stack depth
- IEEE 754 floating-point standard
