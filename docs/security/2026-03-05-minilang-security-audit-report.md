---
title: MiniLang DSL Implementation - Security Audit Report
date: 2026-03-05
severity: MEDIUM
type: security-review
issue: 29
---

# MiniLang DSL Security Audit Report

## Executive Summary

This security audit evaluates the MiniLang reference DSL implementation plan for potential vulnerabilities. As an educational reference implementation, MiniLang must demonstrate security best practices to prevent DSL authors from copying insecure patterns into production systems.

**Overall Risk Assessment: MEDIUM**

The plan demonstrates generally sound architecture but lacks explicit security controls in several critical areas. The main concerns center around:
- Unvalidated file path handling in the runner
- Missing resource limits for parsing and compilation
- Unsafe ClassLoader implementation allowing arbitrary bytecode loading
- Lack of input validation in ANTLR parser error handling
- Missing security documentation for DSL authors

**Critical Finding:** While MiniLang is intended as educational, it will likely be copy-pasted into production systems. Security vulnerabilities in the reference implementation will propagate to downstream DSLs.

---

## Detailed Security Findings

### 1. INPUT VALIDATION - File Path Handling

**Severity: HIGH**
**Location:** `MiniLangRunner.java` (lines 459-467 in plan)

**Vulnerability:**
```java
String sourceFile = args[1];
String source = Files.readString(Path.of(sourceFile));
```

The runner accepts arbitrary file paths from command-line arguments without validation, enabling:

1. **Path Traversal Attack:**
   ```bash
   mvn exec:java -Dexec.args="eval ../../../../etc/passwd"
   ```

2. **Arbitrary File Read:**
   - Read sensitive configuration files (.env, application.properties)
   - Access database credentials
   - Read SSH keys or certificates
   - Expose source code of other projects

3. **Symbolic Link Following:**
   - Attacker creates symlink to sensitive file
   - Runner follows symlink and reads protected content

**Proof of Concept:**
```bash
# Create malicious symlink
ln -s /etc/passwd malicious.minilang

# Execute runner - reads /etc/passwd content
mvn exec:java -Dexec.args="eval malicious.minilang"
```

**Impact:**
- Information disclosure of sensitive files
- Privilege escalation if run with elevated permissions
- Bypassing file access controls

**Remediation:**

```java
public class MiniLangRunner {
    // Define safe directory for .minilang files
    private static final Path SAFE_BASE_DIR = Paths.get("src/main/resources/examples").toAbsolutePath().normalize();
    private static final int MAX_FILE_SIZE = 1_048_576; // 1MB limit

    private static String readSourceFileSafely(String sourceFile) throws IOException {
        // 1. Normalize and resolve the path
        Path requestedPath = Paths.get(sourceFile).toAbsolutePath().normalize();

        // 2. Verify the path is within safe directory (prevent path traversal)
        if (!requestedPath.startsWith(SAFE_BASE_DIR)) {
            throw new SecurityException(
                "Access denied: File must be within " + SAFE_BASE_DIR +
                ", attempted: " + requestedPath
            );
        }

        // 3. Check for symbolic links (prevent symlink attacks)
        if (Files.isSymbolicLink(requestedPath)) {
            throw new SecurityException(
                "Access denied: Symbolic links are not allowed: " + requestedPath
            );
        }

        // 4. Verify file exists and is regular file
        if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
            throw new IOException("File not found or not a regular file: " + requestedPath);
        }

        // 5. Check file extension
        if (!requestedPath.toString().endsWith(".minilang")) {
            throw new SecurityException(
                "Access denied: Only .minilang files are allowed: " + requestedPath
            );
        }

        // 6. Check file size (prevent memory exhaustion)
        long fileSize = Files.size(requestedPath);
        if (fileSize > MAX_FILE_SIZE) {
            throw new SecurityException(
                "Access denied: File too large (max " + MAX_FILE_SIZE + " bytes): " + fileSize
            );
        }

        // 7. Read with resource limit
        return Files.readString(requestedPath, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        // ... existing validation ...

        String sourceFile = args[1];
        String source = readSourceFileSafely(sourceFile);  // Use safe reader

        // ... rest of implementation ...
    }
}
```

**Additional Mitigations:**
- Add `--allow-file-access` flag for advanced users who need unrestricted access
- Document the security model in README.md
- Provide example of how to extend for production use with proper ACLs
- Add audit logging for file access attempts

---

### 2. RESOURCE LIMITS - Parsing & Compilation Bombs

**Severity: HIGH**
**Location:** `MiniLangRunner.java`, `AstBuilder.java`

**Vulnerability:**

No resource limits on:
1. **Parse tree depth** - Deeply nested expressions cause stack overflow
2. **Token count** - Massive token streams exhaust memory
3. **Compilation time** - Infinite loops in malicious grammar
4. **Generated bytecode size** - Memory exhaustion

**Attack Scenarios:**

**Denial of Service via Deep Nesting:**
```minilang
# 10,000 levels of nested parentheses
let x: int = (((((((((((((((((((((((((((((...))))))))))))))))))))))))))))

# Stack overflow in parser or AST builder
```

**Memory Exhaustion via Large Arrays:**
```minilang
# Array with 10 million elements
let huge: int[] = [1, 1, 1, 1, 1, ... (10 million times)]

# OutOfMemoryError during parsing or compilation
```

**Compilation Time Bomb:**
```minilang
# Pathologically complex expression
func bomb(n: int) -> int {
    return ((((n + 1) * 2) - 3) / 4) % 5 + ... (repeated 1000 times)
}
```

