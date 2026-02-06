# Implementation Guidelines Brainstorm

**Date:** 2026-02-06
**Status:** Ready for implementation

## What We're Building

A `GUIDELINES.md` file that codifies KISS and SOLID principles for the JCP project, optimized for consumption by both human contributors and AI coding assistants.

### Goals

1. **Onboard new contributors** - Help newcomers understand patterns quickly
2. **Enforce code quality** - Create checkable rules for consistency
3. **Document existing patterns** - Formalize what's already working well
4. **Guide refactoring efforts** - Establish targets for improvement
5. **Enable agentic engineering** - Guidelines AI assistants can parse and follow

## Why This Approach

### Principle-Centric Structure

Organize by principle (KISS first, then each SOLID letter) because:

- Clear mental model that maps to industry-standard terminology
- Easy to reference specific principles when reviewing code
- AI agents can locate relevant rules by searching principle names
- Matches how developers think about code quality

### Format Decisions

- **Bullet points with keywords** (MUST/SHOULD/MUST NOT) for easy AI parsing
- **Concrete examples from JCP** to demonstrate patterns in context
- **Tiered strictness**: Hard rules (MUST) and soft guidelines (SHOULD)
- **Implementation-focused** - excludes testing guidelines (separate concern)

## Key Decisions

1. **Location**: Separate `GUIDELINES.md` file in project root
2. **Discovery**: CLAUDE.md will reference GUIDELINES.md for AI discoverability
3. **Structure**: Principle-centric (KISS, S, O, L, I, D)
4. **Format**: Bullet points with MUST/SHOULD keywords
5. **Examples**: Include good examples from existing codebase
6. **Scope**: Implementation only (no testing guidelines)

## Proposed Structure

```
GUIDELINES.md
├── Introduction
│   ├── Purpose and audience
│   └── How to read (MUST vs SHOULD)
├── KISS Principles
│   ├── MUST: No unnecessary abstraction
│   ├── MUST: No premature optimization
│   ├── SHOULD: Prefer explicit over clever
│   └── Examples from JCP
├── Single Responsibility Principle (S)
│   ├── MUST: One class = one reason to change
│   ├── MUST: Factory pattern for processors
│   └── Examples: Evaluator/Compiler separation
├── Open/Closed Principle (O)
│   ├── MUST: Extend via new classes
│   ├── MUST: Follow naming conventions for auto-discovery
│   └── Examples: Reflection-based factory pattern
├── Liskov Substitution Principle (L)
│   ├── MUST: Subtypes must be substitutable
│   ├── SHOULD: Use @Override annotation
│   └── Examples: Data<T> implementations
├── Interface Segregation Principle (I)
│   ├── MUST: Keep interfaces focused
│   ├── MUST NOT: Fat interfaces
│   └── Examples: Node, Evaluable, Compilable
├── Dependency Inversion Principle (D)
│   ├── SHOULD: Depend on abstractions
│   ├── SHOULD: Inject dependencies
│   └── Examples: Context interfaces
└── Quick Reference
    └── Cheat sheet for common scenarios
```

## KISS Focus Areas

Based on discussion, the guidelines will proactively address:

1. **Over-abstraction** - Don't create interfaces for single implementations
2. **Premature optimization** - Prove need before optimizing
3. **Feature creep** - Implement only what's required
4. **Over-complexity** - Simplest solution that works

## Open Questions

1. Should we include a "Code Smells" section listing anti-patterns?
2. How detailed should the examples be (snippet vs full file reference)?
3. Should we version the guidelines (v1.0) for future evolution?

## Next Steps

1. Run `/workflows:plan` to create implementation plan
2. Draft GUIDELINES.md content
3. Add reference to CLAUDE.md
4. Review with team/stakeholders
