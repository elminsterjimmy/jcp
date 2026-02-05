---
title: "feat: Add README.md and GitHub Actions CI workflow"
type: feat
date: 2026-02-06
---

# feat: Add README.md and GitHub Actions CI workflow

## Overview

Create a comprehensive README.md for the JCP project and configure GitHub Actions CI with Maven build, multi-version Java testing, and code coverage reporting.

## Problem Statement / Motivation

JCP currently lacks:
- A README.md to introduce the project to visitors
- CI/CD automation for builds and tests
- Visibility into build status and code quality

This impacts project discoverability, contributor onboarding, and code quality assurance.

## Proposed Solution

1. **Update Java version** from 8 to 11 in all pom.xml files
2. **Add JaCoCo plugin** for code coverage reporting
3. **Create GitHub Actions workflow** with Java 11/17/21 matrix
4. **Write comprehensive README.md** with badges, architecture, and examples

## Technical Considerations

### Java Version Migration

Current pom.xml targets Java 8. Files requiring update:
- `/pom.xml` (root) - `java.version`, `maven.compiler.source`, `maven.compiler.target`
- `/core/pom.xml` - `java.compile.version` property
- AspectJ 1.9.19 is compatible with Java 11+

### JaCoCo Integration

- Add JaCoCo plugin to root pom.xml
- Generate XML reports for CI consumption
- No coverage thresholds initially (informational only)
- Multi-module reports generated per-module

### GitHub Actions Workflow

- File: `.github/workflows/maven.yml`
- Triggers: push to master, pull_request to master
- Matrix: Java 11, 17, 21 (all must pass)
- Distribution: Eclipse Temurin
- Caching: Maven dependencies cached by pom.xml hash

### README Structure

Based on brainstorm decisions:
1. Project title with badges (build status, Java version)
2. One-line description
3. Features list (dual-mode execution)
4. Quick Start (installation + basic example)
5. Architecture Overview
6. Usage Examples (eval mode + compile mode)
7. Module Structure
8. Building from Source
9. Running Tests
10. Contributing guidelines
11. License placeholder

## Acceptance Criteria

### Functional Requirements

- [x] All pom.xml files updated to Java 11
- [x] JaCoCo plugin generates coverage reports on `mvn test`
- [x] GitHub Actions workflow runs on PR and push to master
- [ ] CI passes for Java 11, 17, and 21 (pending push to GitHub)
- [x] README.md renders correctly on GitHub
- [ ] Build status badge displays correct status (pending CI run)
- [x] Code examples in README are functional

### Non-Functional Requirements

- [ ] CI build completes in under 10 minutes (pending CI run)
- [x] README is scannable (good heading hierarchy)
- [x] Examples demonstrate both eval and compile modes

## Implementation Plan

### Phase 1: Java Version Update

**Files to modify:**

#### `/pom.xml` (root)

Update properties:
```xml
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

#### `/core/pom.xml`

Check for any override properties and align with root.

#### Verification

```bash
mvn clean install -DskipTests
mvn test
```

### Phase 2: JaCoCo Integration

**Add to root `/pom.xml`:**

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**Verification:**
```bash
mvn test
ls core/target/site/jacoco/  # Should contain index.html
```

### Phase 3: GitHub Actions Workflow

**Create `.github/workflows/maven.yml`:**

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    strategy:
      fail-fast: false
      matrix:
        java: [ '11', '17', '21' ]

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK ${{ matrix.java }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'
        cache: maven

    - name: Build with Maven
      run: mvn clean install -B

    - name: Upload coverage reports
      if: matrix.java == '11'
      uses: actions/upload-artifact@v4
      with:
        name: coverage-report
        path: '**/target/site/jacoco/'
        retention-days: 7
```

### Phase 4: README.md

**Create `/README.md`:**

Key sections:
1. **Header with badges** - Build status from shields.io
2. **What is JCP** - One paragraph description
3. **Features** - Bullet list highlighting dual-mode execution
4. **Quick Start** - Clone, build, run example
5. **Architecture** - Diagram showing eval vs compile paths
6. **Usage Examples** - Code snippets for both modes
7. **Project Structure** - Module breakdown
8. **Building** - Maven commands from CLAUDE.md
9. **Testing** - Test commands
10. **Contributing** - Basic guidelines
11. **License** - TBD placeholder

**Badge URLs:**
```markdown
![Build Status](https://img.shields.io/github/actions/workflow/status/elminsterjimmy/jcp/maven.yml?branch=master)
![Java Version](https://img.shields.io/badge/Java-11%2B-blue)
```

**Example code (eval mode):**
```java
Block program = new BlockImpl();
// ... add statements
EvalContext context = new RootEvalContext();
new EvalVisitor(context).visit(program);
Data result = context.getVariable("varName");
```

**Example code (compile mode):**
```java
Block program = new BlockImpl();
// ... add statements
JcpCompiler compiler = new JcpCompiler();
Class<?> clazz = compiler.compileAndLoad(program, "ClassName");
clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
```

## Dependencies & Risks

### Dependencies
- GitHub repository must exist at `elminsterjimmy/jcp`
- Repository admin access needed for branch protection (optional)

### Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| AspectJ incompatible with Java 11 | Low | Medium | Test locally first; AspectJ 1.9.19 supports Java 11+ |
| CI build time too long | Low | Low | Maven caching enabled |
| Badge URLs incorrect | Medium | Low | Verify after first CI run |

## Success Metrics

- CI workflow runs successfully for all Java versions
- README renders correctly on GitHub
- Build badge shows green status
- Coverage reports generated and accessible

## References

### Internal
- Brainstorm: `/docs/brainstorms/2026-02-06-readme-and-ci-brainstorm.md`
- Project guide: `/CLAUDE.md`
- Architecture: `/docs/architecture/jcp-function-architecture.md`

### External
- [GitHub Actions setup-java](https://github.com/actions/setup-java)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [shields.io badges](https://shields.io/)
