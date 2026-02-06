---
title: Implementation Guidelines (KISS & SOLID)
type: docs
date: 2026-02-06
deepened: 2026-02-06
---

# docs: Add GUIDELINES.md for KISS and SOLID Principles

## Enhancement Summary

**Deepened on:** 2026-02-06
**Research agents used:** pattern-recognition-specialist, architecture-strategist, code-simplicity-reviewer, best-practices-researcher, learnings-researcher

### Key Improvements
1. Added compiler-specific architectural patterns (Visitor, Context Management, Factory+Visitor hybrid)
2. Incorporated documented learnings as concrete MUST rules (dual registration, type resolution)
3. Simplified structure from 5 SOLID sections to focused "Common Patterns" approach
4. Added anti-patterns section based on codebase analysis (God Context, Mutable AST, incomplete TODOs)
5. Optimized format for AI parseability (tables, file path anchors, good/bad contrasts)

### New Considerations Discovered
- Visitor pattern is architecturally central but was missing from original plan
- CompileContext at 476 lines is borderline - document as "watch" threshold
- 5 incomplete TODO comments found in codebase - document as anti-pattern
- Type resolution needs consistent 3-step lookup pattern across all code paths

---

## Overview

Create a `GUIDELINES.md` file that codifies KISS and SOLID principles for the JCP project, optimized for consumption by both human contributors and AI coding assistants. The guidelines will use a principle-centric structure with MUST/SHOULD keywords and concrete examples from the existing codebase.

## Problem Statement / Motivation

The JCP codebase demonstrates excellent architectural patterns (reflection-based factories, dual-mode execution, focused interfaces) but these patterns are not formally documented. This creates challenges for:

1. **New contributors** - Must reverse-engineer patterns from code
2. **AI assistants** - No parseable rules to follow when generating code
3. **Code reviews** - No objective criteria for pattern compliance
4. **Refactoring** - No documented targets for improvement

### Research Insights

**Best Practices from Open Source:**
- CONTRIBUTING.md files in VS Code, Kubernetes, Go, and Rust are typically 200-800 words
- Main guidelines should be **comprehensive yet scannable** (1,500-2,000 words)
- Use progressive disclosure: main file is index, detailed docs linked externally
- RFC 2119 keywords (MUST/SHOULD/MAY) are well-understood by both humans and AI

**AI Parseability:**
- Use structured headings (H2/H3 hierarchy)
- Include file path anchors for all code examples
- Show good/bad contrasts to reduce hallucinations
- Put quick reference tables at TOP (not bottom)
- Keep under 500 lines for context window fit

---

## Proposed Solution

Create `GUIDELINES.md` with:

1. **Principle-centric structure** - KISS first, then each SOLID letter (S, O, L, I, D)
2. **RFC-style keywords** - MUST, SHOULD, MUST NOT for clear enforcement
3. **Concrete examples** - Code snippets from existing JCP classes
4. **AI discoverability** - Reference from CLAUDE.md

### Research Insights

**Simplicity Feedback:**
- Consider reducing 5 separate SOLID sections to focused "Common Patterns"
- Generic SOLID advice won't address actual complexity sources (ASM, context management)
- Target 150-250 lines for main content, not 500
- Make entire document "quick reference" style

**Recommended Approach:**
- Keep RFC keywords (AI models understand these well)
- Lead with Quick Reference table at TOP
- Focus on JCP-specific patterns over generic SOLID theory
- Include anti-patterns section with concrete codebase examples

---

## Technical Approach

### File Structure (Enhanced)

