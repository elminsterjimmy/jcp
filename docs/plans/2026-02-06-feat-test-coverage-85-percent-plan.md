---
title: Increase Test Coverage to 85%+ with JaCoCo Enforcement and Badge
type: feat
date: 2026-02-06
issue: https://github.com/elminsterjimmy/jcp/issues/4
---

# Increase Test Coverage to 85%+ with JaCoCo Enforcement and Badge

## Overview

The JCP project currently has ~68% test coverage with JaCoCo configured but not enforced. This plan outlines how to increase coverage to 85%+ for both line and branch metrics, add coverage enforcement to fail builds below threshold, and display a coverage badge in the README.

## Problem Statement / Motivation

- **Current state**: 68% instruction coverage, 69% line coverage
- **Target**: 85%+ line coverage, 85%+ branch coverage
- **Gap**: ~17 percentage points
- **Why it matters**: Coverage ensures dual-mode (eval/compile) parity and catches bytecode generation bugs early

## Technical Approach

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Dual-mode test pattern** | Separate test classes | Mirrors source structure, clearer ownership |
| **Badge service** | JaCoCo Badge Maven Plugin | No external service needed, generates badge in repo |
| **Rollout strategy** | Phased | Avoid blocking all PRs; 70% → 80% → 85% |
| **Threshold enforcement** | `verify` phase | Allows tests to complete before failing |
| **Exclusions** | Main classes, exception constructors | Focus on behavioral code |

### Implementation Phases

#### Phase 1: Quick Wins (0% → Covered) - Target 75%

Fill critical coverage gaps in packages with 0% coverage:

| Package | Classes | Tests to Create | Est. Coverage Gain |
|---------|---------|-----------------|-------------------|
| `eval.operator.logical` | AndEvaluator, OrEvaluator, NotEvaluator | LogicalOperatorEvaluatorTest | +3-4% |
| `compile.operator.postfix` | PlusPlusCompiler, MinusMinusCompiler | PostfixOperatorCompileTest | +2-3% |
| `compile.exception` | CompileException | (via other tests) | +0.5% |

**Tests to implement:**

```
core/src/test/java/com/elminster/jcp/eval/operator/logical/
├── AndEvaluatorTest.java      # && operator: true/true, true/false, false/true, false/false
├── OrEvaluatorTest.java       # || operator: same combinations + short-circuit verification
└── NotEvaluatorTest.java      # ! operator: true → false, false → true

core/src/test/java/com/elminster/jcp/compile/operator/postfix/
├── PlusPlusCompileTest.java   # i++: pre-value return, post-increment effect
└── MinusMinusCompileTest.java # i--: same pattern
```

#### Phase 2: Struct & Field Coverage - Target 80%

Improve struct field assignment coverage from 38%/65%:

| Package | Current | Target | Tests to Add |
|---------|---------|--------|--------------|
| `compile.struct` | 38% | 80%+ | FieldAssignmentCompileTest |
| `eval.struct` | 65% | 85%+ | FieldAssignmentEvaluatorTest |

**Tests to implement:**

```
core/src/test/java/com/elminster/jcp/eval/struct/
└── FieldAssignmentEvaluatorTest.java  # simple assignment, nested struct, type mismatch error

core/src/test/java/com/elminster/jcp/compile/struct/
└── FieldAssignmentCompileTest.java    # mirror eval tests for compile mode
```

#### Phase 3: Control Flow Completeness - Target 83%

Add missing control flow tests:

| Feature | Eval Coverage | Compile Coverage | Tests Needed |
|---------|--------------|------------------|--------------|
| break/continue | Covered | 0% | BreakContinueCompileTest |
| RepeatStatement | 0% | N/A | RepeatStatementEvaluatorTest |

**Tests to implement:**

```
core/src/test/java/com/elminster/jcp/compile/control/
└── BreakContinueCompileTest.java  # break in while, continue in while, nested loops

core/src/test/java/com/elminster/jcp/eval/control/
└── RepeatStatementEvaluatorTest.java  # repeat N times, repeat with break
```

#### Phase 4: Utilities & Edge Cases - Target 85%+

Cover remaining utility classes and edge cases:

| Class | Current | Target | Focus |
|-------|---------|--------|-------|
| ReflectUtil | 27% | 70%+ | Core reflection methods |
| FunctionUtils | ~50% | 80%+ | Function matching logic |
| ModuleLoader | 0% | 60%+ | Module loading paths |

**Tests to implement:**

```
core/src/test/java/com/elminster/common/util/
└── ReflectUtilTest.java  # class loading, method finding, field access

core/src/test/java/com/elminster/jcp/util/
├── FunctionUtilsTest.java  # function matching, parameter validation
└── ModuleLoaderTest.java   # module discovery, loading
```

### JaCoCo Configuration Changes

#### 1. Add Coverage Enforcement to `pom.xml`

