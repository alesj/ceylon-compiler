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

package com.redhat.ceylon.compiler.java.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.compiler.java.util.ShaSigner;
import com.redhat.ceylon.compiler.java.util.Util;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;

public class JarOutputRepositoryManager {
    
    private Map<Module,ProgressiveJar> openJars = new HashMap<Module, ProgressiveJar>();
    private Log log;
    private Options options;
    private CeyloncFileManager ceyloncFileManager;
    
    JarOutputRepositoryManager(Log log, Options options, CeyloncFileManager ceyloncFileManager){
        this.log = log;
        this.options = options;
        this.ceyloncFileManager = ceyloncFileManager;
    }
    
    public JavaFileObject getFileObject(RepositoryManager repositoryManager, Module module, String fileName, File sourceFile) throws IOException{
        ProgressiveJar progressiveJar = getProgressiveJar(repositoryManager, module);
        return progressiveJar.getJavaFileObject(fileName, sourceFile);
    }
    
    private ProgressiveJar getProgressiveJar(RepositoryManager repositoryManager, Module module) throws IOException {
        ProgressiveJar jarFile = openJars.get(module);
        if(jarFile == null){
            jarFile = new ProgressiveJar(repositoryManager, module, log, options, ceyloncFileManager);
            openJars.put(module, jarFile);
        }
        return jarFile;
    }

    public void flush() throws IOException {
        try{
            for(ProgressiveJar jarFile : openJars.values()){
                jarFile.close();
            }
        }finally{
            // make sure we clear on return and throw, so we don't try to flush again on throw
            openJars.clear();
        }
    }
    
    static class ProgressiveJar {
        private static final String MAPPING_FILE = "META-INF/mapping.txt";
        private File originalJarFile;
        private File outputJarFile;
        private JarOutputStream jarOutputStream;
        private File originalSrcFile;
        private File outputSrcFile;
        private JarOutputStream srcOutputStream;
        final private Set<String> modifiedSourceFiles = new HashSet<String>();
        final private Properties writtenClassesMapping = new Properties(); 
        private Log log;
        private Options options;
        private CeyloncFileManager ceyloncFileManager;
        private RepositoryManager repoManager;
        private ArtifactContext carContext;
        private ArtifactContext srcContext;
        
        public ProgressiveJar(RepositoryManager repoManager, Module module, Log log, Options options, CeyloncFileManager ceyloncFileManager) throws IOException{
            this.log = log;
            this.options = options;
            this.ceyloncFileManager = ceyloncFileManager;
            this.repoManager = repoManager;
            this.carContext = new ArtifactContext(module.getNameAsString(), module.getVersion(), ArtifactContext.CAR);
            this.srcContext = new ArtifactContext(module.getNameAsString(), module.getVersion(), ArtifactContext.SRC);

            setupJarOutput();
            setupSrcOutput();
        }

        private void setupJarOutput() throws IOException {
            File targetJarFile = repoManager.getArtifact(carContext);
            outputJarFile = File.createTempFile("car", ".tmp");
            originalJarFile = targetJarFile;
            jarOutputStream = new JarOutputStream(new FileOutputStream(outputJarFile));
        }

        private void setupSrcOutput() throws IOException {
            File targetSrcFile = repoManager.getArtifact(srcContext);
            outputSrcFile = File.createTempFile("src", ".tmp");
            originalSrcFile = targetSrcFile;
            srcOutputStream = new JarOutputStream(new FileOutputStream(outputSrcFile));
        }

        private Properties getPreviousMapping() throws IOException {
            if (originalJarFile != null) {
                JarFile jarFile = null;
                jarFile = new JarFile(originalJarFile);
                try {
                    JarEntry entry = jarFile.getJarEntry(MAPPING_FILE);
                    if (entry != null) {
                        InputStream inputStream = jarFile.getInputStream(entry);
                        try {
                            Properties previousMapping = new Properties();
                            previousMapping.load(inputStream);
                            return previousMapping;
                        } finally {
                            inputStream.close();
                        }
                    }
                } finally {
                    jarFile.close();
                }
            }
            return null;
        }
        
        private static interface EntryFilter {
            boolean avoid(String entryFullName);
        }

        public void close() throws IOException {
            final Set<String> copiedSourceFiles = copySourceFiles();
            finishUpdatingJar(originalSrcFile, outputSrcFile, srcContext, srcOutputStream, new EntryFilter() {
                @Override
                public boolean avoid(String entryFullName) {
                    return copiedSourceFiles.contains(entryFullName);
                }
            });

            final Properties previousMapping = getPreviousMapping();
            EntryFilter filterForCars = new EntryFilter() {
                @Override
                public boolean avoid(String entryFullName) {
                    boolean classWasUpdated = writtenClassesMapping.containsKey(entryFullName);
                    if (previousMapping != null) {
                        String sourceFileForClass = previousMapping.getProperty(entryFullName);
                        classWasUpdated = classWasUpdated || copiedSourceFiles.contains(sourceFileForClass);
                    }
                    return classWasUpdated || entryFullName.equals(MAPPING_FILE);
                }
            };
            writeMappingJarEntry(previousMapping, filterForCars);
            finishUpdatingJar(originalJarFile, outputJarFile, carContext, jarOutputStream, filterForCars);
        }