```
GUIDELINES.md (new, ~250 lines)
├── Quick Reference Table (TOP - for fast lookup)
├── How to Read This Document (keyword definitions)
├── KISS Principles
│   ├── MUST: Keep implementations minimal (15-50 lines)
│   ├── MUST NOT: Create interfaces for single implementations
│   ├── MUST NOT: Optimize prematurely
│   ├── SHOULD: Prefer explicit over clever
│   └── Examples with file paths
├── Single Responsibility (S)
│   ├── MUST: One class = one reason to change
│   ├── MUST: Parallel evaluator/compiler structure
│   └── Examples: PlusEvaluator vs PlusCompiler
├── Open/Closed (O)
│   ├── MUST: Extend via new classes
│   ├── MUST: Follow naming conventions for auto-discovery
│   └── Example: Factory reflection pattern
├── Liskov Substitution (L)
│   ├── MUST: Subtypes must be substitutable
│   ├── SHOULD: Use @Override annotation
│   └── Examples: Data<T>, SystemDataType hierarchy
├── Interface Segregation (I)
│   ├── MUST: Keep interfaces focused (single capability)
│   ├── MUST NOT: Fat interfaces
│   └── Examples: Node, Evaluable, Compilable
├── Dependency Inversion (D)
│   ├── SHOULD: Depend on abstractions
│   ├── SHOULD: Accept context via parameters
│   └── Examples: EvalContext, CompileContext
├── Compiler-Specific Patterns (NEW)
│   ├── MUST: Mirror eval/compile structure
│   ├── MUST: Dual registration for custom types
│   ├── MUST: Follow 3-step type resolution
│   └── Context management patterns
├── Anti-Patterns to Avoid (NEW)
│   ├── God Context objects (>500 lines)
│   ├── Mutable AST nodes
│   ├── Incomplete TODO comments
│   ├── Deep context chains (>3 levels)
│   └── Silent factory failures
└── References
```

### Research Insights

**Missing Patterns to Add (from Architecture Review):**

1. **Visitor Pattern** - Central to both modes but not documented
   - `AstVisitor.java` interface
   - `EvalVisitor.java` and `CompileVisitor.java` implementations
   - MUST: Visitors delegate to factories (no instanceof checks)
   - MUST NOT: Modify AST during visitor traversal

2. **Context Management Pattern** - Critical for correctness
   - Parent-child context chaining for nested scopes
   - Variable lookup: current → parent chain (lexical scoping)
   - Function lookup: always root context (global namespace)
   - MUST: Create child contexts for blocks/functions
   - MUST NOT: Share contexts across compilation units

3. **Factory + Visitor Hybrid** - Unique to JCP
   - Visitor provides traversal (external iteration)
   - Factory provides processing (reflection-based dispatch)
   - Enables Open/Closed: add classes, not modify visitor

**Additional MUST Rules (from Documented Learnings):**

| Source | Rule | Type |
|--------|------|------|
| struct-type-not-registered | Dual registration: `addDataType()` AND `addGeneratedClass()` | MUST |
| struct-type-resolution | Type resolution: system → custom → ANY (3 steps) | MUST |
| struct-type-resolution | Type resolution methods need `CompileContext` parameter | MUST |
| noclassdeffounderror | All BytecodeGenerator methods must set `rootContext` field | MUST |
| noclassdeffounderror | Test all compilation entry points (compile vs compileWithReturn) | SHOULD |

---

### Phase 1: Create GUIDELINES.md

**Deliverables:**
- `GUIDELINES.md` in project root (~250 lines)
- Quick reference table at TOP
- Introduction with RFC keyword definitions
- KISS section with rules and examples
- All five SOLID sections with rules and examples
- Compiler-specific patterns section (NEW)
- Anti-patterns section (NEW)

**Key Content:**

#### Quick Reference Table (NEW - at TOP)

```markdown
## Quick Reference: Adding a New Language Feature

| Step | Action | File Pattern |
|------|--------|-------------|
| 1 | Define AST node | `ast/[category]/[Feature]Impl.java` |
| 2 | Implement `getName()` | Return `"FEATURE"` (uppercase) |
| 3 | Create evaluator | `eval/[category]/[Feature]Evaluator.java` |
| 4 | Create compiler | `compile/[category]/[Feature]Compiler.java` |
| 5 | Write tests | Both eval AND compile modes |

**No factory registration needed** - reflection handles discovery.
```

#### Introduction Section

```markdown
## How to Read This Document

- **MUST** / **MUST NOT** - Hard rules. Violations are errors.
- **SHOULD** / **SHOULD NOT** - Guidelines. Exceptions require justification.
- **MAY** - Optional. Use judgment.

This document is optimized for:
- Human contributors learning JCP patterns
- AI coding assistants generating compliant code
- Code reviewers evaluating submissions
```

