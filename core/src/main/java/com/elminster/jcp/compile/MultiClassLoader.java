package com.elminster.jcp.compile;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom ClassLoader that can load multiple generated classes.
 * Used for loading struct classes alongside the main class.
 */
public class MultiClassLoader extends ClassLoader {

    private final Map<String, byte[]> classDefinitions = new HashMap<>();

    public MultiClassLoader() {
        super(MultiClassLoader.class.getClassLoader());
    }

    /**
     * Register a class definition.
     *
     * @param className the fully qualified class name
     * @param bytecode  the class bytecode
     */
    public void defineClass(String className, byte[] bytecode) {
        classDefinitions.put(className, bytecode);
    }

    /**
     * Load and define a class from bytecode.
     *
     * @param className the class name
     * @param bytecode  the bytecode
     * @return the loaded class
     */
    public Class<?> loadClass(String className, byte[] bytecode) {
        classDefinitions.put(className, bytecode);
        try {
            return loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + className, e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = classDefinitions.get(name);
        if (bytecode != null) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
        return super.findClass(name);
    }
}