**Impact:**
- Denial of Service (DoS) for build systems
- CI/CD pipeline failures
- Development machine crashes
- Production compiler service outages

**Remediation:**

**Add Parser Limits:**
```java
public class AstBuilder extends MiniLangBaseVisitor<AstNode> {
    private static final int MAX_EXPRESSION_DEPTH = 100;
    private static final int MAX_ARRAY_ELEMENTS = 10_000;
    private static final int MAX_AST_NODES = 100_000;

    private int currentDepth = 0;
    private int nodeCount = 0;

    @Override
    public AstNode visitAddSub(MiniLangParser.AddSubContext ctx) {
        // Check depth limit
        currentDepth++;
        if (currentDepth > MAX_EXPRESSION_DEPTH) {
            throw new SecurityException(
                "Expression nesting too deep (max " + MAX_EXPRESSION_DEPTH + "): " +
                "possible DoS attack or accidental infinite recursion"
            );
        }

        // Check node count
        nodeCount++;
        if (nodeCount > MAX_AST_NODES) {
            throw new SecurityException(
                "Too many AST nodes (max " + MAX_AST_NODES + "): " +
                "program too complex, possible DoS attack"
            );
        }

        try {
            // ... existing logic ...
            return expr;
        } finally {
            currentDepth--;
        }
    }

    @Override
    public AstNode visitArrayLiteralExpr(MiniLangParser.ArrayLiteralExprContext ctx) {
        MiniLangParser.ArrayLiteralContext arrCtx = ctx.arrayLiteral();
        int elementCount = arrCtx.expression().size();

        // Validate array size
        if (elementCount > MAX_ARRAY_ELEMENTS) {
            throw new SecurityException(
                "Array literal too large (max " + MAX_ARRAY_ELEMENTS + " elements): " +
                "found " + elementCount + ", possible DoS attack"
            );
        }

        // ... rest of implementation ...
    }
}
```

**Add Compilation Timeout:**
```java
public class MiniLangRunner {
    private static final long COMPILATION_TIMEOUT_MS = 30_000; // 30 seconds

    private static void executeCompileMode(Block program, String sourceFile)
            throws Exception {
        JcpCompiler compiler = new JcpCompiler();
        String className = "MiniLang_" +
            Path.of(sourceFile).getFileName().toString().replace(".", "_");

        // Compile with timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Class<?>> future = executor.submit(() ->
            compiler.compileAndLoad(program, className)
        );

        try {
            Class<?> clazz = future.get(COMPILATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            Method mainMethod = clazz.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SecurityException(
                "Compilation timeout after " + COMPILATION_TIMEOUT_MS + "ms: " +
                "program too complex or infinite loop detected"
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
```

**Add ANTLR Bail Error Strategy:**
```java
// In MiniLangRunner.main()
MiniLangParser parser = new MiniLangParser(tokens);

// Use BailErrorStrategy instead of default recovery
parser.setErrorHandler(new BailErrorStrategy());

// Set token stream buffer limit
tokens.setMaxBufferSize(100_000); // Limit token buffer
```

**Additional Mitigations:**
- Add `--unsafe` flag to disable limits for trusted input
- Document resource limits in README.md security section
- Provide configuration file for tuning limits per-project
- Add metrics collection for monitoring resource usage

---

### 3. STRING ESCAPE SEQUENCE PROCESSING

**Severity: MEDIUM**
**Location:** `AstBuilder.java` (lines 398-409 in plan)

**Vulnerability:**

The escape sequence processor is overly simplistic and vulnerable to:

1. **Incomplete Escape Handling:**
```java
private String parseStringLiteral(String raw) {
    String content = raw.substring(1, raw.length() - 1);

    return content
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\");  // WRONG ORDER - processes \\\\ before \\n
}
```

**Bug Example:**
```minilang
# User writes: "Line 1\\nLine 2" (escaped backslash + n)
# Expected: "Line 1\nLine 2" (literal backslash-n, not newline)
# Actual: "Line 1
#          Line 2" (incorrect newline)

# Reason: replace("\\\\", "\\") happens LAST, but should be FIRST
```

2. **Missing Unicode Escape Support:**
```minilang
# Unicode escapes not handled
let emoji: string = "Hello \u0041"  # Should be "Hello A"
# Result: literal string "Hello \u0041" (wrong)
```

3. **Missing Hex/Octal Escape Support:**
```minilang
let nul: string = "\x00"  # Hex escape
let bell: string = "\007" # Octal escape
# Not supported - may cause confusion
```

4. **No Validation of Invalid Escapes:**
```minilang
let bad: string = "Invalid \z escape"  # \z is not valid
# Should error, but silently passes through as literal "\z"
```

**Impact:**
- String injection if escapes processed incorrectly
- Data corruption in output
- Security issues if strings used in commands/queries
- Confusion for DSL authors expecting standard behavior

**Remediation:**

