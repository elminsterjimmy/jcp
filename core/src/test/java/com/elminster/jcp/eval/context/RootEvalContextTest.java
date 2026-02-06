package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for RootEvalContext.
 */
class RootEvalContextTest {

    private RootEvalContext rootContext;

    @BeforeEach
    void setUp() {
        rootContext = new RootEvalContext();
    }

    @Nested
    class BasicTests {

        /**
         * Tests that all system data types are auto-registered in root context.
         * <pre>
         * getDataType("Integer")  // returns SystemDataType.INT
         * getDataType("String")   // returns SystemDataType.STRING
         * // ... etc for all SystemDataType values
         * </pre>
         */
        @Test
        void testSystemDataTypesRegistered() {
            for (SystemDataType type : SystemDataType.values()) {
                DataType registered = rootContext.getDataType(type.getName());
                assertNotNull(registered, "Type " + type.getName() + " should be registered");
            }
        }

        /**
         * Tests that getting a non-existent variable returns null.
         * <pre>
         * getVariable("nonExistent")  // returns null
         * </pre>
         */
        @Test
        void testGetVariable_NotExists() {
            Data data = rootContext.getVariable("nonExistent");
            assertNull(data);
        }

        /**
         * Tests that getting a non-existent function returns null.
         * <pre>
         * getFunction("nonExistent#")  // returns null
         * </pre>
         */
        @Test
        void testGetFunction_NotExists() {
            Function func = rootContext.getFunction("nonExistent#");
            assertNull(func);
        }
    }

    @Nested
    class VariableScopeTests {

        /**
         * Tests that variables added to root context can be retrieved.
         * <pre>
         * var x: Int = 42
         * getVariable("x")  // returns Data(42)
         * </pre>
         */
        @Test
        void testGetVariable_InRoot() {
            Data var = DataFactory.INSTANCE.createVariable(Identifier.fromName("x"), SystemDataType.INT, 42);
            rootContext.addVariable(var);

            // Should find variable in root
            Data found = rootContext.getVariable("x");
            assertNotNull(found);
            assertEquals(42, found.get());
        }
    }

    @Nested
    class FunctionScopeTests {

        /**
         * Tests that functions added to root context can be retrieved.
         * <pre>
         * fn myFunc() -> Void { }
         * getFunction("myFunc#")  // returns myFunc
         * </pre>
         */
        @Test
        void testGetFunction_InRoot() {
            Function func = new AbstractFunction(
                Identifier.fromName("myFunc"),
                new ParameterDef[]{},
                SystemDataType.VOID
            );
            rootContext.addFunction(func);

            // Should find function in root
            Function found = rootContext.getFunction("myFunc#");
            assertNotNull(found);
        }
    }

    @Nested
    class ToStringTests {

        /**
         * Tests that toString returns a non-null representation.
         * <pre>
         * rootContext.toString()  // returns non-null string
         * </pre>
         */
        @Test
        void testToString() {
            String str = rootContext.toString();
            assertNotNull(str);
        }
    }

}
