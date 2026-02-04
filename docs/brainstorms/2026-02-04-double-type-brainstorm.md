# Brainstorm: Double Number Type

**Date:** 2026-02-04
**Status:** Ready for planning
**Feature:** 1 of 3 (independent)

## What We're Building

Add `double` (64-bit floating point) type to JCP, supporting both interpret and compile modes.

## Scope

- Add `DOUBLE` to `SystemDataType` enum
- Add `DoubleData extends AnyData<Double>` for eval
- Add `DoubleLiteral` AST node (or extend existing Literal)
- Auto-promotion: `int + double → double`
- All arithmetic ops: `+`, `-`, `*`, `/`, `%`
- All comparison ops: `<`, `<=`, `>`, `>=`, `==`, `!=`

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Precision | 64-bit double only | Standard choice, simpler type system |
| Promotion | Auto int→double | User-friendly, matches Java behavior |
| Literal syntax | Assumed: `3.14`, `1.0` | AST already parsed externally |

## Implementation Notes

### Eval Mode

1. `DoubleData` - wrapper for `Double`, similar to `IntegerData`
2. Update arithmetic evaluators to detect double operands
3. Promotion logic: if either operand is double, promote int to double
4. Return `DoubleData` for promoted operations

### Compile Mode

JVM bytecode specifics:
- Double uses **2 local variable slots** (vs 1 for int)
- Load/Store: `DLOAD`, `DSTORE`, `DCONST_0`, `DCONST_1`
- Arithmetic: `DADD`, `DSUB`, `DMUL`, `DDIV`, `DREM`
- Promotion: `I2D` (int to double)
- Comparison: `DCMPG` or `DCMPL` followed by branch instruction
- Return: `DRETURN`

### Type Hierarchy

```
NUMERIC (parent)
├── INT
└── DOUBLE
```

Both INT and DOUBLE should have NUMERIC as parent for `isCastableTo()` compatibility.

## Files to Create/Modify

**New files:**
- `eval/data/DoubleData.java`
- `ast/expression/literal/DoubleLiteral.java` (if needed)
- `compile/base/DoubleLiteralCompiler.java` (if separate from LiteralCompiler)

**Modified files:**
- `eval/data/DataType.java` - add DOUBLE to enum
- `eval/data/DataFactory.java` - handle DOUBLE creation
- `compile/base/LiteralCompiler.java` - handle double literals
- `compile/util/TypeMapper.java` - map DOUBLE to JVM type
- All arithmetic compilers - add promotion logic
- All comparison compilers - add double comparison logic

## Open Questions

1. Should `NUMERIC` be a real type or just a category for type checking?
2. Division behavior: `5 / 2` → `2` (int) or `2.5` (double)?

## Next Steps

Run `/workflows:plan docs/brainstorms/2026-02-04-double-type-brainstorm.md`
