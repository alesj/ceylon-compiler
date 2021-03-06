/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.compiler.java.test.cmr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.redhat.ceylon.compiler.java.test.CompilerTest;
import com.redhat.ceylon.compiler.java.tools.CeyloncTaskImpl;
import com.redhat.ceylon.compiler.java.util.Util;

public class CMRTest extends CompilerTest {
    
    //
    // Modules
    
    @Test
    public void testMdlModuleDefault() throws IOException{
        compile("module/def/CeylonClass.ceylon");
        
        File carFile = getModuleArchive("default", null);
        assertTrue(carFile.exists());

        JarFile car = new JarFile(carFile);

        ZipEntry moduleClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/def/CeylonClass.class");
        assertNotNull(moduleClass);
        car.close();
    }

    @Test
    public void testMdlModuleDefaultJavaFile() throws IOException{
        compile("module/def/JavaClass.java");
        
        File carFile = getModuleArchive("default", null);
        assertTrue(carFile.exists());

        JarFile car = new JarFile(carFile);

        ZipEntry moduleClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/def/JavaClass.class");
        assertNotNull(moduleClass);
        car.close();
    }

    @Test
    public void testMdlModuleOnlyInOutputRepo(){
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertFalse(carFile.exists());

        File carFileInHomeRepo = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6",
                Util.getHomeRepository());
        if(carFileInHomeRepo.exists())
            carFileInHomeRepo.delete();
        
        compile("module/single/module.ceylon");

