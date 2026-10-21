[← Back to README](../README.md)

# Contributing

## Getting Started

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

## Adding New Language Features

JCP uses a dual-mode architecture — every language feature needs both an interpreter evaluator and a bytecode compiler. The factory pattern maps AST node names to processor classes automatically.

### Steps

1. **Define AST Node** in `ast/` — implement `getName()` for factory lookup
2. **Create Evaluator** in `eval/[category]/[Feature]Evaluator.java` — extends `AbstractAstEvaluator`
3. **Create Compiler** in `compile/[category]/[Feature]Compiler.java` — extends `AbstractAstCompiler`
4. **Write tests** for both modes in `core/src/test/java/`
5. **Update TypeMapper** if adding a new type (descriptors, load/store/return opcodes)

### Naming Conventions

- Evaluators: `[Feature]Evaluator.java` (e.g., `PlusEvaluator`)
- Compilers: `[Feature]Compiler.java` (e.g., `PlusCompiler`)
- AST node `getName()` returns uppercase (e.g., `"PLUS"`)
- The factory maps `getName()` → `PlusEvaluator` / `PlusCompiler` automatically

## Test Requirements

- **Both modes**: Every feature must have tests for eval mode and compile mode
- **Coverage**: JaCoCo enforces minimum **80% instruction and 80% branch coverage** on the core module — the build fails if coverage drops below these thresholds

```bash
# Run tests with coverage report
mvn test -pl core

# View coverage report
open core/target/site/jacoco/index.html

# Verify coverage thresholds pass
mvn verify -pl core
```

## See Also

- [Integration Guide](integration-guide.md) — How JCP's eval and compile modes work
- [Architecture](architecture/jcp-function-architecture.md) — Function system internals
- [CLAUDE.md](../CLAUDE.md) — Full developer guide and project conventions
