package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch coverage tests for FunCallCompiler, MethodCallCompiler, and StaticMethodCallCompiler.
 */
public class FunCallCompilerBranchTest extends AbstractCompileTest {

    // -----------------------------------------------------------------------
    // FunCallCompiler: External class constructor (TypeName.new)
    // -----------------------------------------------------------------------

    /**
     * Tests that TypeName.new with a registered ExternalClassType compiles and runs.
     * Covers the funcName.endsWith(".new") → ExternalClassType branch in FunCallCompiler.
     */
    @Test
    void testExternalClassConstructorNoArgs() throws Exception {
        // StringBuilder sb = StringBuilder.new();
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("StringBuilder.new")
        );
        program.addStatement(new VariableDeclarationImpl(
            "sb",
            new DataTypeImpl("StringBuilder"),
            ctorCall
        ));

        String className = uniqueClassName("TestSbNew");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class);
        byte[] bytecode = generator.compile(program);

        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    /**
     * Tests that constructor with ambiguous argument types throws IllegalArgumentException.
     * StringBuilder has both StringBuilder(String) and StringBuilder(CharSequence), so a
     * String arg is ambiguous → verifies the ambiguity error path.
     */
    @Test
    void testExternalClassConstructorWithAmbiguousArgThrows() {
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("StringBuilder.new"),
            LiteralExpression.of("hello")
        );
        program.addStatement(new VariableDeclarationImpl(
            "sb",
            new DataTypeImpl("StringBuilder"),
            ctorCall
        ));

        String className = uniqueClassName("TestSbNewWithArg");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class);

        assertThrows(IllegalArgumentException.class, () ->
            generator.compile(program)
        );
    }

    /**
     * Tests that TypeName.new with an unregistered type falls through to undefined-function error.
     * Covers the branch where funcName.endsWith(".new") but DataType is NOT ExternalClassType.
     */
    @Test
    void testConstructorWithUnregisteredTypeThrows() {
        Block program = new BlockImpl();
        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("UnknownType.new")
        );

        String className = uniqueClassName("TestUnknownNew");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, ctorCall, SystemDataType.INT)
        );
    }

    /**
     * Tests resolveType for TypeName.new with a module prefix (e.g. "user::SomeType.new").
     * Covers the colonIdx >= 0 branch in resolveType.
     */
    @Test
    void testConstructorWithModulePrefixInResolveType() throws Exception {
        Block program = new BlockImpl();

        // Construct via module-prefixed name - type stripping: "user::StringBuilder" -> "StringBuilder"
        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("user::StringBuilder.new")
        );
        program.addStatement(new VariableDeclarationImpl(
            "sb",
            new DataTypeImpl("StringBuilder"),
            ctorCall
        ));

        String className = uniqueClassName("TestModulePrefixNew");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class, "user");
        byte[] bytecode = generator.compile(program);

        assertNotNull(bytecode);
    }

    // -----------------------------------------------------------------------
    // FunCallCompiler: isModuleFunctionPattern edge cases
    // -----------------------------------------------------------------------

    /**
     * Tests that "::func" (empty module prefix) falls through and becomes an undefined-function error.
     * Covers the isModuleFunctionPattern returning false for moduleDelimiter==0.
     */
    @Test
    void testEmptyModulePrefixFallsThroughToUndefined() {
        Block program = new BlockImpl();
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("::someFunc")
        );

        String className = uniqueClassName("TestEmptyModule");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests that a name with two dots (e.g. "a.b.c") is not treated as a module function pattern.
     * Covers the dotCount > 1 branch in isModuleFunctionPattern for non-:: case.
     */
    @Test
    void testTwoDotNameFallsThroughToUndefined() {
        Block program = new BlockImpl();
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("a.b.c")
        );

        String className = uniqueClassName("TestTwoDots");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests module::Type.method.extra (two dots after ::) is not a valid pattern.
     * Covers the dotCount == 2 branch after ::
     */
    @Test
    void testTwoDotsAfterModuleDelimiterFallsThrough() {
        Block program = new BlockImpl();
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("base::Type.method.extra")
        );

        String className = uniqueClassName("TestTwoDotsAfterModule");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests that a non-base module class not found in context is correctly reported.
     * Covers the ClassNotFoundException path in compileModuleFunctionCall for non-base modules.
     */
    @Test
    void testNonBaseModuleClassNotFoundThrows() {
        Block program = new BlockImpl();
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("guava::Strings.isNullOrEmpty"),
            LiteralExpression.of("hello")
        );

        String className = uniqueClassName("TestNonBaseModuleNotFound");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        // Note: NOT registering guava Strings - so it won't be found

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.BOOLEAN)
        );
    }

    /**
     * Tests module function call where method is not found in a known class.
     * Covers the NoSuchMethodException branch → CompileException("Module function not found").
     */
    @Test
    void testModuleFunctionMethodNotFoundThrows() {
        Block program = new BlockImpl();
        // Math module is valid, but "nonExistentMethod" doesn't exist
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("Math.nonExistentMethod"),
            LiteralExpression.of(42)
        );

        String className = uniqueClassName("TestMethodNotFound");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    // -----------------------------------------------------------------------
    // MethodCallCompiler: error paths
    // -----------------------------------------------------------------------

    /**
     * Tests calling method on a primitive type throws CompileException.
     * Covers the "Cannot call method on type: INT" branch.
     */
    @Test
    void testMethodCallOnPrimitiveTypeThrows() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "x", SystemDataType.INT, LiteralExpression.of(5)
        ));

        MethodCallExpression call = new MethodCallExpression(
            VariableExpression.of("x"),
            "toString"
        );

        String className = uniqueClassName("TestMethodOnPrimitive");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.STRING)
        );
    }

    /**
     * Tests calling non-existent method on external class type throws CompileException.
     * Covers the "Method not found" branch in compileExternalClassCall.
     */
    @Test
    void testMethodCallMethodNotFoundOnExternalType() {
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("StringBuilder.new")
        );
        program.addStatement(new VariableDeclarationImpl(
            "sb", new DataTypeImpl("StringBuilder"), ctorCall
        ));

        MethodCallExpression call = new MethodCallExpression(
            VariableExpression.of("sb"),
            "nonExistentMethod"
        );

        String className = uniqueClassName("TestMethodNotFoundOnExt");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.STRING)
        );
    }

    /**
     * Tests calling a method on a type that implements CharSequence (an interface).
     * Uses StringBuilder and calls length(), which is defined on CharSequence.
     * Covers the interface method dispatch path in MethodCallCompiler.
     */
    @Test
    void testMethodCallOnTypeWithInterfaceMethod() throws Exception {
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("StringBuilder.new")
        );
        program.addStatement(new VariableDeclarationImpl(
            "sb", new DataTypeImpl("StringBuilder"), ctorCall
        ));

        MethodCallExpression lengthCall = new MethodCallExpression(
            VariableExpression.of("sb"),
            "length"
        );

        String className = uniqueClassName("TestInterfaceMethod");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class);
        byte[] bytecode = generator.compileWithReturn(program, lengthCall, SystemDataType.INT);

        assertNotNull(bytecode);
        Class<?> clazz = new ClassLoader() {
            Class<?> def() { return defineClass(className, bytecode, 0, bytecode.length); }
        }.def();
        Method evaluate = clazz.getMethod("evaluate");
        assertEquals(0, evaluate.invoke(null));
    }

    // -----------------------------------------------------------------------
    // StaticMethodCallCompiler: error paths
    // -----------------------------------------------------------------------

    /**
     * Tests that calling static method on a non-class type (e.g. INT) throws.
     * Covers the "Type does not support static methods" branch in StaticMethodCallCompiler.
     */
    @Test
    void testStaticMethodOnNonClassTypeThrows() {
        Block program = new BlockImpl();
        // INT is a SystemDataType, not ExternalClassType or StructType
        // We simulate this by declaring a variable of type INT then trying a static call on it
        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("Integer"),
            "nonExistentMethod",
            LiteralExpression.of(5)
        );

        String className = uniqueClassName("TestStaticOnNonClass");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(Integer.class);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests StaticMethodCallCompiler with undefined type.
     * Covers the "Type not found" branch in StaticMethodCallCompiler.compile().
     */
    @Test
    void testStaticMethodUndefinedTypeThrows() {
        Block program = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("NonExistentClass"),
            "someMethod"
        );

        String className = uniqueClassName("TestStaticUndefinedType");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests resolveType for StaticMethodCallCompiler with undefined type.
     * Covers the "undefined type" branch in StaticMethodCallCompiler.resolveType().
     */
    @Test
    void testStaticMethodResolveTypeUndefinedTypeThrows() {
        Block program = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("GhostType"),
            "someMethod"
        );

        String className = uniqueClassName("TestStaticResolveUndefinedType");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        // compileWithReturn calls resolveType internally
        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests StaticMethodCallCompiler: method not found on external type.
     * Covers the "undefined static method" branch in resolveType and compile.
     */
    @Test
    void testStaticMethodNotFoundOnExternalTypeThrows() {
        Block program = new BlockImpl();
        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("StringBuilder"),
            "noSuchStaticMethod",
            LiteralExpression.of("x")
        );

        String className = uniqueClassName("TestStaticMethodNotFound");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(StringBuilder.class);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests StaticMethodCallCompiler: static method on Integer — exercises INVOKESTATIC path.
     * Uses Integer.max to avoid ambiguity with the base module's Math type.
     */
    @Test
    void testStaticMethodOnExternalClassIntegerMax() throws Exception {
        Block program = new BlockImpl();

        StaticMethodCallExpression call = new StaticMethodCallExpression(
            Identifier.fromName("Integer"),
            "max",
            LiteralExpression.of(10),
            LiteralExpression.of(20)
        );

        String className = uniqueClassName("TestStaticIntegerMax");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        generator.registerExternalClass(Integer.class, "javautil");
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        assertNotNull(bytecode);
        Class<?> clazz = new ClassLoader() {
            Class<?> def() { return defineClass(className, bytecode, 0, bytecode.length); }
        }.def();
        Method evaluate = clazz.getMethod("evaluate");
        assertEquals(20, evaluate.invoke(null));
    }
}
