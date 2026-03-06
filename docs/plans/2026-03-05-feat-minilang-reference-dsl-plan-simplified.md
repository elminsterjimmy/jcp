---
title: feat: Reference DSL Sample for JCP Integration (Simplified)
type: feat
date: 2026-03-05
issue: 29
status: simplified
---

# MiniLang Reference DSL Implementation Plan (Simplified)

## Overview

Implement **MiniLang**, a minimalist reference DSL that demonstrates JCP integration patterns for DSL authors. Focus: clarity over completeness, tests that validate both MiniLang AND JCP core.

**Core Goal**: Show DSL authors the essential pattern:
1. Write ANTLR grammar
2. Convert parse tree to JCP AST
3. Execute in eval and compile modes
4. Test dual-mode parity

**Scope**: Minimal but complete language demonstrating:
- Variables with type annotations (`let x: int = 10`)
- Arithmetic expressions (`x + 5 * 2`)
- Functions (`func add(a: int, b: int) -> int { return a + b }`)
- Control flow (`if`, `while`, `return`, `break`, `continue`)

**Out of Scope** (removed from original plan):
- ❌ Structs with methods (advanced JCP feature, not critical for integration pattern)
- ❌ Arrays (demonstrates same pattern as primitives)
- ❌ Function overloading (advanced feature)
- ❌ Module integration beyond basic usage
- ❌ IDE support
- ❌ Version compatibility strategy
- ❌ Future extensibility planning

---

## Syntax Philosophy

**Minimalist and focused:**
- Explicit types everywhere (`let x: int`, not `let x`)
- Newline-terminated (no semicolons)
- Braces for blocks
- 5 keywords: `let`, `func`, `if`, `while`, `return`

Example program:
```minilang
# Factorial function demonstrating all features
func factorial(n: int) -> int {
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}

let result: int = factorial(5)
print(result)  # 120
```

---

## Implementation Plan

### Phase 1: Grammar + Converter (3 days)

**Day 1: ANTLR Grammar**
- [ ] Create `MiniLang.g4` with rules for:
  - Variables: `let x: int = 10`
  - Functions: `func name(params) -> type { body }`
  - Expressions: arithmetic, literals, function calls
  - Statements: if, while, return, break, continue
- [ ] Configure ANTLR Maven plugin
- [ ] Generate and verify lexer/parser compile
- [ ] Write GrammarTest to verify basic parsing

**Day 2-3: ParseTreeConverter**
- [ ] Create `ParseTreeConverter extends MiniLangBaseVisitor<AstNode>`
- [ ] Implement conversion for statements (inline all logic, no helpers)
- [ ] Implement conversion for expressions
- [ ] Inline type resolution (simple switch, no TypeResolver class)
- [ ] Attach source locations to all nodes
- [ ] Write unit tests for each visitor method

**Deliverables:**
- `MiniLang.g4` (~100 lines)
- `ParseTreeConverter.java` (~250 lines)
- `ParseTreeConverterTest.java` (~150 lines)
- Maven build succeeds

---

### Phase 2: Examples + Integration Tests (2 days)

**Day 4: Example Programs**
- [ ] Write `01-basics.minilang`:
  - Variables, arithmetic, print
  - ~15 lines total
- [ ] Write `02-functions.minilang`:
  - Function declaration, calls, recursion
  - Factorial example
  - ~20 lines total
- [ ] Write `03-control-flow.minilang`:
  - if/else, while loops, break/continue
  - ~25 lines total

**Day 5: Dual-Mode Integration Tests**
- [ ] Create `MiniLangIntegrationTest.java`
- [ ] Parameterized test running each example in BOTH modes
- [ ] Assert identical output between eval and compile
- [ ] Test pattern that validates JCP core dual-mode parity
- [ ] Coverage report: verify 80%+ on MiniLang code

**Test Pattern (Critical for JCP Core Validation):**
```java
@ParameterizedTest
@ValueSource(strings = {"01-basics.minilang", "02-functions.minilang", "03-control-flow.minilang"})
void testDualModeExecution(String exampleFile) throws Exception {
    String source = loadExample(exampleFile);
    Block program = new ParseTreeConverter(exampleFile, source).parse(source);

    // Execute in eval mode
    ByteArrayOutputStream evalOutput = new ByteArrayOutputStream();
    RootEvalContext evalCtx = new RootEvalContext();
    System.setOut(new PrintStream(evalOutput));
    new EvalVisitor(evalCtx).visit(program);

    // Execute in compile mode
    ByteArrayOutputStream compileOutput = new ByteArrayOutputStream();
    JcpCompiler compiler = new JcpCompiler();
    Class<?> clazz = compiler.compileAndLoad(program, "Test_" + exampleFile);
    System.setOut(new PrintStream(compileOutput));
    clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});

    // Assert parity (this validates JCP core behavior!)
    assertEquals(
        evalOutput.toString(),
        compileOutput.toString(),
        "Eval and compile modes must produce identical output"
    );
}
```