        private void writeMappingJarEntry(Properties previousMapping, EntryFilter filter) {
            Properties newMapping = new Properties();
            newMapping.putAll(writtenClassesMapping);
            if (previousMapping != null) {
                // Add the previous mapping entries that are not related to an updated source file 
                for (String classFullName : previousMapping.stringPropertyNames() ) {
                    if (! filter.avoid(classFullName)) {
                        newMapping.setProperty(classFullName, previousMapping.getProperty(classFullName));
                    }
                }
            }
            // Write the mapping file to the Jar
            try {
                jarOutputStream.putNextEntry(new ZipEntry(MAPPING_FILE));
                newMapping.store(jarOutputStream, "");
            }
            catch(IOException e) {
                // TODO : log to the right place
            }
            finally {
                try {
                    jarOutputStream.closeEntry();
                } catch (IOException e) {
                }
            }
        }

        private Set<String> copySourceFiles() throws IOException {
            Set<String> copiedFiles = new HashSet<String>();
            for(String prefixedSourceFile : modifiedSourceFiles){
                // must remove the prefix first
                String sourceFile = toPlatformIndependentPath(prefixedSourceFile);
                srcOutputStream.putNextEntry(new ZipEntry(sourceFile));
                try {
                    InputStream inputStream = new FileInputStream(prefixedSourceFile);
                    try {
                        Util.copy(inputStream, srcOutputStream);
                    } finally {
                        inputStream.close();
                    }
                } finally {
                    srcOutputStream.closeEntry();
                }
                copiedFiles.add(sourceFile);
            }
            return copiedFiles;
        }

        private String toPlatformIndependentPath(String prefixedSourceFile) {
            String sourceFile = getSourceFilePath(ceyloncFileManager, prefixedSourceFile);
            // zips are UNIX-friendly
            sourceFile = sourceFile.replace(File.separatorChar, '/');
            return sourceFile;
        }
        
        private static String getSourceFilePath(JavacFileManager fileManager, String file){
            Iterable<? extends File> prefixes = fileManager.getLocation(StandardLocation.SOURCE_PATH);

            // find the matching source prefix
            int srcDirLength = 0;
            for (File prefixFile : prefixes) {
                String prefix = prefixFile.getPath();
                if (file.startsWith(prefix) && prefix.length() > srcDirLength) {
                    srcDirLength = prefix.length();
                }
                String absPrefix = prefixFile.getAbsolutePath();
                if (file.startsWith(absPrefix) && absPrefix.length() > srcDirLength) {
                    srcDirLength = absPrefix.length();
                }
            }
            
            String path = file.substring(srcDirLength);
            if(path.startsWith(File.separator))
                path = path.substring(1);
            return path;
        }

        public void finishUpdatingJar(File originalFile, File outputFile, ArtifactContext context, 
                JarOutputStream jarOutputStream, EntryFilter filter) throws IOException {
            // now copy all previous jar entries
            if(originalFile != null){
                JarFile jarFile = new JarFile(originalFile);
                Enumeration<JarEntry> entries = jarFile.entries();
                while(entries.hasMoreElements()){
                    JarEntry entry = entries.nextElement();
                    // skip the old entry if we overwrote it
                    if(filter.avoid(entry.getName()))
                        continue;
                    ZipEntry copiedEntry = new ZipEntry(entry.getName());
                    // Preserve the modification time and comment
                    copiedEntry.setTime(entry.getTime());
                    copiedEntry.setComment(entry.getComment());
                    jarOutputStream.putNextEntry(copiedEntry);
                    InputStream inputStream = jarFile.getInputStream(entry);
                    Util.copy(inputStream, jarOutputStream);
                    inputStream.close();
                    jarOutputStream.closeEntry();
                }
                jarFile.close();
            }
            jarOutputStream.flush();
            jarOutputStream.close();
            if(options.get(OptionName.VERBOSE) != null){
                Log.printLines(log.noticeWriter, "[done writing to jar: "+outputFile.getPath()+"]");
            }
            File sha1File = ShaSigner.sign(outputFile, log, options);
            try{
                context.setForceOperation(true);
                repoManager.putArtifact(context, outputFile);
                ArtifactContext sha1Context = context.getSha1Context();
                sha1Context.setForceOperation(true);
                repoManager.putArtifact(sha1Context, sha1File);
            }catch(RuntimeException x){
                log.error("ceylon", "Failed to write module to repository: "+x.getMessage());
                // fatal errors go all the way up but don't print anything if we logged an error
                throw x;
            }finally{
                // now cleanup
                outputFile.delete();
                sha1File.delete();
            }
        }

        public JavaFileObject getJavaFileObject(String fileName, File sourceFile) {
            modifiedSourceFiles.add(sourceFile.getPath());
            // record the class file we produce so that we don't save it from the original jar
        	fileName = fileName.replace(File.separatorChar, '/');
        	addMappingEntry(fileName, toPlatformIndependentPath(sourceFile.getPath()));
            return new JarEntryFileObject(outputJarFile.getPath(), jarOutputStream, fileName);
        }

        private void addMappingEntry(String className,
                String sourcePath) {
            writtenClassesMapping.put(className, sourcePath);
        }
    }
}