**Correct Escape Processing (Order Matters!):**
```java
private String parseStringLiteral(String raw) {
    if (raw.length() < 2 || !raw.startsWith("\"") || !raw.endsWith("\"")) {
        throw new IllegalArgumentException("Invalid string literal: " + raw);
    }

    String content = raw.substring(1, raw.length() - 1);

    // Use StringBuilder for efficient processing
    StringBuilder result = new StringBuilder(content.length());

    for (int i = 0; i < content.length(); i++) {
        char c = content.charAt(i);

        if (c == '\\' && i + 1 < content.length()) {
            char next = content.charAt(i + 1);

            switch (next) {
                case 'n':
                    result.append('\n');
                    i++; // Skip next char
                    break;
                case 't':
                    result.append('\t');
                    i++;
                    break;
                case 'r':
                    result.append('\r');
                    i++;
                    break;
                case '"':
                    result.append('"');
                    i++;
                    break;
                case '\\':
                    result.append('\\');
                    i++;
                    break;
                case 'u':
                    // Unicode escape: \uXXXX
                    if (i + 5 < content.length()) {
                        String hex = content.substring(i + 2, i + 6);
                        try {
                            int codePoint = Integer.parseInt(hex, 16);
                            result.append((char) codePoint);
                            i += 5; // Skip \uXXXX
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                "Invalid unicode escape: \\u" + hex
                            );
                        }
                    } else {
                        throw new IllegalArgumentException(
                            "Incomplete unicode escape at position " + i
                        );
                    }
                    break;
                default:
                    // Invalid escape - throw error instead of silently passing through
                    throw new IllegalArgumentException(
                        "Invalid escape sequence: \\" + next + " at position " + i
                    );
            }
        } else {
            result.append(c);
        }
    }

    return result.toString();
}
```

**Add ANTLR Lexer Validation:**
```antlr
// In MiniLang.g4
STRING: '"' (ESC | ~["\\])* '"' ;

fragment ESC
    : '\\' [ntr"\\]           // Standard escapes
    | '\\u' HEX HEX HEX HEX   // Unicode escape
    ;

fragment HEX: [0-9a-fA-F] ;
```

**Add Unit Tests:**
```java
@Test
void testEscapeSequences() {
    // Backslash handling
    assertEquals("Line 1\\nLine 2", parse("\"Line 1\\\\nLine 2\""));  // Escaped backslash
    assertEquals("Line 1\nLine 2", parse("\"Line 1\\nLine 2\""));    // Newline

    // Quote handling
    assertEquals("Say \"Hi\"", parse("\"Say \\\"Hi\\\"\""));

    // Unicode handling
    assertEquals("Hello A", parse("\"Hello \\u0041\""));

    // Invalid escapes should throw
    assertThrows(IllegalArgumentException.class,
        () -> parse("\"Invalid \\z escape\""));
}
```

**Additional Mitigations:**
- Document supported escape sequences in GRAMMAR.md
- Provide escape sequence reference table in README
- Add warning about raw strings (future enhancement: `r"C:\path\to\file"`)

---

### 4. CLASSLOADER SECURITY - Arbitrary Bytecode Loading

**Severity: CRITICAL**
**Location:** `MultiClassLoader.java`, `JcpCompiler.java`

**Vulnerability:**

The `MultiClassLoader` and `ByteArrayClassLoader` implementations have no security controls:

```java
public class MultiClassLoader extends ClassLoader {
    public void defineClass(String className, byte[] bytecode) {
        classDefinitions.put(className, bytecode);  // No validation!
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = classDefinitions.get(name);
        if (bytecode != null) {
            return defineClass(name, bytecode, 0, bytecode.length);  // Loads arbitrary bytecode!
        }
        return super.findClass(name);
    }
}
```

**Attack Scenarios:**

1. **Malicious Bytecode Injection:**
   - Attacker provides crafted bytecode containing malicious instructions
   - ClassLoader blindly loads and executes the code
   - Bypasses Java security manager (if enabled)

2. **Arbitrary File System Access:**
   - Loaded class contains native method calls
   - Reads/writes files outside sandbox
   - Executes system commands

3. **JVM Crash via Invalid Bytecode:**
   - Malformed bytecode causes JVM segfault
   - Invalid stack frames crash verifier
   - Memory corruption bugs

4. **Privilege Escalation:**
   - Loaded class calls restricted APIs
   - Modifies security manager
   - Accesses protected resources

**Proof of Concept:**
```java
// Malicious MiniLang program that generates harmful bytecode
struct Evil {
    func deleteFiles() -> void {
        # Compiles to bytecode that calls:
        # Runtime.getRuntime().exec("rm -rf /")
    }
}
```

**Impact:**
- **CRITICAL:** Arbitrary code execution
- Complete system compromise
- Data loss/corruption
- Denial of service

**Remediation:**

**Option 1: Add Bytecode Verification (Recommended for Educational Use)**

```java
public class SecureMultiClassLoader extends ClassLoader {
    private static final int MAX_BYTECODE_SIZE = 1_048_576; // 1MB
    private static final Set<String> ALLOWED_PACKAGES = Set.of(
        "java/lang",
        "java/util",
        "java/io",
        "com/elminster/jcp"
    );

    public void defineClass(String className, byte[] bytecode) {
        // 1. Validate bytecode size
        if (bytecode.length > MAX_BYTECODE_SIZE) {
            throw new SecurityException(
                "Bytecode too large (max " + MAX_BYTECODE_SIZE + "): " + bytecode.length
            );
        }

        // 2. Verify bytecode structure with ASM
        try {
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.SKIP_FRAMES);

            // 3. Check for dangerous patterns
            verifyClassSafety(classNode, className);

        } catch (Exception e) {
            throw new SecurityException("Invalid bytecode for class " + className, e);
        }

        classDefinitions.put(className, bytecode);
    }

    private void verifyClassSafety(ClassNode classNode, String expectedName) {
        // Verify class name matches expected
        if (!classNode.name.equals(expectedName.replace('.', '/'))) {
            throw new SecurityException(
                "Class name mismatch: expected " + expectedName +
                ", found " + classNode.name
            );
        }

        // Check method instructions for dangerous calls
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;

                    // Block dangerous API calls
                    if (isDangerousMethod(methodInsn)) {
                        throw new SecurityException(
                            "Forbidden method call: " + methodInsn.owner +
                            "." + methodInsn.name
                        );
                    }
                }
            }
        }
    }

    private boolean isDangerousMethod(MethodInsnNode insn) {
        String owner = insn.owner;
        String name = insn.name;

        // Block Runtime.exec()
        if (owner.equals("java/lang/Runtime") && name.equals("exec")) {
            return true;
        }

        // Block ProcessBuilder
        if (owner.equals("java/lang/ProcessBuilder")) {
            return true;
        }

        // Block System.exit()
        if (owner.equals("java/lang/System") && name.equals("exit")) {
            return true;
        }

        // Block native method loading
        if (owner.equals("java/lang/System") &&
            (name.equals("load") || name.equals("loadLibrary"))) {
            return true;
        }

        // Block reflection (can bypass security)
        if (owner.startsWith("java/lang/reflect/")) {
            return true;
        }

        // Block ClassLoader manipulation
        if (owner.equals("java/lang/ClassLoader")) {
            return true;
        }

        return false;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = classDefinitions.get(name);
        if (bytecode != null) {
            // JVM will verify bytecode structure
            // Our pre-check ensures no dangerous calls
            return defineClass(name, bytecode, 0, bytecode.length);
        }
        return super.findClass(name);
    }
}
```