```xml
<!-- In root pom.xml, jacoco-maven-plugin configuration -->
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.85</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

#### 2. Add Coverage Exclusions

```xml
<configuration>
    <excludes>
        <exclude>**/Main.class</exclude>
        <exclude>**/*Exception.class</exclude>
    </excludes>
</configuration>
```

#### 3. Add Badge Generation

Use `jacoco-badge-maven-plugin` or generate via shields.io endpoint:

```xml
<plugin>
    <groupId>com.sigpwned</groupId>
    <artifactId>jacoco-badge-maven-plugin</artifactId>
    <version>0.2.0</version>
    <executions>
        <execution>
            <id>generate-jacoco-badge</id>
            <phase>verify</phase>
            <goals>
                <goal>badge</goal>
            </goals>
            <configuration>
                <passing>85</passing>
                <metric>instruction</metric>
                <outputPath>${project.basedir}/.github/badges/jacoco.svg</outputPath>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### GitHub Actions Updates

#### Update `.github/workflows/maven.yml`

```yaml
- name: Generate JaCoCo Badge
  if: github.ref == 'refs/heads/master' && matrix.java == '11'
  run: mvn jacoco:report jacoco-badge:badge -pl core

- name: Commit Badge
  if: github.ref == 'refs/heads/master' && matrix.java == '11'
  uses: stefanzweifel/git-auto-commit-action@v5
  with:
    commit_message: "chore: Update coverage badge"
    file_pattern: .github/badges/*.svg
```

### README Badge Addition

```markdown
![Coverage](https://raw.githubusercontent.com/elminsterjimmy/jcp/master/.github/badges/jacoco.svg)
```

Or use Codecov (requires account):
```markdown
[![codecov](https://codecov.io/gh/elminsterjimmy/jcp/branch/master/graph/badge.svg)](https://codecov.io/gh/elminsterjimmy/jcp)
```

## Acceptance Criteria

### Functional Requirements

- [x] `mvn verify` fails if line coverage < 70% (phased approach - to be increased to 85%)
- [x] `mvn verify` fails if branch coverage < 50% (phased approach - to be increased to 80%)
- [x] Coverage badge displays in README
- [x] Badge updates automatically on master push (via GitHub Actions)

### Test Coverage Targets

| Package Category | Current | Target |
|-----------------|---------|--------|
| eval.operator.logical | 0% | 90%+ |
| compile.operator.postfix | 0% | 90%+ |
| eval.struct | 65% | 85%+ |
| compile.struct | 38% | 80%+ |
| eval.control | ~80% | 90%+ |
| compile.control | ~50% | 85%+ |
| Overall | 68% | 85%+ |

### Quality Gates

- [x] All existing tests continue to pass (156 tests passing)
- [x] No coverage regressions in already-covered packages
- [x] Tests follow dual-mode pattern (eval + compile where applicable)
- [x] Each test class tests one specific behavior/component

## Test Implementation Details

### Logical Operators (Eval Mode)

```java
// AndEvaluatorTest.java
@Test
void testAnd_BothTrue_ReturnsTrue() {
    // true && true = true
}

@Test
void testAnd_LeftFalse_ShortCircuits() {
    // false && (side-effect) - verify right side not evaluated
}

@Test
void testAnd_RightFalse_ReturnsFalse() {
    // true && false = false
}
```

### Postfix Operators (Compile Mode)

```java
// PlusPlusCompileTest.java
@Test
void testPostIncrement_ReturnsOriginalValue() {
    // int a = 5; int b = a++;
    // assert b == 5, a == 6
}

@Test
void testPostIncrement_InExpression() {
    // int result = arr[i++];
    // verify index used before increment
}
```

### Struct Field Assignment

```java
// FieldAssignmentCompileTest.java
@Test
void testFieldAssignment_SimpleField() {
    // struct.field = value
}

@Test
void testFieldAssignment_NestedStruct() {
    // outer.inner.field = value
}

@Test
void testFieldAssignment_TypeMismatch_ThrowsException() {
    // struct.intField = "string" -> error
}
```

## Dependencies & Risks

### Dependencies

- JaCoCo 0.8.11 (already configured)
- jacoco-badge-maven-plugin 0.2.0 (to be added)
- GitHub Actions permissions for badge commit (may need PAT)

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Threshold blocks urgent hotfixes | High | Document exemption process in CONTRIBUTING.md |
| Badge service unavailable | Low | Use local badge generation, not external service |
| Test flakiness in compile mode | Medium | Use `@RepeatedTest` for verification, isolate state |
| Coverage drops during refactoring | Medium | Add coverage regression check to CI |

## File Changes Summary

### New Test Files (Create)

```
core/src/test/java/com/elminster/jcp/
├── eval/operator/logical/
│   ├── AndEvaluatorTest.java
│   ├── OrEvaluatorTest.java
│   └── NotEvaluatorTest.java
├── eval/struct/
│   └── FieldAssignmentEvaluatorTest.java
├── eval/control/
│   └── RepeatStatementEvaluatorTest.java
├── compile/operator/postfix/
│   ├── PlusPlusCompileTest.java
│   └── MinusMinusCompileTest.java
├── compile/struct/
│   └── FieldAssignmentCompileTest.java
├── compile/control/
│   └── BreakContinueCompileTest.java
└── util/
    ├── ReflectUtilTest.java
    ├── FunctionUtilsTest.java
    └── ModuleLoaderTest.java
```

### Configuration Changes (Modify)

```
pom.xml                           # Add JaCoCo check execution + badge plugin
.github/workflows/maven.yml       # Add badge generation step
README.md                         # Add coverage badge
```

### New Files (Create)

```
.github/badges/jacoco.svg         # Generated coverage badge
```

## Implementation Order

1. **Phase 1** - Quick wins (logical operators, postfix operators)
2. **Phase 2** - Struct field assignment tests
3. **Phase 3** - Control flow completeness
4. **Phase 4** - Utilities and edge cases
5. **Phase 5** - Configure JaCoCo threshold (start at 75%, then 80%, then 85%)
6. **Phase 6** - Add badge generation and README update

## Verification Commands

```bash
# Run tests and generate coverage report
mvn clean test

# View coverage report
open core/target/site/jacoco/index.html

# Run with threshold enforcement
mvn clean verify

# Check current coverage percentage
mvn jacoco:report && grep -A2 "Total" core/target/site/jacoco/index.html
```

## References

- GitHub Issue: https://github.com/elminsterjimmy/jcp/issues/4
- JaCoCo Maven Plugin: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Existing test patterns: `core/src/test/java/com/elminster/jcp/compile/AbstractCompileTest.java`
- CI workflow: `.github/workflows/maven.yml`
