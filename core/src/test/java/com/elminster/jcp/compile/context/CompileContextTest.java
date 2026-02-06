package com.elminster.jcp.compile.context;

import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.context.CompileContext.FunctionSignature;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.context.CompileContext.LoopLabels;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Label;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompileContext.
 */
class CompileContextTest {

    private CompileContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");
    }

    @Nested
    class LocalVariableTests {

        @Test
        void testAllocateLocal_Int() {
            int index = ctx.allocateLocal("x", SystemDataType.INT);
            assertEquals(0, index);
            assertEquals(1, ctx.getLocalCount());
        }

        @Test
        void testAllocateLocal_Double() {
            int index = ctx.allocateLocal("d", SystemDataType.DOUBLE);
            assertEquals(0, index);
            assertEquals(2, ctx.getLocalCount()); // Double takes 2 slots
        }

        @Test
        void testAllocateLocal_MultipleVariables() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.DOUBLE);
            ctx.allocateLocal("c", SystemDataType.BOOLEAN);

            assertEquals(4, ctx.getLocalCount()); // 1 + 2 + 1
        }

        @Test
        void testGetLocal_Exists() {
            ctx.allocateLocal("x", SystemDataType.INT);
            LocalVariable local = ctx.getLocal("x");

            assertNotNull(local);
            assertEquals(0, local.getIndex());
            assertEquals(SystemDataType.INT, local.getType());
            assertEquals("x", local.getName());
        }

        @Test
        void testGetLocal_NotExists() {
            assertNull(ctx.getLocal("unknown"));
        }

        @Test
        void testHasLocal() {
            ctx.allocateLocal("x", SystemDataType.INT);

            assertTrue(ctx.hasLocal("x"));
            assertFalse(ctx.hasLocal("y"));
        }

        @Test
        void testAllocateLocal_WithCheck() {
            ctx.allocateLocal("x", SystemDataType.INT);

            assertTrue(ctx.hasLocal("x"));
            assertEquals(SystemDataType.INT, ctx.getLocal("x").getType());
        }

        @Test
        void testSetNextLocalIndex() {
            ctx.setNextLocalIndex(5);
            int index = ctx.allocateLocal("x", SystemDataType.INT);
            assertEquals(5, index);
        }
    }

    @Nested
    class ScopeTests {

        @Test
        void testChildContext_InheritsLocalIndex() {
            ctx.allocateLocal("x", SystemDataType.INT);
            CompileContext child = ctx.createChildContext();

            int childIndex = child.allocateLocal("y", SystemDataType.INT);
            assertEquals(1, childIndex); // Continues from parent's index
        }

        @Test
        void testChildContext_InheritsClassName() {
            ctx.setClassName("ParentClass");
            CompileContext child = ctx.createChildContext();

            assertEquals("ParentClass", child.getClassName());
        }

        @Test
        void testGetLocal_FromParent() {
            ctx.allocateLocal("x", SystemDataType.INT);
            CompileContext child = ctx.createChildContext();

            assertNotNull(child.getLocal("x")); // Should find from parent
        }

        @Test
        void testGetParent() {
            CompileContext child = ctx.createChildContext();
            assertEquals(ctx, child.getParent());
        }
    }

    @Nested
    class LoopTests {

        @Test
        void testPushAndPopLoop() {
            Label start = new Label();
            Label end = new Label();
            ctx.pushLoop(start, end);

            assertTrue(ctx.isInLoop());

            LoopLabels labels = ctx.currentLoop();
            assertEquals(start, labels.getStartLabel());
            assertEquals(end, labels.getEndLabel());

            ctx.popLoop();
            assertFalse(ctx.isInLoop());
        }

        @Test
        void testNestedLoops() {
            Label start1 = new Label();
            Label end1 = new Label();
            Label start2 = new Label();
            Label end2 = new Label();

            ctx.pushLoop(start1, end1);
            ctx.pushLoop(start2, end2);

            // Inner loop should be current
            assertEquals(start2, ctx.currentLoop().getStartLabel());

            ctx.popLoop();
            assertEquals(start1, ctx.currentLoop().getStartLabel());

            ctx.popLoop();
            assertFalse(ctx.isInLoop());
        }

        @Test
        void testCurrentLoop_FromParent() {
            Label start = new Label();
            Label end = new Label();
            ctx.pushLoop(start, end);

            CompileContext child = ctx.createChildContext();
            assertTrue(child.isInLoop());
            assertEquals(start, child.currentLoop().getStartLabel());
        }

        @Test
        void testCurrentLoop_NotInLoop() {
            assertNull(ctx.currentLoop());
        }
    }

    @Nested
    class DataTypeTests {

        @Test
        void testAddAndGetDataType() {
            DataTypeImpl customType = new DataTypeImpl("CustomType");
            ctx.addDataType(customType);

            assertEquals(customType, ctx.getDataType("CustomType"));
        }

        @Test
        void testGetDataType_NotExists() {
            assertNull(ctx.getDataType("Unknown"));
        }

        @Test
        void testGetDataType_FromParent() {
            DataTypeImpl customType = new DataTypeImpl("ParentType");
            ctx.addDataType(customType);

            CompileContext child = ctx.createChildContext();
            assertEquals(customType, child.getDataType("ParentType"));
        }
    }

    @Nested
    class FunctionRegistryTests {

        @Test
        void testRegisterAndLookupFunction() {
            ParameterDef[] params = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            ctx.registerFunction("add", params, SystemDataType.INT);

            FunctionSignature sig = ctx.lookupFunction("add", new SystemDataType[]{SystemDataType.INT, SystemDataType.INT});

            assertNotNull(sig);
            assertEquals("add", sig.getName());
            assertEquals(SystemDataType.INT, sig.getReturnType());
            assertEquals("(II)I", sig.getDescriptor());
        }

        @Test
        void testLookupFunction_NotFound() {
            assertNull(ctx.lookupFunction("unknown", new SystemDataType[]{SystemDataType.INT}));
        }

        @Test
        void testLookupFunction_NoArgs() {
            ctx.registerFunction("noArgs", null, SystemDataType.VOID);

            FunctionSignature sig = ctx.lookupFunction("noArgs", null);
            assertNotNull(sig);
            assertEquals("()V", sig.getDescriptor());
        }

        @Test
        void testRegisterFunction_FromChild() {
            CompileContext child = ctx.createChildContext();
            child.registerFunction("childFunc", null, SystemDataType.INT);

            // Should be registered at root
            assertNotNull(ctx.lookupFunction("childFunc", null));
        }

        @Test
        void testLookupFunction_FromChild() {
            ctx.registerFunction("rootFunc", null, SystemDataType.INT);

            CompileContext child = ctx.createChildContext();
            assertNotNull(child.lookupFunction("rootFunc", null));
        }

        @Test
        void testFunctionOverload() {
            ParameterDef[] params1 = {ParameterDef.of("a", SystemDataType.INT)};
            ParameterDef[] params2 = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };

            ctx.registerFunction("func", params1, SystemDataType.INT);
            ctx.registerFunction("func", params2, SystemDataType.INT);

            FunctionSignature sig1 = ctx.lookupFunction("func", new SystemDataType[]{SystemDataType.INT});
            FunctionSignature sig2 = ctx.lookupFunction("func", new SystemDataType[]{SystemDataType.INT, SystemDataType.INT});

            assertNotNull(sig1);
            assertNotNull(sig2);
            assertEquals("(I)I", sig1.getDescriptor());
            assertEquals("(II)I", sig2.getDescriptor());
        }
    }

    @Nested
    class GeneratedClassTests {

        @Test
        void testAddAndGetGeneratedClass() {
            byte[] bytecode = new byte[]{1, 2, 3};
            ctx.addGeneratedClass("TestStruct", bytecode);

            assertArrayEquals(bytecode, ctx.getGeneratedClasses().get("TestStruct"));
        }

        @Test
        void testAddGeneratedClass_FromChild() {
            byte[] bytecode = new byte[]{1, 2, 3};
            CompileContext child = ctx.createChildContext();
            child.addGeneratedClass("ChildStruct", bytecode);

            // Should be registered at root
            assertNotNull(ctx.getGeneratedClasses().get("ChildStruct"));
        }

        @Test
        void testGetGeneratedClasses_FromChild() {
            byte[] bytecode = new byte[]{1, 2, 3};
            ctx.addGeneratedClass("RootStruct", bytecode);

            CompileContext child = ctx.createChildContext();
            assertNotNull(child.getGeneratedClasses().get("RootStruct"));
        }
    }

    @Nested
    class FunctionReturnTypeTests {

        @Test
        void testSetAndGetCurrentFunctionReturnType() {
            ctx.setCurrentFunctionReturnType(SystemDataType.INT);
            assertEquals(SystemDataType.INT, ctx.getCurrentFunctionReturnType());
        }

        @Test
        void testGetCurrentFunctionReturnType_FromParent() {
            ctx.setCurrentFunctionReturnType(SystemDataType.INT);
            CompileContext child = ctx.createChildContext();

            assertEquals(SystemDataType.INT, child.getCurrentFunctionReturnType());
        }

        @Test
        void testGetCurrentFunctionReturnType_NotSet() {
            assertNull(ctx.getCurrentFunctionReturnType());
        }
    }

    @Nested
    class FunctionCompatibilityTests {

        /**
         * Tests that function lookup finds compatible types via isCastableTo.
         * <pre>
         * fn accept(x: Any) -> Void
         * lookupFunction("accept", [Int])  // finds it since Int.isCastableTo(Any)
         * </pre>
         */
        @Test
        void testLookupFunction_CompatibleTypes() {
            ParameterDef[] params = {ParameterDef.of("x", SystemDataType.ANY)};
            ctx.registerFunction("accept", params, SystemDataType.VOID);

            // INT is castable to ANY
            FunctionSignature sig = ctx.lookupFunction("accept", new SystemDataType[]{SystemDataType.INT});
            assertNotNull(sig);
        }

        /**
         * Tests that function lookup fails for incompatible types.
         * <pre>
         * fn accept(x: Int) -> Void
         * lookupFunction("accept", [String])  // fails, String not castable to Int
         * </pre>
         */
        @Test
        void testLookupFunction_IncompatibleTypes() {
            ParameterDef[] params = {ParameterDef.of("x", SystemDataType.INT)};
            ctx.registerFunction("accept", params, SystemDataType.VOID);

            // STRING is not castable to INT
            FunctionSignature sig = ctx.lookupFunction("accept", new SystemDataType[]{SystemDataType.STRING});
            assertNull(sig);
        }

        /**
         * Tests that null argument type is considered compatible (runtime will verify).
         * <pre>
         * fn accept(x: Int) -> Void
         * lookupFunction("accept", [null])  // compatible, runtime verifies
         * </pre>
         */
        @Test
        void testLookupFunction_NullArgType() {
            ParameterDef[] params = {ParameterDef.of("x", SystemDataType.INT)};
            ctx.registerFunction("accept", params, SystemDataType.VOID);

            // null argType should be compatible
            FunctionSignature sig = ctx.lookupFunction("accept", new SystemDataType[]{null});
            assertNotNull(sig);
        }

        /**
         * Tests that wrong argument count fails lookup.
         * <pre>
         * fn accept(x: Int, y: Int) -> Void
         * lookupFunction("accept", [Int])  // fails, wrong arg count
         * </pre>
         */
        @Test
        void testLookupFunction_WrongArgCount() {
            ParameterDef[] params = {
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            };
            ctx.registerFunction("accept", params, SystemDataType.VOID);

            // Wrong number of args
            FunctionSignature sig = ctx.lookupFunction("accept", new SystemDataType[]{SystemDataType.INT});
            assertNull(sig);
        }

        /**
         * Tests lookup with empty args array vs null params.
         * <pre>
         * fn noArgs() -> Void
         * lookupFunction("noArgs", [])  // finds it
         * </pre>
         */
        @Test
        void testLookupFunction_EmptyArgsVsNullParams() {
            ctx.registerFunction("noArgs", new ParameterDef[0], SystemDataType.VOID);

            FunctionSignature sig = ctx.lookupFunction("noArgs", new SystemDataType[0]);
            assertNotNull(sig);
        }
    }

    @Nested
    class FunctionSignatureTests {

        @Test
        void testFunctionSignature_Equals() {
            ParameterDef[] params = {ParameterDef.of("a", SystemDataType.INT)};
            FunctionSignature sig1 = new FunctionSignature("func", params, SystemDataType.INT);
            FunctionSignature sig2 = new FunctionSignature("func", params, SystemDataType.INT);

            assertEquals(sig1, sig2);
            assertEquals(sig1.hashCode(), sig2.hashCode());
        }

        @Test
        void testFunctionSignature_NotEquals_DifferentName() {
            ParameterDef[] params = {ParameterDef.of("a", SystemDataType.INT)};
            FunctionSignature sig1 = new FunctionSignature("func1", params, SystemDataType.INT);
            FunctionSignature sig2 = new FunctionSignature("func2", params, SystemDataType.INT);

            assertNotEquals(sig1, sig2);
        }

        @Test
        void testFunctionSignature_NotEquals_DifferentParams() {
            ParameterDef[] params1 = {ParameterDef.of("a", SystemDataType.INT)};
            ParameterDef[] params2 = {ParameterDef.of("a", SystemDataType.DOUBLE)};
            FunctionSignature sig1 = new FunctionSignature("func", params1, SystemDataType.INT);
            FunctionSignature sig2 = new FunctionSignature("func", params2, SystemDataType.INT);

            assertNotEquals(sig1, sig2);
        }

        @Test
        void testFunctionSignature_SameObjectEquals() {
            ParameterDef[] params = {ParameterDef.of("a", SystemDataType.INT)};
            FunctionSignature sig = new FunctionSignature("func", params, SystemDataType.INT);

            assertEquals(sig, sig);
        }

        @Test
        void testFunctionSignature_NotEqualsNull() {
            ParameterDef[] params = {ParameterDef.of("a", SystemDataType.INT)};
            FunctionSignature sig = new FunctionSignature("func", params, SystemDataType.INT);

            assertNotEquals(null, sig);
        }

        @Test
        void testFunctionSignature_NotEqualsOtherClass() {
            ParameterDef[] params = {ParameterDef.of("a", SystemDataType.INT)};
            FunctionSignature sig = new FunctionSignature("func", params, SystemDataType.INT);

            assertNotEquals("string", sig);
        }

        @Test
        void testFunctionSignature_NullParams() {
            FunctionSignature sig = new FunctionSignature("func", null, SystemDataType.VOID);

            assertEquals(0, sig.getParameters().length);
            assertEquals("()V", sig.getDescriptor());
        }
    }
}