**Option 2: Use SecurityManager (Legacy, Deprecated in Java 17+)**

```java
public class MiniLangRunner {
    static {
        // Install security manager (if not already set)
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkExec(String cmd) {
                    throw new SecurityException("Execution of external programs not allowed");
                }

                @Override
                public void checkWrite(String file) {
                    // Allow only output directory writes
                    if (!file.startsWith("/tmp/minilang-output/")) {
                        throw new SecurityException("File write not allowed: " + file);
                    }
                }
            });
        }
    }
}
```

**Option 3: Sandbox Execution (Recommended for Production)**

```java
// Use separate process with restricted permissions
ProcessBuilder pb = new ProcessBuilder(
    "java",
    "-Djava.security.manager",
    "-Djava.security.policy=minilang.policy",
    "-cp", "...",
    "com.elminster.minilang.MiniLangRunner",
    "eval", sourceFile
);

// Set resource limits
pb.redirectErrorStream(true);
Process process = pb.start();

// Enforce timeout
if (!process.waitFor(30, TimeUnit.SECONDS)) {
    process.destroyForcibly();
    throw new SecurityException("Execution timeout");
}
```

**Additional Mitigations:**
- Document ClassLoader security model in SECURITY.md
- Add `--trusted` flag to bypass checks for trusted bytecode
- Implement bytecode signing for production deployments
- Use Java modules (JPMS) to restrict package access
- Consider using GraalVM native image for sandboxing

---

### 5. CODE INJECTION - Class Name Generation

**Severity: MEDIUM**
**Location:** `MiniLangRunner.java` (lines 514-515 in plan)

**Vulnerability:**

```java
String className = "MiniLang_" +
    Path.of(sourceFile).getFileName().toString().replace(".", "_");
```

The class name is derived from user-controlled filename without proper sanitization:

**Attack Scenarios:**

1. **Invalid Class Names:**
```bash
# Special characters in filename
mvn exec:java -Dexec.args="eval 'evil;System.exit(1);.minilang'"
# Generates class name: MiniLang_evil;System_exit(1);_minilang
# May cause bytecode generation errors or injection
```

2. **Reserved Keywords:**
```bash
mvn exec:java -Dexec.args="eval class.minilang"
# Generates class name: MiniLang_class
# 'class' is Java reserved keyword
```

3. **Collision Attacks:**
```bash
# Multiple files with same class name after sanitization
evil-file.minilang  -> MiniLang_evil_file_minilang
evil.file.minilang  -> MiniLang_evil_file_minilang  (collision!)
```

**Impact:**
- Bytecode generation failures
- Class loading errors
- Potential injection if class name used in reflection
- Confusion and debugging difficulty

**Remediation:**

```java
private static String sanitizeClassName(String filename) {
    // Remove extension
    if (filename.endsWith(".minilang")) {
        filename = filename.substring(0, filename.length() - 9);
    }

    // Remove any path separators
    filename = filename.replace("/", "").replace("\\", "");

    // Replace invalid Java identifier chars with underscore
    StringBuilder sb = new StringBuilder("MiniLang_");

    for (int i = 0; i < filename.length(); i++) {
        char c = filename.charAt(i);

        if (i == 0) {
            // First character must be Java identifier start
            if (Character.isJavaIdentifierStart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        } else {
            // Subsequent characters must be Java identifier part
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
    }

    String className = sb.toString();

    // Check for reserved keywords
    Set<String> reserved = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for",
        "if", "implements", "import", "instanceof", "int", "interface",
        "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while"
    );

    if (reserved.contains(className.toLowerCase())) {
        className = className + "_Class";
    }

    // Add uniqueness suffix to prevent collisions
    className = className + "_" + System.currentTimeMillis();

    return className;
}

private static void executeCompileMode(Block program, String sourceFile)
        throws Exception {
    JcpCompiler compiler = new JcpCompiler();

    // Use safe class name generation
    String filename = Path.of(sourceFile).getFileName().toString();
    String className = sanitizeClassName(filename);

    Class<?> clazz = compiler.compileAndLoad(program, className);
    Method mainMethod = clazz.getMethod("main", String[].class);
    mainMethod.invoke(null, (Object) new String[]{});
}
```

**Additional Mitigations:**
- Use UUID for class names instead of filename-based
- Document class naming convention in README
- Add unit tests for edge case filenames
- Validate final class name against Java naming rules

---

