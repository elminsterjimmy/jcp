package com.elminster.jcp.module.base.arrays;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SortKey#toComparator()} — no JCP runtime required.
 */
class SortKeyTest {

    private StructType personType;
    private StructType addressType;

    @BeforeEach
    void setUp() {
        personType = new StructType("Person", Arrays.asList(
            new StructFieldDef("name", SystemDataType.STRING),
            new StructFieldDef("age", SystemDataType.INT)
        ));
        addressType = new StructType("Address", Arrays.asList(
            new StructFieldDef("city", SystemDataType.STRING)
        ));
    }

    private StructData person(String name, int age) {
        StructData p = new StructData(Identifier.fromName("p"), personType);
        p.setField("name", new StringData(name, false));
        p.setField("age", new IntegerData(age, false));
        return p;
    }

    // --- single field, int leaf ---

    @Test
    void testSortByInt_asc() {
        Object[] arr = { person("B", 30), person("A", 20), person("C", 10) };
        Arrays.sort(arr, SortKey.by("age").toComparator());
        assertEquals(10, ((StructData) arr[0]).getField("age").get());
        assertEquals(20, ((StructData) arr[1]).getField("age").get());
        assertEquals(30, ((StructData) arr[2]).getField("age").get());
    }

    @Test
    void testSortByInt_desc() {
        Object[] arr = { person("B", 10), person("A", 30), person("C", 20) };
        Arrays.sort(arr, SortKey.by("age").desc().toComparator());
        assertEquals(30, ((StructData) arr[0]).getField("age").get());
        assertEquals(20, ((StructData) arr[1]).getField("age").get());
        assertEquals(10, ((StructData) arr[2]).getField("age").get());
    }

    // --- single field, String leaf ---

    @Test
    void testSortByString_asc() {
        Object[] arr = { person("Charlie", 1), person("Alice", 2), person("Bob", 3) };
        Arrays.sort(arr, SortKey.by("name").toComparator());
        assertEquals("Alice", ((StructData) arr[0]).getField("name").get());
        assertEquals("Bob", ((StructData) arr[1]).getField("name").get());
        assertEquals("Charlie", ((StructData) arr[2]).getField("name").get());
    }

    @Test
    void testSortByString_desc() {
        Object[] arr = { person("Alice", 1), person("Charlie", 2), person("Bob", 3) };
        Arrays.sort(arr, SortKey.by("name").desc().toComparator());
        assertEquals("Charlie", ((StructData) arr[0]).getField("name").get());
        assertEquals("Bob", ((StructData) arr[1]).getField("name").get());
        assertEquals("Alice", ((StructData) arr[2]).getField("name").get());
    }

    // --- asc() is identity ---

    @Test
    void testAscIsDefault() {
        SortKey k = SortKey.by("age");
        SortKey k2 = SortKey.by("age").asc();
        Object[] a1 = { person("B", 30), person("A", 10) };
        Object[] a2 = { person("B", 30), person("A", 10) };
        Arrays.sort(a1, k.toComparator());
        Arrays.sort(a2, k2.toComparator());
        assertEquals(((StructData) a1[0]).getField("age").get(),
                     ((StructData) a2[0]).getField("age").get());
    }

    // --- missing field → null-last ---

    @Test
    void testMissingField_sortsLast_asc() {
        StructType t = new StructType("T", Arrays.asList(new StructFieldDef("x", SystemDataType.INT)));
        StructData withField = new StructData(Identifier.fromName("a"), t);
        withField.setField("x", new IntegerData(5, false));
        StructData missingField = new StructData(Identifier.fromName("b"), t);

        Object[] arr = { missingField, withField };
        Arrays.sort(arr, SortKey.by("x").toComparator());
        assertEquals(5, ((StructData) arr[0]).getField("x").get());
        assertNull(((StructData) arr[1]).getField("x"));
    }

    @Test
    void testMissingField_sortsLast_desc() {
        StructType t = new StructType("T", Arrays.asList(new StructFieldDef("x", SystemDataType.INT)));
        StructData withField = new StructData(Identifier.fromName("a"), t);
        withField.setField("x", new IntegerData(5, false));
        StructData missingField = new StructData(Identifier.fromName("b"), t);

        Object[] arr = { missingField, withField };
        Arrays.sort(arr, SortKey.by("x").desc().toComparator());
        assertEquals(5, ((StructData) arr[0]).getField("x").get());
        assertNull(((StructData) arr[1]).getField("x"));
    }

