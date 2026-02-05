# README and GitHub CI Brainstorm

**Date:** 2026-02-06
**Status:** Ready for implementation

## What We're Building

Two deliverables for the JCP project:

1. **Comprehensive README.md** - Full project documentation with badges, features, architecture overview, installation, usage examples, and contributing guidelines

2. **GitHub Actions Maven Workflow** - CI pipeline for automated builds and testing

## Why This Approach

### README Style: Comprehensive

- JCP is a complex dual-mode language implementation (interpreter + bytecode compiler)
- Comprehensive documentation helps new contributors understand the architecture
- Badges provide quick visibility into build status and project health
- Usage examples demonstrate both eval and compile modes

### CI Configuration

**Triggers:** Pull requests and pushes to master branch
- Prevents broken code from merging
- Doesn't waste CI resources on feature branch pushes

**Java Versions:** 11, 17, 21 (matrix build)
- Java 11 as the primary/default LTS version
- Java 17 and 21 for forward compatibility testing
- Note: Current pom.xml targets Java 8 - may need updating

**Features:**
- Build and test (mvn clean install)
- Code coverage with JaCoCo

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| README style | Comprehensive | Complex project needs thorough docs |
| CI triggers | PR + push to master | Balance coverage vs resource use |
| Java primary | 11 | LTS with good ecosystem support |
| Java matrix | 11, 17, 21 | All current LTS versions |
| Coverage tool | JaCoCo | Standard Maven integration |

## Open Questions

1. **Java version in pom.xml:** Currently targets Java 8. Should this be updated to Java 11 as part of this work?
2. **Coverage thresholds:** Should the CI fail if coverage drops below a certain percentage?
3. **Badge service:** Use shields.io for badges? GitHub's native badges?

## Implementation Notes

### README Structure
```
- Project logo/title with badges
- One-line description
- Features list
- Quick Start (installation + basic example)
- Architecture Overview (dual-mode execution)
- Usage Examples (eval mode + compile mode)
- Module Structure
- Building from Source
- Running Tests
- Contributing
- License
```

### GitHub Workflow Structure
```yaml
.github/workflows/maven.yml
- Trigger: push to master, pull_request to master
- Matrix: Java 11, 17, 21
- Steps: checkout, setup-java, mvn install, coverage report
```

## Next Steps

Run `/workflows:plan` to create implementation plan.