### 6. ERROR HANDLING - Information Disclosure

**Severity: LOW**
**Location:** `MiniLangRunner.java` (lines 477-485 in plan)

**Vulnerability:**

The error handler reveals detailed syntax error information:

```java
parser.addErrorListener(new BaseErrorListener() {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        System.err.printf("Syntax error at %d:%d - %s%n",
            line, charPositionInLine, msg);  // Reveals internal details
    }
});
```

**Information Leakage:**
- Internal grammar structure exposed
- Parser state information
- Token types and rules
- ANTLR internals

**Attack Scenarios:**

1. **Grammar Reverse Engineering:**
   - Attacker sends various inputs
   - Analyzes error messages
   - Reconstructs grammar rules
   - Finds parser bugs

2. **Fingerprinting:**
   - Error messages reveal ANTLR version
   - Can identify known vulnerabilities
   - Enables targeted attacks

**Impact:**
- Intellectual property disclosure (grammar rules)
- Attack surface reconnaissance
- Parser bug exploitation

**Remediation:**

```java
parser.addErrorListener(new BaseErrorListener() {
    private static final Logger logger = LoggerFactory.getLogger("MiniLangParser");
    private static final boolean VERBOSE_ERRORS =
        Boolean.getBoolean("minilang.verbose.errors");

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {

        // Log full details for debugging (file only)
        logger.error("Syntax error at {}:{} - {} (symbol: {}, exception: {})",
            line, charPositionInLine, msg, offendingSymbol, e);

        // User-facing error (minimal details)
        if (VERBOSE_ERRORS) {
            // Verbose mode for development
            System.err.printf("Syntax error at %d:%d - %s%n",
                line, charPositionInLine, msg);
        } else {
            // Production mode - generic message
            System.err.printf("Syntax error at line %d, column %d%n",
                line, charPositionInLine);
            System.err.println("Please check your MiniLang syntax.");
        }
    }
});
```

**Additional Mitigations:**
- Add `--verbose` flag for detailed errors
- Document error message format
- Sanitize error messages in production mode
- Consider custom error message formatter

---

### 7. EXAMPLE PROGRAMS - Malicious Code Patterns

**Severity: LOW**
**Location:** Example `.minilang` files (Phase 3)

**Concern:**

The plan includes 8 example programs but doesn't specify security review requirements. Examples must not contain:

1. **Infinite Loops:**
```minilang
# BAD EXAMPLE
while true {
    print("spam")
}
```

2. **Resource Exhaustion:**
```minilang
# BAD EXAMPLE
let huge: int[] = [0, 0, 0, ...]  # Millions of elements
```

3. **Misleading Code:**
```minilang
# BAD EXAMPLE - Looks safe but isn't
func "safe"_looking_function() -> void {
    # Actually dangerous operation
}
```

4. **Security Anti-patterns:**
```minilang
# BAD EXAMPLE - Demonstrates bad practice
let password: string = "hardcoded_password"  # DON'T DO THIS
```

**Remediation:**

**Add Security Review Checklist for Examples:**

```markdown
## Example Program Security Checklist

Before adding any example program, verify:

- [ ] No infinite loops or unbounded recursion
- [ ] No excessive resource consumption (large arrays, deep nesting)
- [ ] No hardcoded secrets or sensitive data
- [ ] No security anti-patterns demonstrated without warning
- [ ] Comments explain security implications of code
- [ ] Example is appropriate for educational reference
- [ ] Code follows secure coding guidelines
```

**Add Example Validation Tests:**

```java
@Test
void validateAllExamples() throws Exception {
    Path examplesDir = Paths.get("src/main/resources/examples");

    Files.walk(examplesDir)
        .filter(p -> p.toString().endsWith(".minilang"))
        .forEach(exampleFile -> {
            String source = Files.readString(exampleFile);

            // Check for dangerous patterns
            assertFalse(source.contains("while true"),
                "Example contains infinite loop: " + exampleFile);

            assertFalse(source.contains("password"),
                "Example contains hardcoded password: " + exampleFile);

            // Parse and validate AST
            Block program = parseProgram(source);
            validateAstSafety(program);
        });
}

private void validateAstSafety(Block program) {
    // Check for excessive array sizes
    // Check for deep recursion
    // Check for unbounded loops
    // etc.
}
```

**Add Security Warning Comments to Examples:**

```minilang
# 04-functions.minilang

# SECURITY NOTE: This is an educational example.
# In production DSLs, ensure:
# - Recursion depth limits are enforced
# - Stack overflow protection is enabled
# - Function parameters are validated

func factorial(n: int) -> int {
    # Recursive function - potential stack overflow for large n
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}
```

---

## Security Requirements Summary

### MUST IMPLEMENT (Before Merge)

1. ✅ **File Path Validation**
   - Path traversal protection
   - Symbolic link checks
   - File extension validation
   - Size limits
   - Base directory restriction

2. ✅ **Resource Limits**
   - Maximum expression depth (100)
   - Maximum array size (10,000)
   - Maximum AST nodes (100,000)
   - Compilation timeout (30s)
   - File size limit (1MB)

3. ✅ **String Escape Validation**
   - Correct escape sequence processing
   - Invalid escape rejection
   - Unicode escape support
   - Unit test coverage

4. ✅ **Class Name Sanitization**
   - Remove invalid characters
   - Check reserved keywords
   - Prevent collisions
   - Validate Java identifier rules

### SHOULD IMPLEMENT (Recommended)

5. ⚠️ **Bytecode Verification**
   - Dangerous method call detection
   - Bytecode size limits
   - Structure validation
   - Whitelist allowed APIs