        // make sure it was created in the output repo
        assertTrue(carFile.exists());
        // make sure it wasn't created in the home repo
        assertFalse(carFileInHomeRepo.exists());
    }

    @Test
    public void testMdlWithCeylonImport() throws IOException{
        compile("module/ceylon_import/module.ceylon", "module/ceylon_import/ImportCeylonLanguage.ceylon");
    }
    
    @Test
    public void testMdlWithCommonPrefix() throws IOException{
        compile("module/depend/prefix/module.ceylon");
        // This is heisenbug https://github.com/ceylon/ceylon-compiler/issues/460 and for some
        // reason it only happens _sometimes_, hence the repeats
        compile("module/depend/prefix_suffix/module.ceylon");
        compile("module/depend/prefix_suffix/module.ceylon");
        compile("module/depend/prefix_suffix/module.ceylon");
        compile("module/depend/prefix_suffix/module.ceylon");
        compile("module/depend/prefix_suffix/module.ceylon");
    }
    
    @Test
    public void testMdlModuleFromCompiledModule() throws IOException{
        compile("module/single/module.ceylon");
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertTrue(carFile.exists());

        JarFile car = new JarFile(carFile);
        // just to be sure
        ZipEntry bogusEntry = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/BOGUS");
        assertNull(bogusEntry);

        ZipEntry moduleClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/module.class");
        assertNotNull(moduleClass);
        car.close();

        compile("module/single/subpackage/Subpackage.ceylon");

        // MUST reopen it
        car = new JarFile(carFile);

        ZipEntry subpackageClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/subpackage/Subpackage.class");
        assertNotNull(subpackageClass);

        car.close();
    }

    @Ignore("M3")
    @Test
    public void testMdlCarWithInvalidSHA1() throws IOException{
        compile("module/single/module.ceylon");
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertTrue(carFile.exists());

        JarFile car = new JarFile(carFile);
        // just to be sure
        ZipEntry moduleClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/module.class");
        assertNotNull(moduleClass);
        car.close();

        // now let's break the SHA1
        File shaFile = getArchiveName("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6", destDir, "car.sha1");
        Writer w = new FileWriter(shaFile);
        w.write("fubar");
        w.flush();
        w.close();
        
        // now try to compile the subpackage with a broken SHA1
        //compile("module/single/subpackage/Subpackage.ceylon");
        assertErrors("module/single/subpackage/Subpackage", 
                new CompilerError(-1, "Module car "+destDir+"/com/redhat/ceylon/compiler/java/test/cmr/module/single/6.6.6/com.redhat.ceylon.compiler.java.test.cmr.module.single-6.6.6.car has an invalid SHA1 signature: you need to remove it and rebuild the archive, since it may be corrupted."));
    }

    @Test
    public void testMdlCompilerGeneratesModuleForValidUnits() throws IOException{
        CeyloncTaskImpl compilerTask = getCompilerTask("module/single/module.ceylon", "module/single/Correct.ceylon", "module/single/Invalid.ceylon");
        Boolean success = compilerTask.call();
        assertFalse(success);
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertTrue(carFile.exists());

        JarFile car = new JarFile(carFile);

        ZipEntry moduleClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/module.class");
        assertNotNull(moduleClass);

        ZipEntry correctClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/Correct.class");
        assertNotNull(correctClass);

        ZipEntry invalidClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/Invalid.class");
        assertNull(invalidClass);
        
        car.close();
    }

    @Ignore("M3")
    @Test
    public void testMdlInterdepModule(){
        // first compile it all from source
        compile("module/interdep/a/module.ceylon", "module/interdep/a/package.ceylon", "module/interdep/a/b.ceylon", "module/interdep/a/A.ceylon",
                "module/interdep/b/module.ceylon", "module/interdep/b/package.ceylon", "module/interdep/b/a.ceylon", "module/interdep/b/B.ceylon");
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.interdep.a", "6.6.6");
        assertTrue(carFile.exists());

        carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.interdep.b", "6.6.6");
        assertTrue(carFile.exists());
        
        // then try to compile only one module (the other being loaded from its car) 
        compile("module/interdep/a/module.ceylon", "module/interdep/a/b.ceylon", "module/interdep/a/A.ceylon");
    }

    @Test
    public void testMdlDependentModule(){
        // Compile only the first module 
        compile("module/depend/a/module.ceylon", "module/depend/a/package.ceylon", "module/depend/a/A.ceylon");
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.depend.a", "6.6.6");
        assertTrue(carFile.exists());

        // then try to compile only one module (the other being loaded from its car) 
        compile("module/depend/b/module.ceylon", "module/depend/b/a.ceylon", "module/depend/b/aWildcard.ceylon");

        carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.depend.b", "6.6.6");
        assertTrue(carFile.exists());
    }

    @Test
    public void testMdlImplicitDependentModule(){
        // Compile only the first module 
        compile("module/implicit/a/module.ceylon", "module/implicit/a/package.ceylon", "module/implicit/a/A.ceylon",
                "module/implicit/b/module.ceylon", "module/implicit/b/package.ceylon", "module/implicit/b/B.ceylon", "module/implicit/b/B2.ceylon",
                "module/implicit/c/module.ceylon", "module/implicit/c/package.ceylon", "module/implicit/c/c.ceylon");
        
        // Dependencies:
        //
        // c.ceylon--> B2.ceylon
        //         |
        //         '-> B.ceylon  --> A.ceylon

        // Successfull tests :
        
        compile("module/implicit/c/c.ceylon");
        compile("module/implicit/b/B.ceylon", "module/implicit/c/c.ceylon");
        compile("module/implicit/b/B2.ceylon", "module/implicit/c/c.ceylon");
        
        // Failing tests :
        
        Boolean success1 = getCompilerTask("module/implicit/c/c.ceylon", "module/implicit/b/B.ceylon").call();
        // => B.ceylon : package not found in dependent modules: com.redhat.ceylon.compiler.java.test.cmr.module.implicit.a
        Boolean success2 = getCompilerTask("module/implicit/c/c.ceylon", "module/implicit/b/B2.ceylon").call();
        // => c.ceylon : TypeVisitor caused an exception visiting Import node: com.sun.tools.javac.code.Symbol$CompletionFailure: class file for com.redhat.ceylon.compiler.test.cmr.module.implicit.a.A not found at unknown

        Assert.assertTrue(success1 && success2);
    }

    private void copy(File source, File dest) throws IOException {
        InputStream inputStream = new FileInputStream(source);
        OutputStream outputStream = new FileOutputStream(dest); 
        byte[] buffer = new byte[4096];
        int read;
        while((read = inputStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, read);
        }
        inputStream.close();
        outputStream.close();
    }
    
    @Test
    public void testMdlSuppressObsoleteClasses() throws IOException{
        File sourceFile = new File(path, "module/single/SuppressClass.ceylon");

        copy(new File(path, "module/single/SuppressClass_1.ceylon"), sourceFile);
        CeyloncTaskImpl compilerTask = getCompilerTask("module/single/module.ceylon", "module/single/SuppressClass.ceylon");
        Boolean success = compilerTask.call();
        assertTrue(success);

        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertTrue(carFile.exists());
        ZipFile car = new ZipFile(carFile);
        ZipEntry oneClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/One.class");
        assertNotNull(oneClass);
        ZipEntry twoClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/Two.class");
        assertNotNull(twoClass);
        car.close();

        copy(new File(path, "module/single/SuppressClass_2.ceylon"), sourceFile);
        compilerTask = getCompilerTask("module/single/module.ceylon", "module/single/SuppressClass.ceylon");
        success = compilerTask.call();
        assertTrue(success);
        
        carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        assertTrue(carFile.exists());
        car = new ZipFile(carFile);
        oneClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/One.class");
        assertNotNull(oneClass);
        twoClass = car.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/Two.class");
        assertNull(twoClass);
        car.close();
        
        sourceFile.delete();
    }

    
    @Test
    public void testMdlMultipleRepos(){
        cleanCars("build/ceylon-cars-a");
        cleanCars("build/ceylon-cars-b");
        cleanCars("build/ceylon-cars-c");
        
        // Compile the first module in its own repo 
        File repoA = new File("build/ceylon-cars-a");
        repoA.mkdirs();
        Boolean result = getCompilerTask(Arrays.asList("-out", repoA.getPath()),
                "module/depend/a/module.ceylon", "module/depend/a/package.ceylon", "module/depend/a/A.ceylon").call();
        Assert.assertEquals(Boolean.TRUE, result);
        
        File carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.depend.a", "6.6.6", repoA.getPath());
        assertTrue(carFile.exists());

        // make another repo for the second module
        File repoB = new File("build/ceylon-cars-b");
        repoB.mkdirs();

        // then try to compile only one module (the other being loaded from its car) 
        result = getCompilerTask(Arrays.asList("-out", repoB.getPath(), "-rep", repoA.getPath()),
                "module/depend/b/module.ceylon", "module/depend/b/package.ceylon", "module/depend/b/a.ceylon", "module/depend/b/B.ceylon").call();
        Assert.assertEquals(Boolean.TRUE, result);

        carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.depend.b", "6.6.6", repoB.getPath());
        assertTrue(carFile.exists());

        // make another repo for the third module
        File repoC = new File("build/ceylon-cars-c");
        repoC.mkdirs();

        // then try to compile only one module (the others being loaded from their car) 
        result = getCompilerTask(Arrays.asList("-out", repoC.getPath(), 
                "-rep", repoA.getPath(), "-rep", repoB.getPath()),
                "module/depend/c/module.ceylon", "module/depend/c/a.ceylon", "module/depend/c/b.ceylon").call();
        Assert.assertEquals(Boolean.TRUE, result);

        carFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.depend.c", "6.6.6", repoC.getPath());
        assertTrue(carFile.exists());
    }

    @Test
    public void testMdlJarDependency() throws IOException{
        // compile our java class
        File outputFolder = new File("build/java-jar");
        cleanCars(outputFolder.getPath());
        outputFolder.mkdirs();
        
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null, null);
        File javaFile = new File(path+"/module/jarDependency/java/JavaDependency.java");
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFile);
        CompilationTask task = javaCompiler.getTask(null, null, null, Arrays.asList("-d", "build/java-jar", "-sourcepath", "test-src"), null, compilationUnits);
        assertEquals(Boolean.TRUE, task.call());
        
        File jarFolder = new File(outputFolder, "com/redhat/ceylon/compiler/java/test/cmr/module/jarDependency/java/1.0/");
        jarFolder.mkdirs();
        File jarFile = new File(jarFolder, "com.redhat.ceylon.compiler.java.test.cmr.module.jarDependency.java-1.0.jar");
        // now jar it up
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jarFile));
        ZipEntry entry = new ZipEntry("com/redhat/ceylon/compiler/java/test/cmr/module/jarDependency/java/JavaDependency.class");
        outputStream.putNextEntry(entry);
        
        FileInputStream inputStream = new FileInputStream(javaFile);
        Util.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();

        // Try to compile the ceylon module
        CeyloncTaskImpl ceylonTask = getCompilerTask(Arrays.asList("-out", destDir, "-rep", outputFolder.getPath()), 
                (DiagnosticListener<? super FileObject>)null, 
                "module/jarDependency/ceylon/module.ceylon", "module/jarDependency/ceylon/Foo.ceylon");
        assertEquals(Boolean.TRUE, ceylonTask.call());
    }

    @Test
    public void testMdlMavenDependency() throws IOException{
        // Try to compile the ceylon module
        CeyloncTaskImpl ceylonTask = getCompilerTask(Arrays.asList("-out", destDir, "-rep", "mvn:http://repo1.maven.org/maven2"), 
                (DiagnosticListener<? super FileObject>)null, 
                "module/maven/module.ceylon", "module/maven/foo.ceylon");
        assertEquals(Boolean.TRUE, ceylonTask.call());
    }

    @Test
    public void testMdlSourceArchive() throws IOException{
        File sourceArchiveFile = getSourceArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        sourceArchiveFile.delete();
        assertFalse(sourceArchiveFile.exists());

        // compile one file
        compile("module/single/module.ceylon");

        // make sure it was created
        assertTrue(sourceArchiveFile.exists());

        JarFile sourceArchive = new JarFile(sourceArchiveFile);
        assertEquals(1, countEntries(sourceArchive));

        ZipEntry moduleClass = sourceArchive.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/module.ceylon");
        assertNotNull(moduleClass);
        sourceArchive.close();

        // now compile another file
        compile("module/single/subpackage/Subpackage.ceylon");

        // MUST reopen it
        sourceArchive = new JarFile(sourceArchiveFile);
        assertEquals(2, countEntries(sourceArchive));

        ZipEntry subpackageClass = sourceArchive.getEntry("com/redhat/ceylon/compiler/java/test/cmr/module/single/subpackage/Subpackage.ceylon");
        assertNotNull(subpackageClass);
        sourceArchive.close();
    }

    @Test
    public void testMdlMultipleVersions(){
        // Compile module A/1
        Boolean result = getCompilerTask(Arrays.asList("-src", path+"/module/multiversion/a1"),
                "module/multiversion/a1/a/module.ceylon", "module/multiversion/a1/a/package.ceylon", "module/multiversion/a1/a/A.ceylon").call();
        Assert.assertEquals(Boolean.TRUE, result);
        
        ErrorCollector collector = new ErrorCollector();
        // Compile module A/2 with B importing A/1
        result = getCompilerTask(Arrays.asList("-src", path+"/module/multiversion/a2:"+path+"/module/multiversion/b"),
                collector,
                "module/multiversion/a2/a/module.ceylon", "module/multiversion/a2/a/package.ceylon", "module/multiversion/a2/a/A.ceylon",
                "module/multiversion/b/b/module.ceylon", "module/multiversion/b/b/B.ceylon").call();
        Assert.assertEquals(Boolean.FALSE, result);
        
        compareErrors(collector.actualErrors, new CompilerError(-1, "Trying to import or compile two different versions of the same module: a (1 and 2)"));
    }

    private int countEntries(JarFile jar) {
        int count = 0;
        Enumeration<JarEntry> entries = jar.entries();
        while(entries.hasMoreElements()){
            count++;
            entries.nextElement();
        }
        return count;
    }

    @Test
    public void testMdlSha1Signatures() throws IOException{
        File sourceArchiveFile = getSourceArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        File sourceArchiveSignatureFile = new File(sourceArchiveFile.getPath()+".sha1");
        File moduleArchiveFile = getModuleArchive("com.redhat.ceylon.compiler.java.test.cmr.module.single", "6.6.6");
        File moduleArchiveSignatureFile = new File(moduleArchiveFile.getPath()+".sha1");
        // cleanup
        sourceArchiveFile.delete();
        sourceArchiveSignatureFile.delete();
        moduleArchiveFile.delete();
        moduleArchiveSignatureFile.delete();
        // safety check
        assertFalse(sourceArchiveFile.exists());
        assertFalse(sourceArchiveSignatureFile.exists());
        assertFalse(moduleArchiveFile.exists());
        assertFalse(moduleArchiveSignatureFile.exists());

        // compile one file
        compile("module/single/module.ceylon");

        // make sure everything was created
        assertTrue(sourceArchiveFile.exists());
        assertTrue(sourceArchiveSignatureFile.exists());
        assertTrue(moduleArchiveFile.exists());
        assertTrue(moduleArchiveSignatureFile.exists());

        // check the signatures vaguely
        checkSha1(sourceArchiveSignatureFile);
        checkSha1(moduleArchiveSignatureFile);
    }

    private void checkSha1(File signatureFile) throws IOException {
        Assert.assertEquals(40, signatureFile.length());
        FileInputStream reader = new FileInputStream(signatureFile);
        byte[] bytes = new byte[40];
        Assert.assertEquals(40, reader.read(bytes));
        reader.close();
        char[] sha1 = new String(bytes, "ASCII").toCharArray();
        for (int i = 0; i < sha1.length; i++) {
            char c = sha1[i];
            Assert.assertTrue((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F'));
        }
    }
}
