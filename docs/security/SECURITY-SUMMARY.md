# MiniLang Security Audit - Executive Summary

**Date:** 2026-03-05
**Project:** MiniLang Reference DSL (Issue #29)
**Overall Risk:** MEDIUM
**Status:** 7 vulnerabilities identified, 0 remediated

---

## Critical Findings (Must Fix Before Merge)

### 1. Path Traversal Vulnerability (HIGH)
**File:** `MiniLangRunner.java`

The runner accepts arbitrary file paths without validation, allowing attackers to read any file on the system:

```bash
# Attack: Read /etc/passwd
mvn exec:java -Dexec.args="eval ../../../../etc/passwd"
```

**Fix:** Implement base directory restriction, symlink checks, and file extension validation.

**Impact:** Arbitrary file read, information disclosure, potential privilege escalation.

### 2. Resource Exhaustion (HIGH)
**File:** `AstBuilder.java`

No limits on parse tree depth, array size, or compilation time. Allows denial-of-service attacks:

```minilang
# DoS attack: 10,000 nested parentheses
let x: int = (((((((((...))))))))))))
```

**Fix:** Add depth limits (100), array size limits (10,000), AST node limits (100,000), and compilation timeout (30s).

**Impact:** Denial of service, CI/CD pipeline failures, development machine crashes.

---

## High Priority Findings (Fix Before Release)

### 3. String Escape Processing Bug (MEDIUM)
**File:** `AstBuilder.parseStringLiteral()`

Incorrect escape sequence order causes wrong output:

```java
// WRONG ORDER - processes \\\\ before \\n
return content
    .replace("\\n", "\n")
    .replace("\\t", "\t")
    .replace("\\\"", "\"")
    .replace("\\\\", "\\");  // Should be FIRST!
```

**Fix:** Use state machine approach with single-pass processing.

**Impact:** String injection, data corruption, security issues if strings used in commands.

### 4. Unsafe ClassLoader (CRITICAL severity, LOW likelihood)
**File:** `MultiClassLoader.java`

Loads arbitrary bytecode without verification, enabling arbitrary code execution:

```java
public void defineClass(String className, byte[] bytecode) {
    classDefinitions.put(className, bytecode);  // No validation!
}
```

**Fix:** Implement bytecode verification with ASM, block dangerous method calls (Runtime.exec, System.exit, etc.).

**Impact:** Arbitrary code execution, complete system compromise.

---

## Medium Priority Findings

### 5. Class Name Injection (MEDIUM)
**File:** `MiniLangRunner.executeCompileMode()`

Class names derived from filenames without sanitization:

```bash
# Special characters in filename
mvn exec:java -Dexec.args="eval 'evil;System.exit(1);.minilang'"
# Generates: MiniLang_evil;System_exit(1);_minilang (invalid!)
```

**Fix:** Sanitize using `Character.isJavaIdentifierPart()`, check reserved keywords, add uniqueness suffix.

---

## Low Priority Findings

### 6. Information Disclosure (LOW)
**File:** Error handling in `MiniLangRunner`

Error messages reveal internal grammar structure and ANTLR details.

**Fix:** Sanitize error messages, separate debug vs production modes.

### 7. Example Programs (LOW)
**File:** Phase 3 examples

No security review process defined for example programs.

**Fix:** Add security checklist and validation tests for examples.

---

## Risk Matrix

| Finding | Severity | Likelihood | Priority | Estimated Fix Time |
|---------|----------|-----------|----------|-------------------|
| Path Traversal | HIGH | HIGH | P0 | 1 day |
| Resource Limits | HIGH | HIGH | P0 | 1 day |
| String Escapes | MEDIUM | MEDIUM | P1 | 1 day |
| ClassLoader | CRITICAL | LOW | P1 | 1 day |
| Class Name Injection | MEDIUM | MEDIUM | P2 | 0.5 day |
| Error Messages | LOW | LOW | P3 | 0.5 day |
| Example Security | LOW | LOW | P3 | 0.5 day |

**Total Additional Development Time:** ~6 days (30% overhead on 20-day plan)

---

## OWASP Top 10 Compliance

| Category | Status | Findings |
|----------|--------|----------|
| A01: Broken Access Control | ❌ VULNERABLE | Path traversal |
| A03: Injection | ⚠️ PARTIAL | Class name, string escapes |
| A04: Insecure Design | ⚠️ PARTIAL | No resource limits |
| A05: Security Misconfiguration | ❌ VULNERABLE | Verbose errors |
| A08: Data Integrity Failures | ❌ VULNERABLE | No bytecode verification |
| All Others | ✅ COMPLIANT | N/A or not applicable |

---

## Recommended Actions

### Immediate (Block PR Merge)

1. Implement file path validation in `MiniLangRunner`
2. Add resource limits to `AstBuilder`
3. Fix string escape processing
4. Add security test suite

### Before Release

5. Implement bytecode verification in `MultiClassLoader`
6. Sanitize class name generation
7. Create SECURITY.md documentation
8. Review all example programs

### Future Enhancements

9. Process isolation/sandboxing
10. Audit logging
11. Bytecode signing
12. External security audit

---

## Security Documentation Requirements

### Files to Create

1. **SECURITY.md** - Complete threat model, security features, known limitations
2. **SecurityUtils.java** - Centralized validation and sanitization utilities
3. **SecurityTest.java** - Comprehensive security test suite

### Files to Update

1. **README.md** - Add security section with warnings and limitations
2. **CLAUDE.md** - Document security review process
3. **pom.xml** - Add OWASP Dependency Check plugin

---

## Key Recommendations for DSL Authors

As MiniLang serves as a reference implementation, it must demonstrate:

1. **Never trust user input** - Validate all file paths, class names, and parameters
2. **Enforce resource limits** - Prevent DoS attacks from malicious or accidental complexity
3. **Verify bytecode** - Don't load arbitrary bytecode without validation
4. **Sanitize errors** - Avoid information disclosure in error messages
5. **Document security** - Make threat model and limitations explicit

---

## Testing Strategy

### Security Test Coverage Required

- Path traversal attack tests (10+ cases)
- Resource exhaustion tests (5+ cases)
- String injection tests (10+ cases)
- Class name injection tests (5+ cases)
- Bytecode verification tests (5+ cases)

### Minimum Coverage Target

- 80% instruction coverage (existing requirement)
- 80% branch coverage (existing requirement)
- 100% security control coverage (NEW requirement)

---

## Review Checklist

Before marking issue #29 as complete:

- [ ] All P0 findings remediated and tested
- [ ] All P1 findings tracked in separate issues
- [ ] Security test suite added with 100% pass rate
- [ ] SECURITY.md created and reviewed
- [ ] README.md security section added
- [ ] Example programs security reviewed
- [ ] External security review scheduled (recommended)
- [ ] Security controls documented in code comments

---

## Conclusion

MiniLang demonstrates sound architecture but requires security enhancements before serving as a reference implementation. The identified vulnerabilities are common in DSL projects and must be addressed to prevent propagation to downstream implementations.

**Estimated Impact:** +6 days development time (26 days total vs. 20 days planned)

**Value Proposition:** Demonstrating secure DSL development practices justifies the additional effort. MiniLang will be widely copied, and security vulnerabilities in the reference implementation would propagate to production systems.

---

## References

- Full audit report: `/Users/I772698/workspaces/jcp/docs/security/2026-03-05-minilang-security-audit-report.md`
- Implementation plan: `/Users/I772698/workspaces/jcp/docs/plans/2026-03-05-feat-minilang-reference-dsl-plan.md`
- OWASP Top 10: https://owasp.org/Top10/
- CWE Database: https://cwe.mitre.org/

---

**Next Steps:**

1. Review this summary with project stakeholders
2. Prioritize P0 findings for immediate implementation
3. Update implementation plan with security gates
4. Schedule follow-up review after Phase 4
