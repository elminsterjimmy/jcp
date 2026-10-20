package com.elminster.jcp.module;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.util.CompileModeClassConverter;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.module.base.BaseModuleRegister;
import com.elminster.jcp.util.ClassConverter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JcpModuleRegistry {

    public static final JcpModuleRegistry DEFAULT = new JcpModuleRegistry();

    static {
        for (Class<?> clazz : BaseModuleRegister.classToRegister()) {
            DEFAULT.registerClass(clazz, "base");
        }
    }

    private final List<RegistrationEntry> entries = new ArrayList<>();
    private final List<URLClassLoader> jarClassLoaders = new ArrayList<>();

    public void registerClass(Class<?> clazz, String module) {
        entries.add(new RegistrationEntry(clazz, module));
    }

    public void registerJar(Path jarPath, String module) throws IOException {
        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
        jarClassLoaders.add(loader);

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.contains("module-info")) {
                    continue;
                }
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                try {
                    Class<?> clazz = loader.loadClass(className);
                    if (Modifier.isPublic(clazz.getModifiers())
                            && (!clazz.isMemberClass() || Modifier.isStatic(clazz.getModifiers()))) {
                        registerClass(clazz, module);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                }
            }
        }
    }

    public void initEval(EvalContext ctx) {
        for (RegistrationEntry entry : entries) {
            ClassConverter.registerClass(entry.clazz, ctx, entry.module);
        }
    }

    public void initCompile(CompileContext ctx) {
        for (RegistrationEntry entry : entries) {
            CompileModeClassConverter.registerClass(entry.clazz, ctx, entry.module);
        }
    }

    public List<RegistrationEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<URLClassLoader> getJarClassLoaders() {
        return Collections.unmodifiableList(jarClassLoaders);
    }

    public static final class RegistrationEntry {
        private final Class<?> clazz;
        private final String module;

        RegistrationEntry(Class<?> clazz, String module) {
            this.clazz = clazz;
            this.module = module;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public String getModule() {
            return module;
        }
    }
}