**Why this test pattern benefits JCP Core:**
- Validates JCP's dual-mode execution guarantee
- Catches divergence bugs between evaluators and compilers
- Provides regression tests for JCP core changes
- Real-world usage patterns, not synthetic tests

**Deliverables:**
- 3 example `.minilang` files (~60 lines total)
- `MiniLangIntegrationTest.java` (~200 lines)
- All tests passing in both modes

---

### Phase 3: Documentation (1 day)

**Day 6: Single README**
- [ ] Write `README.md` with sections:
  - **What is MiniLang?** (1 paragraph)
  - **Quick Start** (3 commands to run examples)
  - **Syntax Overview** (table with 10 rows showing each feature)
  - **How It Works** (architecture flow: ANTLR → Converter → JCP)
  - **Customizing for Your DSL** (5-step guide)
  - **Testing Your DSL** (explain dual-mode test pattern)
  - **Examples** (links to 3 programs)
- [ ] Add inline comments to grammar explaining key rules
- [ ] Add JavaDoc to ParseTreeConverter public methods
- [ ] Update root README to link to MiniLang module

**Documentation Size:**
- README: ~200 lines (not 500+)
- Grammar comments: inline (not separate file)
- JavaDoc: minimal, intent-revealing

**Deliverables:**
- `module/minilang/README.md`
- Commented grammar
- JavaDoc on key classes

---

## Module Structure (Simplified)

```
module/minilang/
├── pom.xml                                       # Minimal: ANTLR + jcp-core only
├── README.md                                     # Single comprehensive doc
├── src/main/
│   ├── antlr4/com/elminster/minilang/
│   │   └── MiniLang.g4                          # Grammar (~100 lines)
│   ├── java/com/elminster/minilang/
│   │   └── ParseTreeConverter.java             # All logic inline (~250 lines)
│   └── resources/examples/
│       ├── 01-basics.minilang
│       ├── 02-functions.minilang
│       └── 03-control-flow.minilang
└── src/test/java/com/elminster/minilang/
    ├── ParseTreeConverterTest.java              # Unit tests
    └── MiniLangIntegrationTest.java             # Dual-mode tests

Total: ~800 LOC (vs 2500 in original plan)
```

**Key Simplifications:**
- ❌ No TypeResolver.java (inline switch statement)
- ❌ No separate converter classes (one file, focused)
- ❌ No GRAMMAR.md or CUSTOMIZATION.md (merged into README)
- ❌ No runner CLI (tests demonstrate usage)
- ❌ Only 3 examples (not 8)
- ❌ One test file for integration (not 5 feature-specific classes)

---

## Critical Patterns (From Learnings)

### 1. Dual-Registration for Custom Types

**Not in initial scope** (no structs), but document for future:

When adding custom types later, remember:
```java
// Declaration compiler MUST do BOTH:
ctx.addDataType(typeMetadata);        // Compile-time lookups
ctx.addGeneratedClass(name, bytecode); // Runtime loading
```

See: `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`

### 2. Type Resolution Pattern

```java
// In ParseTreeConverter (inline, no helper class):
private DataType resolveType(String typeName) {
    return switch (typeName.toLowerCase()) {
        case "int" -> SystemDataType.INT;
        case "double" -> SystemDataType.DOUBLE;
        case "boolean" -> SystemDataType.BOOLEAN;
        case "string" -> SystemDataType.STRING;
        case "void" -> SystemDataType.VOID;
        default -> SystemDataType.ANY;  // Fallback for unknown types
    };
}
```

No separate TypeResolver class - it's a 10-line method.

### 3. Source Location Attachment

```java
private SourceLocation locationFrom(ParserRuleContext ctx) {
    Token start = ctx.getStart();
    Token stop = ctx.getStop();
    return SourceLocation.span(
        sourceFile,
        start.getLine(),
        start.getCharPositionInLine() + 1,  // ANTLR is 0-based
        stop.getLine(),
        stop.getCharPositionInLine() + stop.getText().length(),
        null  // Source line optional for simplicity
    );
}
```