    @Test
    void testBothMissingField_staysEqual() {
        StructType t = new StructType("T", Arrays.asList(new StructFieldDef("x", SystemDataType.INT)));
        StructData a = new StructData(Identifier.fromName("a"), t);
        StructData b = new StructData(Identifier.fromName("b"), t);
        Comparator<Object> cmp = SortKey.by("x").toComparator();
        assertEquals(0, cmp.compare(a, b));
    }

    // --- non-StructData element → null-last ---

    @Test
    void testNonStructElement_sortsLast() {
        StructData s = new StructData(Identifier.fromName("a"), personType);
        s.setField("age", new IntegerData(1, false));
        Object nonStruct = "plain string";

        Object[] arr = { nonStruct, s };
        Arrays.sort(arr, SortKey.by("age").toComparator());
        assertSame(s, arr[0]);
        assertSame(nonStruct, arr[1]);
    }

    // --- nested path ---

    @Test
    void testNestedPath() {
        StructType personWithAddress = new StructType("PersonAddr", Arrays.asList(
            new StructFieldDef("name", SystemDataType.STRING),
            new StructFieldDef("address", SystemDataType.ANY)
        ));

        StructData addr1 = new StructData(Identifier.fromName("addr1"), addressType);
        addr1.setField("city", new StringData("Berlin", false));
        StructData addr2 = new StructData(Identifier.fromName("addr2"), addressType);
        addr2.setField("city", new StringData("Amsterdam", false));

        // Wrap nested StructData in AnyData so .get() returns the StructData
        StructData p1 = new StructData(Identifier.fromName("p1"), personWithAddress);
        p1.setField("name", new StringData("X", false));
        p1.setField("address", new com.elminster.jcp.eval.data.AnyData<>(addr1));

        StructData p2 = new StructData(Identifier.fromName("p2"), personWithAddress);
        p2.setField("name", new StringData("Y", false));
        p2.setField("address", new com.elminster.jcp.eval.data.AnyData<>(addr2));

        Object[] arr = { p1, p2 };
        Arrays.sort(arr, SortKey.by("address.city").toComparator());
        // p2 (Amsterdam) should sort before p1 (Berlin)
        StructData first = (StructData) arr[0];
        StructData firstAddr = (StructData) first.getField("address").get();
        assertEquals("Amsterdam", firstAddr.getField("city").get());
    }

    // --- illegal field path → IllegalArgumentException ---

    @Test
    void testNestedPath_nonStructMidNode_throws() {
        StructType t = new StructType("T", Arrays.asList(new StructFieldDef("x", SystemDataType.STRING)));
        StructData s = new StructData(Identifier.fromName("s"), t);
        // "x" is a String, not a StructData — navigating "x.sub" is an illegal field path
        s.setField("x", new StringData("hello", false));

        Comparator<Object> cmp = SortKey.by("x.sub").toComparator();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cmp.compare(s, s));
        assertTrue(ex.getMessage().contains("x.sub"));
    }

    @Test
    void testNestedPath_missingMidSegment_throws() {
        StructType t = new StructType("T", Arrays.asList(new StructFieldDef("x", SystemDataType.STRING)));
        StructData s = new StructData(Identifier.fromName("s"), t);
        // "missing" segment is not present in the struct — illegal path
        Comparator<Object> cmp = SortKey.by("missing.leaf").toComparator();
        assertThrows(IllegalArgumentException.class, () -> cmp.compare(s, s));
    }

    // --- by() rejects null/empty fieldPath ---

    @Test
    void testBy_nullFieldPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> SortKey.by(null));
    }

    @Test
    void testBy_emptyFieldPath_throws() {
        assertThrows(IllegalArgumentException.class, () -> SortKey.by(""));
    }

    // --- Direction enum: asc()/desc() are independent and immutable ---

    @Test
    void testAscDesc_returnNewInstances() {
        SortKey base = SortKey.by("age");
        SortKey desc = base.desc();
        SortKey asc = desc.asc();
        // base unchanged: still ascending
        Object[] arr1 = { person("B", 30), person("A", 10) };
        Arrays.sort(arr1, base.toComparator());
        assertEquals(10, ((StructData) arr1[0]).getField("age").get());
        // desc sorts descending
        Object[] arr2 = { person("B", 10), person("A", 30) };
        Arrays.sort(arr2, desc.toComparator());
        assertEquals(30, ((StructData) arr2[0]).getField("age").get());
        // asc sorts ascending
        Object[] arr3 = { person("B", 30), person("A", 10) };
        Arrays.sort(arr3, asc.toComparator());
        assertEquals(10, ((StructData) arr3[0]).getField("age").get());
    }
}
