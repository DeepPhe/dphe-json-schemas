package dphe;

import dphe.utils.GitUtil;
import dphe.utils.SchemaUtil;
import dphe.utils.SourceUtil;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {
    public static void main(String[] args) throws Exception {
        // Get the current application's classpath which includes all Maven dependencies
        String currentClasspath = System.getProperty("java.class.path");
        System.out.println("Debug - Classpath: " + currentClasspath);
        if (args.length < 4) {
            System.err.println("Usage: <repoUrl> <branchOrTag> <relativeSourceDir> <outputDir>");
            System.exit(1);
        }
        String repoUrl = args[0];
        String branch = args[1];
        String relativeSourceDir = args[2].replace("\\", "/");
        Path outputDir = Paths.get(args[3]).toAbsolutePath();
        Files.createDirectories(outputDir);
// 1) Clone shallow to temp dir
        Path workDir = Files.createTempDirectory("git2schema-");
        System.out.println("Cloning into: " + workDir);
        GitUtil.cloneShallow(repoUrl, branch, workDir);
// 2) Resolve target source directory inside the repo
        Path sourceDir = workDir.resolve(relativeSourceDir);
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Relative directory not found in repo: " + sourceDir);
        }
        System.out.println("Target source dir: " + sourceDir);
// 3) Gather .java files from both target directory AND its parent directory
        List<Path> javaFiles = new ArrayList<>();
// First add files from parent directory (that need to be compiled first)
        Path parentDir = sourceDir.getParent();
        if (parentDir != null && Files.isDirectory(parentDir)) {
            try (Stream<Path> s = Files.walk(parentDir)) {
                List<Path> parentFiles = s.filter(p -> p.toString().endsWith(".java")).filter(p -> !p.startsWith(sourceDir)) // Skip files in target dir (will add them next)
                        .collect(Collectors.toList());
                javaFiles.addAll(parentFiles);
            }
        }
// Then add files from target directory
        try (Stream<Path> s = Files.walk(sourceDir)) {
            List<Path> targetFiles = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
            javaFiles.addAll(targetFiles);
        }
        if (javaFiles.isEmpty()) {
            System.out.println("No .java files found in target directory.");
            return;
        }
// 4) Compile to a temp classes dir (use repo root as sourcepath)
        Path classesDir = Files.createDirectories(workDir.resolve(".classes"));
        List<Path> extraClassPath = new ArrayList<>();
        extraClassPath.add(workDir.resolve("src/main/java")); // Add the base source directory
        boolean compiled = SourceUtil.compile(javaFiles, workDir, classesDir, extraClassPath);
        if (!compiled) {
            System.err.println("Compilation failed. See diagnostics above.");
            System.exit(2);
        }
// 5) Map sources -> fully-qualified class names via package declarations
        Map<Path, String> fqnMap = new LinkedHashMap<>();
        for (Path file : javaFiles) {
            String fqn = SourceUtil.inferFqnFromSource(file, workDir);
            if (fqn != null) fqnMap.put(file, fqn);
        }
// 6) Load classes and generate schemas
        try (URLClassLoader cl = new URLClassLoader(new URL[]{classesDir.toUri().toURL()})) {
            SchemaUtil schemaUtil = new SchemaUtil(cl);
            for (Map.Entry<Path, String> e : fqnMap.entrySet()) {
                String fqn = e.getValue();
                try {
                    Class<?> clazz = Class.forName(fqn, false, cl);
                    if (clazz.isAnnotation() || clazz.isInterface() || clazz.isEnum()) {
                        System.out.println("Skipping non-POJO: " + fqn);
                        continue;
                    }
                    String json = schemaUtil.generateSchemaJson(clazz);
                    String simple = clazz.getSimpleName();
                    Path out = outputDir.resolve(simple + ".schema.json");
                    Files.writeString(out, json);
                    System.out.println("Wrote: " + out);
                } catch (Throwable ex) {
                    System.err.println("Failed schema for " + fqn + ": " + ex);
                }
            }
        }
        System.out.println("Done. Schemas in: " + outputDir);
    }
}