---

## Testing Strategy (JCP Core Validation Focus)

### Dual-Mode Parity Tests

**Purpose**: Validate JCP core guarantees, not just MiniLang

**What These Tests Validate:**
1. **JCP Core**: Evaluators and compilers produce identical results
2. **JCP Core**: Type system works consistently in both modes
3. **JCP Core**: Context management works correctly
4. **JCP Core**: Factory pattern finds processors correctly
5. **MiniLang**: Grammar and converter work correctly

**Coverage Requirements:**
- MiniLang code: 80%+ (matches JCP core requirement)
- Test BOTH modes for EVERY feature
- No mode-specific behavior allowed

### Test Matrix

| Feature | Test Method | Validates |
|---------|-------------|-----------|
| Variables | `testBasics()` | Variable declaration, assignment, scope |
| Arithmetic | `testBasics()` | Binary operators, precedence, type promotion |
| Functions | `testFunctions()` | Function declaration, calls, parameters, returns, recursion |
| Control Flow | `testControlFlow()` | if/else, while, break, continue |
| All Combined | `testIntegration()` | Parameterized test running all examples |

---

## Acceptance Criteria

### Functional Requirements

**Grammar:**
- [ ] ANTLR grammar compiles without conflicts
- [ ] Parses all 3 example programs successfully
- [ ] Reports syntax errors with line/column numbers

**Converter:**
- [ ] Converts all parse tree nodes to correct JCP AST nodes
- [ ] Attaches source locations to every node
- [ ] Type resolution works for all system types

**Execution:**
- [ ] All examples run in eval mode
- [ ] All examples compile and run in compile mode
- [ ] Outputs are identical between modes

**Testing:**
- [ ] Parameterized integration test validates dual-mode parity
- [ ] Coverage ≥80% on MiniLang code
- [ ] All tests pass: `mvn verify -pl module/minilang`

### Documentation Requirements

- [ ] README explains purpose in 1 paragraph
- [ ] README shows how to run examples in 3 commands
- [ ] README explains dual-mode test pattern
- [ ] Grammar has inline comments for key rules
- [ ] Examples are self-documenting with comments

### Quality Gates

**Phase 1 Complete:**
- [ ] Grammar compiles
- [ ] Converter unit tests pass
- [ ] Maven build succeeds

**Phase 2 Complete:**
- [ ] All examples run in both modes
- [ ] Integration tests pass
- [ ] Coverage ≥80%

**Phase 3 Complete:**
- [ ] README complete
- [ ] Code review approved
- [ ] Ready to merge

---

## Risk Analysis (Simplified)

### Technical Risks

**Risk: ANTLR Grammar Conflicts**
- Likelihood: Low
- Impact: Medium (1 day delay)
- Mitigation: Start simple, test incrementally

**Risk: Eval/Compile Mode Divergence**
- Likelihood: Medium
- Impact: High (breaks core assumption)
- Mitigation: Dual-mode test from day 1, test EVERY feature

**Risk: Coverage < 80%**
- Likelihood: Low
- Impact: Medium (blocks merge)
- Mitigation: Run coverage after each phase

---

## Timeline Summary

**Total: 6 days (not 20)**

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Grammar + Converter | 3 days | Working AST conversion |
| Examples + Tests | 2 days | 3 examples, integration tests passing |
| Documentation | 1 day | README complete |

**What was cut from original 20-day plan:**
- 14 days of over-engineering
- Structs with methods
- Arrays
- Function overloading
- 5 unnecessary example programs
- 3 extra test classes
- 2 extra documentation files
- TypeResolver abstraction
- CLI runner with mode selection
- Risk matrices
- Future planning sections
- IDE support planning

---

## Success Metrics

### Primary: Does it teach DSL integration?

**Yes if:**
- DSL author can read README in 10 minutes
- DSL author can understand ParseTreeConverter in 30 minutes
- DSL author can adapt grammar for their syntax in 1 hour
- DSL author can copy dual-mode test pattern immediately

### Secondary: Does it validate JCP core?

**Yes if:**
- Dual-mode tests catch divergence bugs
- Examples exercise all major JCP features
- Tests run as part of JCP core test suite
- Coverage contributes to JCP's 80% requirement

---

## Implementation Notes

### Code Style

**KISS Principles Applied:**
- One converter class, not three
- Inline type resolution, no helper
- Simple switch statements, no polymorphism
- Direct conversions, no intermediate representations
- Clear variable names, minimal comments

