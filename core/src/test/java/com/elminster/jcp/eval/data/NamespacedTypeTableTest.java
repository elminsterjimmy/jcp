package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.excpetion.AmbiguousTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamespacedTypeTableTest {

    private NamespacedTypeTable table;

    @BeforeEach
    void setUp() {
        table = new NamespacedTypeTable();
    }

    @Nested
    class SingleRegistration {

        @Test
        void testRegisterAndResolveBySimpleName() {
            ExternalClassType type = new ExternalClassType("Date", java.util.Date.class);
            table.register(type);

            assertSame(type, table.getBySimpleName("Date"));
        }

        @Test
        void testRegisterAndResolveByFqn() {
            ExternalClassType type = new ExternalClassType("Date", java.util.Date.class);
            table.register(type);

            assertSame(type, table.getByFqn("java.util.Date"));
        }

        @Test
        void testUnknownSimpleNameReturnsNull() {
            assertNull(table.getBySimpleName("Unknown"));
        }

        @Test
        void testUnknownFqnReturnsNull() {
            assertNull(table.getByFqn("com.example.Unknown"));
        }
    }

    @Nested
    class IdempotentReRegistration {

        @Test
        void testSameFqnReRegistrationIsNoOp() {
            ExternalClassType type = new ExternalClassType("Date", java.util.Date.class);
            table.register(type);
            // should not throw
            assertDoesNotThrow(() -> table.register(type));
            assertSame(type, table.getBySimpleName("Date"));
        }

        @Test
        void testSameFqnDifferentInstanceIsNoOp() {
            // Two separate ExternalClassType instances for the exact same Java class
            ExternalClassType first = new ExternalClassType("Date", java.util.Date.class);
            ExternalClassType second = new ExternalClassType("Date", java.util.Date.class);
            table.register(first);
            assertDoesNotThrow(() -> table.register(second));
            // First registration wins; no throw
            assertSame(first, table.getBySimpleName("Date"));
        }
    }

    @Nested
    class Coexistence {

        @Test
        void testTwoDifferentFqnsSameSimpleNameCoexist() {
            ExternalClassType utilDate = new ExternalClassType("Date", java.util.Date.class);
            ExternalClassType sqlDate = new ExternalClassType("Date", java.sql.Date.class);

            table.register(utilDate);
            assertDoesNotThrow(() -> table.register(sqlDate));

            // Both reachable by FQN
            assertSame(utilDate, table.getByFqn("java.util.Date"));
            assertSame(sqlDate, table.getByFqn("java.sql.Date"));
        }

        @Test
        void testAmbiguousSimpleNameThrows() {
            table.register(new ExternalClassType("Date", java.util.Date.class));
            table.register(new ExternalClassType("Date", java.sql.Date.class));

            AmbiguousTypeException ex = assertThrows(
                    AmbiguousTypeException.class,
                    () -> table.getBySimpleName("Date"));
            assertEquals("Date", ex.getSimpleName());
            assertTrue(ex.getCandidates().contains("java.util.Date"), "message should name java.util.Date");
            assertTrue(ex.getCandidates().contains("java.sql.Date"), "message should name java.sql.Date");
        }

        @Test
        void testUnambiguousSimpleNameAfterTwoDistinctRegistrations() {
            ExternalClassType utilDate = new ExternalClassType("Date", java.util.Date.class);
            ExternalClassType utilList = new ExternalClassType("ArrayList", java.util.ArrayList.class);

            table.register(utilDate);
            table.register(utilList);

            // Neither simple name is ambiguous
            assertSame(utilDate, table.getBySimpleName("Date"));
            assertSame(utilList, table.getBySimpleName("ArrayList"));
        }
    }

    @Nested
    class ShadowingByNonExternal {

        @Test
        void testStructTypeShadowsExternalClassWithSameSimpleName() {
            // Simulates: user declares a JCP type named "Math"; module also registers
            // com.elminster.jcp.module.base.math.Math as an ExternalClassType.
            ExternalClassType extMath = new ExternalClassType("Math",
                    com.elminster.jcp.module.base.math.Math.class);
            // A struct/system DataType whose getName() == "Math" (FQN == "Math")
            DataType structMath = new DataTypeImpl("Math");

            table.register(extMath);
            table.register(structMath);

            // Struct wins (non-ExternalClassType preferred)
            assertSame(structMath, table.getBySimpleName("Math"));
        }

        @Test
        void testTwoExternalClassesWithSameSimpleNameAreAmbiguous() {
            table.register(new ExternalClassType("Date", java.util.Date.class));
            table.register(new ExternalClassType("Date", java.sql.Date.class));

            assertThrows(AmbiguousTypeException.class, () -> table.getBySimpleName("Date"));
        }
    }

    @Nested
    class SystemTypeRegistration {

        @Test
        void testSystemTypeUsesSimpleNameAsFqn() {
            // System types (no Java class) — FQN == getName() == "Integer"
            DataType intType = SystemDataType.INT;
            table.register(intType);

            String name = intType.getName(); // "Integer"
            assertSame(intType, table.getBySimpleName(name));
            assertSame(intType, table.getByFqn(name));
        }

        @Test
        void testSystemTypeAndExternalTypeWithSameSimpleNameBothRegistered() {
            // Struct types also use simple name as FQN; they should not collide with
            // a same-simple-name ExternalClassType (different FQN for the external one).
            ExternalClassType extDate = new ExternalClassType("Date", java.util.Date.class);
            table.register(extDate);

            // Should be resolvable by FQN
            assertSame(extDate, table.getByFqn("java.util.Date"));
        }
    }

    @Nested
    class ContainsSimpleName {

        @Test
        void testContainsAfterRegistration() {
            table.register(new ExternalClassType("Date", java.util.Date.class));
            assertTrue(table.containsSimpleName("Date"));
        }

        @Test
        void testNotContainsBeforeRegistration() {
            assertFalse(table.containsSimpleName("Date"));
        }
    }

    @Nested
    class GetByModuleAndSimpleName {

        @Test
        void testReturnsCorrectTypeWhenTwoModulesShareSimpleName() {
            ExternalClassType baseStrings = new ExternalClassType("Strings",
                com.elminster.jcp.module.base.strings.Strings.class);
            baseStrings.setModule("base");

            ExternalClassType guavaStrings = new ExternalClassType("Strings",
                com.google.common.base.Strings.class);
            guavaStrings.setModule("guava");

            table.register(baseStrings);
            table.register(guavaStrings);

            assertSame(guavaStrings, table.getByModuleAndSimpleName("guava", "Strings"));
            assertSame(baseStrings, table.getByModuleAndSimpleName("base", "Strings"));
        }

        @Test
        void testReturnsSingleTypeWhenNoCollision() {
            ExternalClassType dateType = new ExternalClassType("Date", java.util.Date.class);
            dateType.setModule("javautil");
            table.register(dateType);

            assertSame(dateType, table.getByModuleAndSimpleName("javautil", "Date"));
        }

        @Test
        void testReturnsNullForNonExistentModule() {
            ExternalClassType dateType = new ExternalClassType("Date", java.util.Date.class);
            dateType.setModule("javautil");
            table.register(dateType);

            assertNull(table.getByModuleAndSimpleName("unknownmod", "Date"));
        }

        @Test
        void testReturnsNullForNonExistentSimpleName() {
            ExternalClassType dateType = new ExternalClassType("Date", java.util.Date.class);
            dateType.setModule("javautil");
            table.register(dateType);

            assertNull(table.getByModuleAndSimpleName("javautil", "NonExistent"));
        }

        @Test
        void testReturnsNullForEmptyTable() {
            assertNull(table.getByModuleAndSimpleName("anymod", "AnyType"));
        }

        @Test
        void testFiltersOutNonExternalClassTypes() {
            // Register a non-ExternalClassType (struct/system) with same simple name
            DataType structType = new DataTypeImpl("Math");
            table.register(structType);

            // module:: lookup should NOT match non-ExternalClassType
            assertNull(table.getByModuleAndSimpleName("base", "Math"));
        }
    }
}
