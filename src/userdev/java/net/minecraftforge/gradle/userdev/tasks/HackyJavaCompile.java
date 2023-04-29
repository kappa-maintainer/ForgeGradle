/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gradle.userdev.tasks;

import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaCompiler;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.language.base.internal.compile.Compiler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/*
 *  A terrible hack to use JavaCompile while bypassing
 *  Gradle's normal task infrastructure.
 *  This is internal API Modders DO NOT reference this.
 *  It can and will be removed if we get a better way to do this.
 */
public class HackyJavaCompile extends JavaCompile {

    public void doHackyCompile() {

        // What follows is a horrible hack to allow us to call JavaCompile
        // from our dependency resolver.
        // As described in https://github.com/MinecraftForge/ForgeGradle/issues/550,
        // invoking Gradle tasks in the normal way can lead to deadlocks
        // when done from a dependency resolver.

        setCompiler();

        this.getOutputs().setPreviousOutputFiles(this.getProject().files());
        final DefaultJavaCompileSpec spec = reflectCreateSpec();
        spec.setSourceFiles(getSource());
        spec.getCompileOptions().setCompilerArgs(Arrays.asList("--add-exports", "java.base/jdk.internal.loader=ALL-UNNAMED", "--add-exports", "java.base/java.net=ALL-UNNAMED", "--add-exports", "java.base/java.nio=ALL-UNNAMED", "--add-exports", "java.base/java.io=ALL-UNNAMED", "--add-exports", "java.base/java.lang=ALL-UNNAMED", "--add-exports", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-exports", "java.base/java.text=ALL-UNNAMED", "--add-exports", "java.base/java.util=ALL-UNNAMED", "--add-exports", "java.base/jdk.internal.reflect=ALL-UNNAMED", "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED", "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming", "--add-exports", "java.desktop/sun.awt.image=ALL-UNNAMED", "--add-exports", "java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED", "--add-modules", "jdk.dynalink", "--add-exports", "jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED", "--add-modules", "java.sql.rowset", "--add-exports", "java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED"));
        Compiler<JavaCompileSpec> compiler = createCompiler(spec);
        final WorkResult execute = compiler.execute(spec);
        setDidWork(execute.getDidWork());
    }

    private void setCompiler() {
        JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);
        Provider<JavaCompiler> compiler = service.compilerFor(s -> s.getLanguageVersion().set(JavaLanguageVersion.of(this.getSourceCompatibility())));
        this.getJavaCompiler().set(compiler);
    }

    private DefaultJavaCompileSpec reflectCreateSpec() {
        try {
            Method createSpec = JavaCompile.class.getDeclaredMethod("createSpec");
            createSpec.setAccessible(true);
            return (DefaultJavaCompileSpec) createSpec.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find JavaCompile#createSpec method; might be on incompatible newer version of Gradle", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Exception while invoking JavaCompile#createSpec", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Compiler<JavaCompileSpec> createCompiler(JavaCompileSpec spec) {
        try {
            Method createCompiler = JavaCompile.class.getDeclaredMethod("createCompiler");
            createCompiler.setAccessible(true);
            return (Compiler<JavaCompileSpec>) createCompiler.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find JavaCompile#createCompiler method; might be on incompatible newer version of Gradle", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Exception while invoking JavaCompile#createCompiler", e);
        }
    }

}
