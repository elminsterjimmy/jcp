package com.elminster.jcp.module;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.util.CompileModeClassConverter;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.module.base.BaseModuleRegister;
import com.elminster.jcp.util.ClassConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Unified registry for Java classes and JARs used by both eval and compile modes.
 *
 * <p>{@link #DEFAULT} is a bootstrap singleton pre-loaded with base module classes.
 * It is intended for single-threaded init-time use only — callers should not mutate
 * it after the initial registration phase, and should not close it (the base module
 * classes do not require classloader cleanup). For custom registrations (e.g. user
 * JARs), create a new {@code JcpModuleRegistry} instance and close it when done.
 */
public class JcpModuleRegistry implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(JcpModuleRegistry.class);

    public static final JcpModuleRegistry DEFAULT = new JcpModuleRegistry(true);

    private final List<RegistrationEntry> entries = new ArrayList<>();
    private final List<URLClassLoader> jarLoaders = new ArrayList<>();

    public JcpModuleRegistry() {
    }

    private JcpModuleRegistry(boolean loadBase) {
        if (loadBase) {
            for (Class<?> clazz : BaseModuleRegister.classToRegister()) {
                entries.add(new RegistrationEntry(clazz, "base", null));
            }
        }
    }

    public void registerClass(Class<?> clazz, String module) {
        entries.add(new RegistrationEntry(clazz, module, null));
    }

    public void registerClass(Class<?> clazz, String module,
                               Consumer<ExternalClassType> customizer) {
        entries.add(new RegistrationEntry(clazz, module, customizer));
    }

    public void registerJar(Path jarPath, String module) throws IOException {
        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(
                new URL[]{jarUrl}, getClass().getClassLoader());
        jarLoaders.add(loader);

        try (JarInputStream jis = new JarInputStream(jarUrl.openStream())) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;
                if (name.contains("$")) continue;

                String className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    Class<?> clazz = loader.loadClass(className);
                    if (Modifier.isPublic(clazz.getModifiers())) {
                        registerClass(clazz, module);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    logger.warn("Skipping class {} from JAR {}: {}", className, jarPath, e.getMessage());
                }
            }
        }
    }

    public void initEval(EvalContext ctx) {
        for (RegistrationEntry entry : entries) {
            ClassConverter.registerClass(entry.clazz, ctx, entry.module);
            if (entry.customizer != null) {
                applyCustomizer(entry, ctx.getDataTypeByFqn(entry.clazz.getName()));
            }
        }
    }

    public void initCompile(CompileContext ctx) {
        for (RegistrationEntry entry : entries) {
            CompileModeClassConverter.registerClass(entry.clazz, ctx, entry.module);
            if (entry.customizer != null) {
                applyCustomizer(entry, ctx.getDataTypeByFqn(entry.clazz.getName()));
            }
        }
    }

    private static void applyCustomizer(RegistrationEntry entry, DataType dt) {
        if (dt instanceof ExternalClassType) {
            entry.customizer.accept((ExternalClassType) dt);
        }
    }

    public List<ClassLoader> getJarLoaders() {
        return Collections.unmodifiableList(jarLoaders);
    }

    /** Package-private for test visibility. */
    int entryCount() {
        return entries.size();
    }

    @Override
    public void close() throws IOException {
        for (URLClassLoader loader : jarLoaders) {
            loader.close();
        }
        jarLoaders.clear();
    }

    private static class RegistrationEntry {
        final Class<?> clazz;
        final String module;
        final Consumer<ExternalClassType> customizer;

        RegistrationEntry(Class<?> clazz, String module, Consumer<ExternalClassType> customizer) {
            this.clazz = clazz;
            this.module = module;
            this.customizer = customizer;
        }
    }
}
