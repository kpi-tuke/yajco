package yajco.generator.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.annotation.DependsOn;
import yajco.generator.parsergen.CompilerGenerator;

public class ServiceFinder {

    /**
     * Discovers and loads all FilesGenerator implementations via ServiceLoader.
     * Filters generators based on enabled/disabled properties and validates required dependencies.
     * 
     * <p>Property format: Use the fully qualified class name as key with "true"/"false" value:
     * <br>Example: {@code com.example.MyGenerator=false} (case-insensitive)
     * <br>Default: generators are enabled if property not specified
     * 
     * @param properties configuration properties to enable/disable generators
     * @return Set of all enabled FilesGenerator instances with satisfied dependencies
     * @throws GeneratorException if a required @DependsOn dependency is missing
     */
    public static Set<FilesGenerator> findFilesGenerators(Properties properties) {
        Map<String, FilesGenerator> generators = new HashMap<String, FilesGenerator>();
        ServiceLoader<FilesGenerator> compilerServiceLoader = ServiceLoader.load(FilesGenerator.class, ServiceFinder.class.getClassLoader());
        for (FilesGenerator filesGenerator : compilerServiceLoader) {
            String className = filesGenerator.getClass().getCanonicalName();
            System.out.print("Loaded FilesGenerator: "+className);
            if ("true".equalsIgnoreCase(properties.getProperty(className, "true"))) {
                generators.put(className, filesGenerator);
                System.out.println();
            } else {
                System.out.println("(DISABLED in settings)");
            }
        }

        for (FilesGenerator filesGenerator : generators.values()) {
            DependsOn dependsOn = filesGenerator.getClass().getAnnotation(DependsOn.class);
            if (dependsOn != null) {
                for (String name : dependsOn.value()) {
                    if (!generators.containsKey(name)) {
                        throw new GeneratorException("Required generator " + name + " is not included in project. Required by " + filesGenerator.getClass().getCanonicalName());
                    }
                }
            }
        }

        return new HashSet<FilesGenerator>(generators.values());
    }

    /**
     * Discovers the CompilerGenerator implementation via ServiceLoader.
     * Returns the first implementation found; logs warning if multiple exist.
     * 
     * @return the first CompilerGenerator found, or null if none available
     */
    public static CompilerGenerator findCompilerGenerator() {
        ServiceLoader<CompilerGenerator> compilerServiceLoader = ServiceLoader.load(CompilerGenerator.class, ServiceFinder.class.getClassLoader());
        CompilerGenerator compilerGenerator = null;
        int count = 0;
        for (CompilerGenerator compGen : compilerServiceLoader) {
            count++;
            //LOG name
            System.out.println("Found compiler generator: " + compGen.getClass().getName() + " [" + compGen.getClass().getClassLoader().getResource(compGen.getClass().getName().replace('.', '/') + ".class") + "]");

            if (compilerGenerator == null) {
                compilerGenerator = compGen;
            }
        }
        if (count > 0) {
            if (count > 1) {
                // LOG WARNING
                System.out.println("WARNING: Found more than 1 compiler generator!!!! Will use only one.");
            }
            System.out.println("Selected compiler generator: " + compilerGenerator.getClass().getName());
        } else {
            //LOG ERROR
            //don't throw error, return null - handle later
            //throw new GeneratorException("No compiler generator in class path. Include service implementation of " + CompilerGenerator.class.getName() + " in your classpath.");
        }
        return compilerGenerator;
    }
}