6. ⚠️ **Error Message Sanitization**
   - Minimize information disclosure
   - Separate user vs debug messages
   - Logging for audit trail

7. ⚠️ **Example Security Review**
   - No infinite loops
   - No resource exhaustion
   - Security warning comments
   - Validation tests

### NICE TO HAVE (Future Enhancements)

8. 📋 **Security Documentation**
   - SECURITY.md file
   - Threat model
   - Security best practices guide
   - Incident response procedures

9. 📋 **Sandboxing**
   - Process isolation
   - Resource quotas
   - Network restrictions
   - Filesystem permissions

10. 📋 **Audit Logging**
    - File access logs
    - Compilation events
    - Error occurrences
    - Performance metrics

---

## OWASP Top 10 Compliance Assessment

### A01:2021 – Broken Access Control

**Status: VULNERABLE**

- ❌ No access control on file paths
- ❌ No user authentication/authorization
- ❌ Arbitrary file read possible

**Mitigation:** Implement file path validation (Finding #1)

### A02:2021 – Cryptographic Failures

**Status: NOT APPLICABLE**

- ✅ No sensitive data transmission
- ✅ No cryptographic operations
- ℹ️ Educational tool, no production crypto needed

### A03:2021 – Injection

**Status: VULNERABLE**

- ⚠️ Class name injection possible (Finding #5)
- ⚠️ String escape injection possible (Finding #3)
- ✅ No SQL/command injection (no external systems)

**Mitigation:** Implement class name sanitization and proper escape handling

### A04:2021 – Insecure Design

**Status: NEEDS REVIEW**

- ⚠️ No resource limits (Finding #2)
- ⚠️ No security requirements documented
- ℹ️ Educational focus, but should model secure design

**Mitigation:** Add resource limits and document security model

### A05:2021 – Security Misconfiguration

**Status: VULNERABLE**

- ❌ No security headers/configurations
- ❌ Default configuration insecure
- ❌ Error messages too verbose (Finding #6)

**Mitigation:** Secure defaults, error message sanitization

### A06:2021 – Vulnerable and Outdated Components

**Status: ACCEPTABLE**

- ✅ ANTLR 4.13.1 (current version)
- ✅ ASM 9.6 (current version)
- ✅ Dependencies specified in POM
- ℹ️ Need regular updates

**Recommendation:** Add Dependabot for automatic updates

### A07:2021 – Identification and Authentication Failures

**Status: NOT APPLICABLE**

- ℹ️ No authentication mechanism needed
- ℹ️ Local command-line tool

### A08:2021 – Software and Data Integrity Failures

**Status: VULNERABLE**

- ❌ No bytecode verification (Finding #4)
- ❌ No class signature validation
- ❌ Arbitrary code execution possible

**Mitigation:** Implement bytecode verification

### A09:2021 – Security Logging and Monitoring Failures

**Status: VULNERABLE**

- ❌ No audit logging
- ❌ No security event monitoring
- ❌ No alerting mechanism

**Mitigation:** Add security event logging (optional for educational tool)

### A10:2021 – Server-Side Request Forgery (SSRF)

**Status: NOT APPLICABLE**

- ℹ️ No server-side requests
- ℹ️ No external network calls

---

## Risk Matrix

| Finding | Severity | Likelihood | Risk Level | Priority |
|---------|----------|-----------|------------|----------|
| #1 File Path Handling | HIGH | HIGH | **CRITICAL** | P0 |
| #2 Resource Limits | HIGH | HIGH | **CRITICAL** | P0 |
| #3 String Escapes | MEDIUM | MEDIUM | **HIGH** | P1 |
| #4 ClassLoader Security | CRITICAL | LOW | **HIGH** | P1 |
| #5 Class Name Injection | MEDIUM | MEDIUM | **MEDIUM** | P2 |
| #6 Error Messages | LOW | LOW | **LOW** | P3 |
| #7 Example Programs | LOW | LOW | **LOW** | P3 |

**Priority Definitions:**
- **P0 (Critical):** Must fix before merge - blocks PR approval
- **P1 (High):** Should fix before release - can merge with issue tracking
- **P2 (Medium):** Fix in next iteration - document workaround
- **P3 (Low):** Enhancement - add to backlog

---

## Recommended Implementation Roadmap

### Phase 1 Additions (Project Setup) - SECURITY GATE

**Before completing Phase 1:**
- [ ] Add `SecurityUtils.java` helper class
- [ ] Implement file path validation
- [ ] Add resource limit constants
- [ ] Create `SECURITY.md` documentation

**Estimated Effort:** +1 day

### Phase 2 Additions (AST Builder) - SECURITY GATE

**Before completing Phase 2:**
- [ ] Implement proper string escape processing
- [ ] Add AST depth/size validation
- [ ] Add resource limit checks in visitor methods
- [ ] Unit tests for security constraints

**Estimated Effort:** +1 day

### Phase 3 Additions (Examples) - SECURITY GATE

**Before completing Phase 3:**
- [ ] Security review of all examples
- [ ] Add security warning comments
- [ ] Example validation tests
- [ ] No malicious patterns

**Estimated Effort:** +0.5 day

### Phase 4 Additions (Runner) - SECURITY GATE

**Before completing Phase 4:**
- [ ] Integrate file path validation
- [ ] Add compilation timeout
- [ ] Implement class name sanitization
- [ ] Sanitize error messages
- [ ] Add security flags (--trusted, --unsafe)

**Estimated Effort:** +1 day

### Phase 5 Additions (Testing) - SECURITY GATE

**Before completing Phase 5:**
- [ ] Add security test suite
- [ ] Path traversal attack tests
- [ ] DoS attack tests (resource limits)
- [ ] String injection tests
- [ ] Class name injection tests

**Estimated Effort:** +1 day

### Phase 6 Additions (Documentation) - SECURITY GATE

**Before completing Phase 6:**
- [ ] Add SECURITY.md with threat model
- [ ] Document all security features
- [ ] Add security best practices guide
- [ ] Security section in README
- [ ] Known limitations documented

**Estimated Effort:** +1 day

**Total Additional Effort:** ~6 days
**Original Estimate:** 20 days
**New Estimate with Security:** **26 days**

---

## Documentation Requirements

### SECURITY.md (Required)

```markdown
# MiniLang Security Documentation

## Threat Model

MiniLang is an educational DSL demonstrating JCP integration. It is **NOT** designed for production use without additional security hardening.

### Trust Boundaries

- **Trusted:** Source code in `src/main/resources/examples/`
- **Untrusted:** User-provided `.minilang` files
- **Trusted:** JCP core library bytecode
- **Untrusted:** Generated bytecode from user programs

### Assets

- Developer's file system
- Build environment resources (CPU, memory)
- Generated bytecode
- JVM process integrity

### Threats

1. **Path Traversal:** Malicious file path reads sensitive files
2. **Resource Exhaustion:** Large programs cause DoS
3. **Code Injection:** Bytecode contains malicious instructions
4. **Information Disclosure:** Error messages leak internal details

## Security Features

### File Access Control

MiniLang restricts file access to `src/main/resources/examples/` by default.

Override with `--allow-file-access` flag (use with caution):
```bash
mvn exec:java -Dexec.args="eval --allow-file-access /path/to/file.minilang"
```

### Resource Limits

| Resource | Limit | Configurable |
|----------|-------|--------------|
| Expression depth | 100 levels | `-Dminilang.max.depth=N` |
| Array size | 10,000 elements | `-Dminilang.max.array=N` |
| AST nodes | 100,000 nodes | `-Dminilang.max.nodes=N` |
| Compilation timeout | 30 seconds | `-Dminilang.timeout=N` |
| File size | 1 MB | `-Dminilang.max.file.size=N` |

### Bytecode Verification

Generated bytecode is verified for:
- ✅ Valid class structure (ASM validation)
- ❌ Dangerous method calls (future enhancement)
- ❌ Native method loading (future enhancement)

## Security Recommendations for DSL Authors

1. **Never trust user input** - Validate all file paths and parameters
2. **Enforce resource limits** - Prevent DoS attacks
3. **Sanitize error messages** - Avoid information disclosure
4. **Verify generated bytecode** - Use SecurityManager or sandboxing
5. **Document security model** - Make threat model explicit

## Known Limitations

- No bytecode signature verification
- No process isolation/sandboxing
- No network request restrictions
- No audit logging
- SecurityManager deprecated (Java 17+)

## Reporting Security Issues

Report security vulnerabilities to: security@elminster.com

Do NOT open public GitHub issues for security problems.

## Version History

- v1.0.0 - Initial release with basic security controls
```

### README.md Security Section (Required)

Add to main README:

```markdown
## Security Considerations

⚠️ **MiniLang is an educational reference implementation.**

It is **NOT** production-ready and should not be used in security-sensitive environments without additional hardening.

### Security Features

- File access restricted to examples directory
- Resource limits prevent DoS attacks
- Input validation on file paths and class names
- Bytecode structure verification

### Security Limitations

- No process sandboxing
- No audit logging
- Limited bytecode verification
- No cryptographic verification

See [SECURITY.md](SECURITY.md) for complete threat model and security details.
```

---

## Testing Strategy for Security

### Security Test Suite

Create `SecurityTest.java`:

```java
@Nested
@DisplayName("Security Tests")
class SecurityTest {

    @Nested
    @DisplayName("File Path Validation")
    class FilePathValidationTests {

        @Test
        void shouldBlockPathTraversal() {
            assertThrows(SecurityException.class, () ->
                MiniLangRunner.main(new String[]{"eval", "../../../../etc/passwd"})
            );
        }

        @Test
        void shouldBlockSymlinks() throws IOException {
            Path symlink = Files.createSymbolicLink(
                Paths.get("examples/symlink.minilang"),
                Paths.get("/etc/passwd")
            );

            try {
                assertThrows(SecurityException.class, () ->
                    MiniLangRunner.main(new String[]{"eval", symlink.toString()})
                );
            } finally {
                Files.deleteIfExists(symlink);
            }
        }

        @Test
        void shouldBlockNonMinilangFiles() {
            assertThrows(SecurityException.class, () ->
                MiniLangRunner.main(new String[]{"eval", "examples/data.txt"})
            );
        }
    }

    @Nested
    @DisplayName("Resource Limits")
    class ResourceLimitTests {

        @Test
        void shouldRejectDeeplyNestedExpressions() {
            // Generate 200-level nested expression (limit is 100)
            StringBuilder deep = new StringBuilder("let x: int = ");
            for (int i = 0; i < 200; i++) deep.append("(");
            deep.append("1");
            for (int i = 0; i < 200; i++) deep.append(")");

            assertThrows(SecurityException.class, () ->
                parseProgram(deep.toString())
            );
        }

        @Test
        void shouldRejectLargeArrayLiterals() {
            // Generate 20,000-element array (limit is 10,000)
            StringBuilder huge = new StringBuilder("let arr: int[] = [");
            for (int i = 0; i < 20_000; i++) {
                if (i > 0) huge.append(", ");
                huge.append("1");
            }
            huge.append("]");

            assertThrows(SecurityException.class, () ->
                parseProgram(huge.toString())
            );
        }

        @Test
        void shouldTimeoutLongCompilation() {
            // Extremely complex expression
            StringBuilder complex = new StringBuilder("let x: int = ");
            for (int i = 0; i < 1000; i++) {
                complex.append("((1 + 2) * 3) + ");
            }
            complex.append("0");

            assertThrows(TimeoutException.class, () ->
                compileWithTimeout(complex.toString(), 1000) // 1 second timeout
            );
        }
    }

    @Nested
    @DisplayName("String Escape Validation")
    class StringEscapeValidationTests {

        @Test
        void shouldRejectInvalidEscapes() {
            assertThrows(IllegalArgumentException.class, () ->
                parseStringLiteral("\"Invalid \\z escape\"")
            );
        }

        @Test
        void shouldHandleEscapedBackslash() {
            assertEquals("Line 1\\nLine 2",
                parseStringLiteral("\"Line 1\\\\nLine 2\""));
        }

        @Test
        void shouldHandleUnicodeEscapes() {
            assertEquals("Hello A",
                parseStringLiteral("\"Hello \\u0041\""));
        }
    }

    @Nested
    @DisplayName("Class Name Sanitization")
    class ClassNameSanitizationTests {

        @Test
        void shouldSanitizeSpecialCharacters() {
            assertEquals("MiniLang_evil_System_exit_1__minilang_" + timestamp(),
                sanitizeClassName("evil;System.exit(1);.minilang"));
        }

        @Test
        void shouldHandleReservedKeywords() {
            assertEquals("MiniLang_class_Class_" + timestamp(),
                sanitizeClassName("class.minilang"));
        }

        @Test
        void shouldPreventCollisions() {
            String name1 = sanitizeClassName("evil-file.minilang");
            String name2 = sanitizeClassName("evil.file.minilang");

            assertNotEquals(name1, name2, "Class names should not collide");
        }
    }
}
```

---

## Acceptance Criteria Updates

Add to existing acceptance criteria:

### Security Requirements (New)

**File Access Control:**
- [ ] Path traversal blocked
- [ ] Symbolic links blocked
- [ ] File extension validated
- [ ] Size limits enforced
- [ ] Base directory restriction works

**Resource Limits:**
- [ ] Expression depth limit enforced (100)
- [ ] Array size limit enforced (10,000)
- [ ] AST node limit enforced (100,000)
- [ ] Compilation timeout enforced (30s)
- [ ] File size limit enforced (1MB)

**Input Validation:**
- [ ] String escapes processed correctly
- [ ] Invalid escapes rejected
- [ ] Class names sanitized
- [ ] Reserved keywords handled
- [ ] Error messages sanitized

**Security Testing:**
- [ ] Path traversal tests pass
- [ ] DoS attack tests pass
- [ ] String injection tests pass
- [ ] Class name injection tests pass
- [ ] Example validation tests pass

**Security Documentation:**
- [ ] SECURITY.md complete
- [ ] README security section added
- [ ] Threat model documented
- [ ] Known limitations listed
- [ ] Security best practices guide

---

## Conclusion

MiniLang is an educational project that will serve as a template for DSL authors. The security vulnerabilities identified in this audit must be addressed to prevent propagation of insecure patterns into production DSLs.

**Key Takeaways:**

1. **File path validation is critical** - Prevent path traversal attacks
2. **Resource limits prevent DoS** - Always bound user-controlled resources
3. **String processing is dangerous** - Use state machines, not regex replace
4. **ClassLoaders need validation** - Arbitrary bytecode = arbitrary code execution
5. **Error messages leak info** - Sanitize user-facing errors

**Recommended Actions:**

1. Implement P0 findings (#1, #2) before Phase 4 completion
2. Implement P1 findings (#3, #4) before Phase 5 completion
3. Document security model in SECURITY.md during Phase 6
4. Add security test suite in Phase 5
5. Consider external security review before 1.0 release

With these security enhancements, MiniLang will demonstrate not just JCP integration, but also secure DSL development practices.

---

## References

### Internal References
- `/Users/I772698/workspaces/jcp/docs/plans/2026-03-05-feat-minilang-reference-dsl-plan.md` - Implementation plan
- `/Users/I772698/workspaces/jcp/core/src/main/java/com/elminster/jcp/compile/MultiClassLoader.java` - ClassLoader implementation
- `/Users/I772698/workspaces/jcp/core/src/main/java/com/elminster/jcp/compile/JcpCompiler.java` - Compiler implementation
- `/Users/I772698/workspaces/jcp/GUIDELINES.md` - Development guidelines
- `/Users/I772698/workspaces/jcp/CLAUDE.md` - Project overview

### External References
- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [CWE-22: Path Traversal](https://cwe.mitre.org/data/definitions/22.html)
- [CWE-400: Resource Exhaustion](https://cwe.mitre.org/data/definitions/400.html)
- [CWE-94: Code Injection](https://cwe.mitre.org/data/definitions/94.html)
- [CWE-117: Log Injection](https://cwe.mitre.org/data/definitions/117.html)
- [Java ClassLoader Security](https://docs.oracle.com/javase/8/docs/technotes/guides/security/spec/security-spec.doc3.html)
- [ASM Bytecode Verification](https://asm.ow2.io/)
- [ANTLR Security Best Practices](https://github.com/antlr/antlr4/blob/master/doc/faq/general.md)

---

**Report Author:** Claude Code Security Specialist
**Report Date:** 2026-03-05
**Report Version:** 1.0
**Classification:** Internal Review
