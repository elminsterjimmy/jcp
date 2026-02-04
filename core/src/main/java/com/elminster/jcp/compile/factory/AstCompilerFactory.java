package com.elminster.jcp.compile.factory;

import com.elminster.common.util.ReflectUtil;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.Compilable;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating AST compilers.
 * Similar to {@link com.elminster.jcp.eval.factory.AstEvaluatorFactory}.
 */
public abstract class AstCompilerFactory {

    private static final String AST_COMPILE_BASE_PACKAGE = "com.elminster.jcp.compile";
    private static final Map<String, ? extends Class<?>> SYSTEM_COMPILERS;

    static {
        try {
            SYSTEM_COMPILERS = ClassPath.from(ClassLoader.getSystemClassLoader())
                    .getAllClasses()
                    .stream()
                    .filter(clazz -> clazz.getPackageName().startsWith(AST_COMPILE_BASE_PACKAGE)
                            && clazz.getSimpleName().endsWith("Compiler"))
                    .map(clazz -> clazz.load())
                    .collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException("failed to load the system compilers", e);
        }
    }

    /**
     * Get a compiler for the given AST node.
     *
     * @param astNode the AST node
     * @return the compiler for the node
     */
    public static Compilable getCompiler(Node astNode) {
        if (astNode instanceof Compilable) {
            return (Compilable) astNode;
        }
        String name = astNode.getName();
        try {
            Class<?> clazz = SYSTEM_COMPILERS.get(getCompilerClassName(name));
            @SuppressWarnings("unchecked")
            Constructor<Compilable> constructor = (Constructor<Compilable>) ReflectUtil.getConstructor(clazz, Node.class);
            return constructor.newInstance(astNode);
        } catch (NullPointerException | IllegalAccessException
                | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(String.format("cannot get compiler for node: %s", astNode), e);
        }
    }

    private static String getCompilerClassName(String name) {
        return normalize(name) + "Compiler";
    }

    private static String normalize(String name) {
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        boolean upper = true;
        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if ('_' == ch) {
                upper = true;
            } else {
                if (upper) {
                    sb.append(Character.toUpperCase(ch));
                    upper = false;
                } else {
                    sb.append(Character.toLowerCase(ch));
                }
            }
        }
        return sb.toString();
    }
}