#### KISS Section Examples

```markdown
## KISS Principles

### MUST: Keep implementations minimal

Concrete processor classes SHOULD be 15-50 lines. If a class exceeds 100 lines,
consider extracting a base class.

**Good Example - PlusCompiler.java (21 lines):**

**File:** `core/src/main/java/com/elminster/jcp/compile/operator/arithmetic/PlusCompiler.java`

```java
public class PlusCompiler extends ArithmeticCompiler {
    public PlusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DADD : Opcodes.IADD);
    }
}
```

### MUST NOT: Create interfaces for single implementations

**Bad:**
```java
interface PlusOperation { int add(int a, int b); }
class PlusOperationImpl implements PlusOperation { ... }
```

**Good:**
```java
class PlusCompiler extends ArithmeticCompiler { ... }
```

Only create an interface when you have 2+ implementations.

### MUST NOT: Optimize prematurely

Prove performance need with benchmarks before adding complexity.
```

#### Compiler-Specific Patterns (NEW)

```markdown
## Compiler-Specific Patterns

### MUST: Dual Registration for Custom Types

When implementing declaration compilers for custom types (structs, enums):

```java
// BOTH registrations are required
ctx.addDataType(typeMetadata);        // For compile-time type checking
ctx.addGeneratedClass(name, bytecode); // For runtime class loading
```

**Why:** Registering only bytecode causes "Unknown type" errors downstream.

**Source:** `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`

### MUST: Follow 3-Step Type Resolution

When resolving type names during compilation:

1. Check system types first (fast path for primitives)
2. Check custom types from context: `ctx.getDataType(typeName)`
3. Fall back to `SystemDataType.ANY` only if neither match

**Why:** Skipping step 2 breaks struct field access and method calls.

**Source:** `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`

### MUST: Maintain Eval/Compile Mode Parity

When implementing compile mode for an eval feature:

1. Check what state changes the evaluator makes to `EvalContext`
2. Ensure compiler makes equivalent changes to `CompileContext`
3. Test BOTH modes in integration tests
```

#### Anti-Patterns Section (NEW)

```markdown
## Anti-Patterns to Avoid

### ❌ God Context Objects

**Warning signs:**
- Context class > 500 lines
- Context handles > 5 distinct concerns

**Current state:** `CompileContext` is 476 lines - watch this boundary.

### ❌ Mutable AST Nodes

**Bad:** `FunctionDeclarationImpl` has mutable fields
**Good:** `IntLiteral.of()` - functional interface, no mutation
**Risk:** Concurrent evaluation can cause race conditions

### ❌ Incomplete TODO Comments

Found in codebase:
- `FieldAssignmentCompiler.java:63` - "TODO: Add compile-time type checking"
- `AbstractModuleFunction.java:28` - "TODO" (no description)

**MUST:** Use format `TODO(owner, priority): action - rationale`

### ❌ Deep Context Chains

**SHOULD:** Limit parent context chain to 3 levels
**Risk:** O(n) lookup time for deeply nested scopes
**Mitigation:** Cache frequently accessed symbols at root

### ❌ Silent Factory Failures

Current behavior: Factory throws RuntimeException with node name (GOOD).
**SHOULD:** Add factory test verifying all AST nodes have processors.
```

### Phase 2: Update CLAUDE.md

**Deliverables:**
- Add reference to GUIDELINES.md in CLAUDE.md

**Change:**

```markdown
## Implementation Guidelines

See [GUIDELINES.md](GUIDELINES.md) for KISS and SOLID principles with concrete
code examples. AI assistants MUST follow these guidelines when generating code.
```

---

## Acceptance Criteria

### Functional Requirements

- [x] `GUIDELINES.md` exists at project root
- [x] Quick reference table at TOP of document
- [x] Contains KISS section with MUST/SHOULD rules
- [x] Contains all 5 SOLID principle sections
- [x] Contains Compiler-Specific Patterns section (NEW)
- [x] Contains Anti-Patterns section (NEW)
- [x] Each section has concrete code examples from JCP codebase
- [x] File paths reference actual existing files

