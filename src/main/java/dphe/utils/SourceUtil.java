package dphe.utils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class SourceUtil {
  public static boolean compile(List<Path> javaFiles, Path sourceRoot, Path classesDir, List<Path> extraClassPath) throws IOException {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if (compiler == null)
          throw new IllegalStateException("No system JavaCompiler. Are you running a JRE instead of a JDK?");

      StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
      fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));
      Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(
          javaFiles.stream().map(Path::toFile).collect(Collectors.toList()));

      List<String> options = new ArrayList<>();
      options.add("-sourcepath");
      options.add(sourceRoot.toString());
      options.add("-Xlint:none");

      // Get the current application's classpath which includes all Maven dependencies
      String currentClasspath = System.getProperty("java.class.path");

      // Build classpath with both current classpath and any extra classpath provided
      StringBuilder classpath = new StringBuilder(currentClasspath);
      if (!extraClassPath.isEmpty()) {
          classpath.append(File.pathSeparator);
          classpath.append(extraClassPath.stream().map(Path::toString)
              .collect(Collectors.joining(File.pathSeparator)));
      }

      // Add classpath to compiler options
      options.add("-classpath");
      options.add(classpath.toString());

      JavaCompiler.CompilationTask task = compiler.getTask(null, fm, null, options, null, units);
      boolean ok = task.call();
      fm.close();
      return ok;
  }

    public static String inferFqnFromSource(Path javaFile, Path repoRoot) throws IOException {
        List<String> lines = Files.readAllLines(javaFile);
        String pkg = null;
        String cls = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                String s = line.substring("package ".length());
                if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
                pkg = s.trim();
            }
            if (cls == null && (line.startsWith("public class ") || line.startsWith("class "))) {
                cls = line.replace("public ", "").replace("class ", "").trim();
                int i = cls.indexOf(' ');
                if (i > 0) cls = cls.substring(0, i);
            }
            if (pkg != null && cls != null) break;
        }
        if (cls == null) {
// Fallback: derive from filename
            String name = javaFile.getFileName().toString();
            cls = name.endsWith(".java") ? name.substring(0, name.length() - 5) : name;
        }
        return pkg == null ? cls : pkg + "." + cls;
    }
}