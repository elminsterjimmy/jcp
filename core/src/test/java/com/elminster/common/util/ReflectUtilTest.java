package com.elminster.common.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReflectUtil.
 */
class ReflectUtilTest {

    @Nested
    class GetConstructorTests {

        /**
         * Tests getting a constructor with one parameter.
         * <pre>
         * new String(String)  // constructor found
         * </pre>
         */
        @Test
        void testGetConstructor_Success() {
            Constructor<String> constructor = ReflectUtil.getConstructor(String.class, String.class);
            assertNotNull(constructor);
            assertEquals(String.class, constructor.getDeclaringClass());
        }

        /**
         * Tests getting a no-argument constructor.
         * <pre>
         * new StringBuilder()  // constructor found
         * </pre>
         */
        @Test
        void testGetConstructor_NoArgs() {
            Constructor<StringBuilder> constructor = ReflectUtil.getConstructor(StringBuilder.class);
            assertNotNull(constructor);
        }

        /**
         * Tests that getting a non-existent constructor throws RuntimeException.
         * <pre>
         * new String(File, Date)  // throws RuntimeException (no such constructor)
         * </pre>
         */
        @Test
        void testGetConstructor_NotFound() {
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.getConstructor(String.class, java.io.File.class, java.util.Date.class)
            );
            assertTrue(exception.getMessage().contains("Constructor not found"));
            assertTrue(exception.getCause() instanceof NoSuchMethodException);
        }
    }

    @Nested
    class InvokeByNameTests {

        /**
         * Tests invoking an instance method by name.
         * <pre>
         * "Hello, World!".length()  // returns 13
         * </pre>
         */
        @Test
        void testInvoke_InstanceMethod() {
            String target = "Hello, World!";
            Object result = ReflectUtil.invoke(target, "length");
            assertEquals(13, result);
        }

        /**
         * Tests invoking a method with multiple arguments.
         * <pre>
         * "Hello, World!".substring(0, 5)  // returns "Hello"
         * </pre>
         */
        @Test
        void testInvoke_MethodWithArgs() {
            String target = "Hello, World!";
            Object result = ReflectUtil.invoke(target, "substring", 0, 5);
            assertEquals("Hello", result);
        }

        /**
         * Tests invoking a method with null argument.
         * <pre>
         * new StringBuilder("Hello").append(null)  // returns "Hellonull"
         * </pre>
         */
        @Test
        void testInvoke_MethodWithNullArg() {
            StringBuilder target = new StringBuilder("Hello");
            // append(Object) can take null
            Object result = ReflectUtil.invoke(target, "append", (Object) null);
            assertNotNull(result);
        }

        /**
         * Tests that invoking a non-existent method throws RuntimeException.
         * <pre>
         * "Hello".nonExistentMethod()  // throws RuntimeException
         * </pre>
         */
        @Test
        void testInvoke_MethodNotFound() {
            String target = "Hello";
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.invoke(target, "nonExistentMethod")
            );
            assertTrue(exception.getMessage().contains("Failed to invoke method"));
        }

        /**
         * Tests that runtime exceptions from invoked methods are propagated.
         * <pre>
         * new ArrayList<>().get(0)  // throws IndexOutOfBoundsException
         * </pre>
         */
        @Test
        void testInvoke_MethodThrowsRuntimeException() {
            java.util.List<String> list = new java.util.ArrayList<>();
            assertThrows(IndexOutOfBoundsException.class, () ->
                ReflectUtil.invoke(list, "get", 0)
            );
        }

        /**
         * Tests that errors from invoked methods are propagated.
         * <pre>
         * helper.throwError()  // throws AssertionError
         * </pre>
         */
        @Test
        void testInvoke_MethodThrowsError() {
            TestHelper helper = new TestHelper();
            assertThrows(AssertionError.class, () ->
                ReflectUtil.invoke(helper, "throwError")
            );
        }

        /**
         * Tests that checked exceptions are wrapped in RuntimeException.
         * <pre>
         * helper.throwCheckedException()  // throws RuntimeException wrapping IOException
         * </pre>
         */
        @Test
        void testInvoke_MethodThrowsCheckedException() {
            TestHelper helper = new TestHelper();
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.invoke(helper, "throwCheckedException")
            );
            assertTrue(exception.getCause() instanceof java.io.IOException);
        }

        /**
         * Tests that invoking on null target throws RuntimeException.
         * <pre>
         * null.anyMethod()  // throws RuntimeException
         * </pre>
         */
        @Test
        void testInvoke_NullTarget() {
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.invoke(null, "anyMethod")
            );
            assertTrue(exception.getMessage().contains("Failed to invoke method"));
        }
    }

    @Nested
    class InvokeByMethodTests {

        /**
         * Tests invoking via Method object.
         * <pre>
         * "Hello, World!".length()  // returns 13 (via Method object)
         * </pre>
         */
        @Test
        void testInvoke_ByMethod_Success() throws Exception {
            String target = "Hello, World!";
            Method method = String.class.getMethod("length");
            Object result = ReflectUtil.invoke(target, method);
            assertEquals(13, result);
        }

        /**
         * Tests invoking via Method object with arguments.
         * <pre>
         * "Hello, World!".charAt(0)  // returns 'H' (via Method object)
         * </pre>
         */
        @Test
        void testInvoke_ByMethod_WithArgs() throws Exception {
            String target = "Hello, World!";
            Method method = String.class.getMethod("charAt", int.class);
            Object result = ReflectUtil.invoke(target, method, 0);
            assertEquals('H', result);
        }

        /**
         * Tests that runtime exceptions are propagated via Method invocation.
         * <pre>
         * new ArrayList<>().get(0)  // throws IndexOutOfBoundsException (via Method)
         * </pre>
         */
        @Test
        void testInvoke_ByMethod_ThrowsRuntimeException() throws Exception {
            java.util.List<String> list = new java.util.ArrayList<>();
            Method method = java.util.ArrayList.class.getMethod("get", int.class);
            assertThrows(IndexOutOfBoundsException.class, () ->
                ReflectUtil.invoke(list, method, 0)
            );
        }

        /**
         * Tests that errors are propagated via Method invocation.
         * <pre>
         * helper.throwError()  // throws AssertionError (via Method)
         * </pre>
         */
        @Test
        void testInvoke_ByMethod_ThrowsError() throws Exception {
            TestHelper helper = new TestHelper();
            Method method = TestHelper.class.getMethod("throwError");
            assertThrows(AssertionError.class, () ->
                ReflectUtil.invoke(helper, method)
            );
        }

        /**
         * Tests that checked exceptions are wrapped via Method invocation.
         * <pre>
         * helper.throwCheckedException()  // throws RuntimeException wrapping IOException
         * </pre>
         */
        @Test
        void testInvoke_ByMethod_ThrowsCheckedException() throws Exception {
            TestHelper helper = new TestHelper();
            Method method = TestHelper.class.getMethod("throwCheckedException");
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.invoke(helper, method)
            );
            assertTrue(exception.getCause() instanceof java.io.IOException);
        }
    }

    @Nested
    class PrimitiveTypeCompatibilityTests {

        /**
         * Tests invoking method with int primitive argument.
         * <pre>
         * helper.acceptInt(42)  // returns 42
         * </pre>
         */
        @Test
        void testInvoke_IntPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptInt", 42);
            assertEquals(42, result);
        }

        /**
         * Tests invoking method with long primitive argument.
         * <pre>
         * helper.acceptLong(42L)  // returns 42L
         * </pre>
         */
        @Test
        void testInvoke_LongPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptLong", 42L);
            assertEquals(42L, result);
        }

        /**
         * Tests invoking method with double primitive argument.
         * <pre>
         * helper.acceptDouble(3.14)  // returns 3.14
         * </pre>
         */
        @Test
        void testInvoke_DoublePrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptDouble", 3.14);
            assertEquals(3.14, result);
        }

        /**
         * Tests invoking method with float primitive argument.
         * <pre>
         * helper.acceptFloat(3.14f)  // returns 3.14f
         * </pre>
         */
        @Test
        void testInvoke_FloatPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptFloat", 3.14f);
            assertEquals(3.14f, result);
        }

        /**
         * Tests invoking method with boolean primitive argument.
         * <pre>
         * helper.acceptBoolean(true)  // returns true
         * </pre>
         */
        @Test
        void testInvoke_BooleanPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptBoolean", true);
            assertEquals(true, result);
        }

        /**
         * Tests invoking method with char primitive argument.
         * <pre>
         * helper.acceptChar('A')  // returns 'A'
         * </pre>
         */
        @Test
        void testInvoke_CharPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptChar", 'A');
            assertEquals('A', result);
        }

        /**
         * Tests invoking method with byte primitive argument.
         * <pre>
         * helper.acceptByte((byte) 42)  // returns (byte) 42
         * </pre>
         */
        @Test
        void testInvoke_BytePrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptByte", (byte) 42);
            assertEquals((byte) 42, result);
        }

        /**
         * Tests invoking method with short primitive argument.
         * <pre>
         * helper.acceptShort((short) 42)  // returns (short) 42
         * </pre>
         */
        @Test
        void testInvoke_ShortPrimitive() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptShort", (short) 42);
            assertEquals((short) 42, result);
        }
    }

    @Nested
    class MethodResolutionTests {

        /**
         * Tests exact method signature matching.
         * <pre>
         * "hello".toUpperCase()  // returns "HELLO"
         * </pre>
         */
        @Test
        void testInvoke_ExactMatch() {
            String target = "hello";
            Object result = ReflectUtil.invoke(target, "toUpperCase");
            assertEquals("HELLO", result);
        }

        /**
         * Tests auto-unboxing from Integer to int.
         * <pre>
         * helper.acceptInt(Integer.valueOf(100))  // returns 100 (auto-unboxing)
         * </pre>
         */
        @Test
        void testInvoke_CompatibleTypes() {
            TestHelper helper = new TestHelper();
            Object result = ReflectUtil.invoke(helper, "acceptInt", Integer.valueOf(100));
            assertEquals(100, result);
        }

        /**
         * Tests that incompatible types throw RuntimeException.
         * <pre>
         * helper.acceptInt("not an int")  // throws RuntimeException (String cannot match int)
         * </pre>
         */
        @Test
        void testInvoke_IncompatibleTypes() {
            TestHelper helper = new TestHelper();
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ReflectUtil.invoke(helper, "acceptInt", "not an int")
            );
            assertTrue(exception.getMessage().contains("Failed to invoke method"));
        }
    }

    /**
     * Helper class for testing reflection with various method signatures.
     */
    public static class TestHelper {

        public void throwError() {
            throw new AssertionError("Test error");
        }

        public void throwCheckedException() throws java.io.IOException {
            throw new java.io.IOException("Test checked exception");
        }

        public int acceptInt(int value) {
            return value;
        }

        public long acceptLong(long value) {
            return value;
        }

        public double acceptDouble(double value) {
            return value;
        }

        public float acceptFloat(float value) {
            return value;
        }

        public boolean acceptBoolean(boolean value) {
            return value;
        }

        public char acceptChar(char value) {
            return value;
        }

        public byte acceptByte(byte value) {
            return value;
        }

        public short acceptShort(short value) {
            return value;
        }
    }
}