### Non-Functional Requirements

- [x] Document is parseable by AI assistants (clear keywords, structure)
- [x] Code examples compile (if extracted to files)
- [x] File is 200-300 lines (not 500 - per simplicity review)
- [x] Uses GitHub-flavored markdown
- [x] Good/bad contrasts included for key rules

### Quality Gates

- [x] CLAUDE.md references GUIDELINES.md
- [x] All code examples have file path citations
- [x] No broken internal links
- [x] Documented learnings integrated as MUST rules

### Research Insights

**Additional Quality Checks:**
- [x] Visitor pattern documented (central but was missing)
- [x] Context management patterns documented
- [x] All 5 TODO anti-patterns captured
- [x] Type resolution 3-step pattern included
- [x] Dual registration pattern included

---

## Success Metrics

1. **Contributor onboarding** - New contributors can find naming conventions quickly
2. **AI compliance** - Claude/Copilot generates code following patterns
3. **Code review efficiency** - Reviewers can cite specific guideline sections

### Research Insights

**Validation Tests (after creating GUIDELINES.md):**
Ask Claude these questions to verify parseability:
1. "What are the MUST rules for adding a new language feature?"
2. "Show me a KISS-compliant compiler implementation."
3. "What's the type resolution order for custom types?"
4. "Why do declaration compilers need dual registration?"

If Claude answers correctly from GUIDELINES.md alone, the structure works.

---

## Dependencies & Prerequisites

- None - documentation only

---

## Risk Analysis & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Guidelines become stale | Medium | Medium | Review quarterly; update with major features |
| Over-prescription | Low | Medium | Focus on existing patterns, not theoretical ideals |
| AI misinterpretation | Low | Low | Use RFC keywords consistently; test with Claude |
| Visitor pattern misuse | HIGH | HIGH | Explicit guidance + code review checklist |
| Context mismanagement | HIGH | HIGH | Document parent-child discipline, provide examples |
| Factory convention violations | Medium | Medium | Document naming prominently, consider build-time validation |

### Research Insights

**New Risks Identified:**

1. **Visitor pattern misuse** - Contributors may add instanceof checks instead of using factories
   - Mitigation: Add explicit "MUST delegate to factories, MUST NOT use instanceof"

2. **Context mismanagement** - Incorrect scope handling causes variable leakage or incorrect JVM indices
   - Mitigation: Document parent-child discipline with concrete examples

3. **Factory convention violations** - Inconsistent naming breaks auto-discovery
   - Mitigation: Document naming prominently, consider adding build-time validation test

---

## References & Research

### Internal References

- Factory pattern: `core/src/main/java/com/elminster/jcp/eval/factory/AstEvaluatorFactory.java`
- KISS example: `core/src/main/java/com/elminster/jcp/compile/operator/arithmetic/PlusCompiler.java`
- SRP example: `core/src/main/java/com/elminster/jcp/eval/operator/arithmetic/PlusEvaluator.java`
- ISP example: `core/src/main/java/com/elminster/jcp/ast/Node.java`
- DIP example: `core/src/main/java/com/elminster/jcp/eval/context/EvalContext.java`
- Visitor interface: `core/src/main/java/com/elminster/jcp/ast/vistor/AstVisitor.java`
- EvalVisitor: `core/src/main/java/com/elminster/jcp/eval/EvalVisitor.java`
- CompileVisitor: `core/src/main/java/com/elminster/jcp/compile/CompileVisitor.java`

### Brainstorm Document

- `docs/brainstorms/2026-02-06-implementation-guidelines-brainstorm.md`

### Documented Learnings (Integrated as MUST Rules)

- Dual registration pattern: `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`
- Type resolution pattern: `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`
- Context consistency: `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md`
- Methods-as-functions KISS: `docs/architecture/jcp-function-architecture.md`

### External References

- RFC 2119 - Key words for Requirement Levels (IETF)
- Refactoring Guru - SOLID principles with code examples
- Claude Code Documentation - CLAUDE.md file structure best practices