**From GUIDELINES.md:**
- Processor classes 15-50 lines (converter methods will follow this)
- Extract base if >100 lines (won't happen with simplified scope)
- Test both eval and compile (enforced in integration tests)

### ANTLR Patterns

**Key Patterns from Research:**
- Use visitor pattern (returns values, unlike listeners)
- Keywords MUST come before ID rule in lexer
- Left-recursive rules for operator precedence
- Label alternatives for specific visitor methods (`#VarDeclStmt`)
- visitLabel() before visitLineNumber() for ASM

**Grammar Organization:**
```antlr
grammar MiniLang;

// High-level structure
program : statement* EOF ;

// Statements (order: simple to complex)
statement
    : letStatement      # LetStmt
    | ifStatement       # IfStmt
    | whileStatement    # WhileStmt
    | returnStatement   # ReturnStmt
    | functionDecl      # FuncDecl
    | expressionStmt    # ExprStmt
    ;

// Expressions (precedence via nesting)
expression
    : expression '(' argumentList? ')'      # FunctionCall
    | expression ('*' | '/' | '%') expression  # MultDiv
    | expression ('+' | '-') expression        # AddSub
    | expression ('<' | '>' | '<=') expression # Comparison
    | literal                                  # LiteralExpr
    | ID                                       # Identifier
    | '(' expression ')'                       # Parens
    ;
```

---

## Maven POM Configuration

```xml
<dependencies>
    <dependency>
        <groupId>com.elminster</groupId>
        <artifactId>jcp-core</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.antlr</groupId>
        <artifactId>antlr4-runtime</artifactId>
        <version>4.13.1</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.antlr</groupId>
            <artifactId>antlr4-maven-plugin</artifactId>
            <version>4.13.1</version>
            <configuration>
                <visitor>true</visitor>
                <listener>false</listener>
            </configuration>
            <executions>
                <execution>
                    <goals><goal>antlr4</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## References

### Internal Documentation
- `GUIDELINES.md` - KISS and SOLID principles
- `README.md` - JCP architecture overview
- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md` - Dual registration
- `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md` - Type resolution
- `docs/brainstorms/2026-03-05-minilang-syntax-design-brainstorm.md` - Original syntax design

### Research Documents Generated
- `docs/research/2026-03-05-antlr4-framework-documentation.md` - ANTLR patterns
- `docs/plans/2026-03-05-minilang-plan-enhancements.md` - Deep research findings

### Review Feedback Applied
- DHH review: Simplified from 20 days to 6 days
- Kieran review: No God Objects, focused responsibilities
- Simplicity review: Removed 40% LOC, eliminated YAGNI features

---

## What Was Removed (And Why)

### Features Cut
- **Structs with methods** → Too complex for initial reference, demonstrates advanced pattern not needed for integration
- **Arrays** → Same pattern as primitives, doesn't add educational value
- **Function overloading** → Advanced feature, YAGNI for reference implementation
- **Module integration beyond print()** → Out of scope, JCP already has module examples

### Documentation Cut
- **GRAMMAR.md** → Grammar documents itself with inline comments
- **CUSTOMIZATION.md** → Merged into README as "How to Customize" section
- **CHANGELOG.md** → Reference code doesn't need versioning
- **Architecture diagrams** → README text flow is sufficient for ~600 LOC

### Code Cut
- **TypeResolver.java** → 10-line switch statement, inline it
- **Separate converter classes** → One focused converter is clearer
- **CLI runner with mode selection** → Tests are the real usage
- **Custom error listener** → ANTLR defaults are fine
- **5 feature-specific test classes** → One parameterized test class

### Planning Cut
- **6 phases → 3 phases** → Artificial separation, work flows naturally
- **Risk matrices** → Over-planning for 6-day project
- **Resource requirements** → One person, one branch, done when done
- **Future extensibility planning** → YAGNI, add when needed
- **IDE support planning** → Not in scope
- **Version compatibility** → Lives in repo, no releases

---

## Next Steps After Plan Approval

1. Create feature branch: `feat/29-minilang-reference-dsl`
2. Phase 1: Days 1-3 (Grammar + Converter)
3. Phase 2: Days 4-5 (Examples + Tests)
4. Phase 3: Day 6 (Documentation)
5. Code review with focus on clarity
6. Merge to master
7. Update main README to feature MiniLang

**Total calendar time: 6 business days (1.2 weeks)**

---

**Plan Status**: Simplified from original 20-day comprehensive plan based on unanimous reviewer feedback. Focused on core value: teaching DSL integration patterns with tests that also validate JCP core.