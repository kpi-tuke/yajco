package yajco.annotation.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yajco.annotation.Exclude;
import yajco.annotation.config.Parser;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.util.ServiceFinder;
import yajco.model.Language;
import yajco.printer.Printer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@SupportedAnnotationTypes({"yajco.annotation.config.Parser", "yajco.annotation.Exclude"})
//Nutnost upravit reference resolver, ktory funguje len s jednym konstruktoroms
//Najlepsie to uplne zmenit podla novej struktury
//TODO: zabezpecit lahsie rozsirenie o rozne anotacie - passing to model bez testovania
//Mozno by v anotacii mozhlo by zapisane na co sa to mapuje cez metaanotaciu -
//rovno skontruujem objekt a skopirujem rovnake vlasnosti
//Pozadujem prazdny konstrutkor a priamo vlozim do premennych
public class AnnotationProcessor extends AbstractProcessor {

    /**
     * Version string.
     */
    private static final String VERSION = "0.7.0";
    private static final String PROPERTY_SETTINGS_FILE = "/yajco.properties";
    private static final Logger logger = LoggerFactory.getLogger("YAJCO Annotation Processor");
    /**
     * Stored round environment.
     */
    private RoundEnvironment roundEnv;
    private Properties baseProperties;
    private Properties properties;
    private Language language;
    private Set<String> excludes = new HashSet<>();
    private Set<? extends Element> rootElements;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Check if processing is over and write all collected parser service providers
        if (roundEnv.processingOver()) {
            try {
                CompilerGenerator.writeParserServiceProviders(processingEnv.getFiler());
            } catch (IOException e) {
                logger.error("Error writing parser service providers", e);
            }
            return false;
        }

        // Initialize properties
        baseProperties = this.loadSettingsFromSettingsFile();

        // Check if YAJCo is not disabled.
        if ("false".equalsIgnoreCase(baseProperties.getProperty("yajco"))) {
            logger.info("Property 'yajco' set to false - terminating YAJCo tool!");
            return false;
        }

        this.roundEnv = roundEnv;
        rootElements = roundEnv.getRootElements();
        this.processExcludedElements();

        try {
            if (hasParserAnnotation(annotations)) {
                System.out.println("YAJCo parser generator " + VERSION);

                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(yajco.annotation.config.Parser.class);
                if (elements.isEmpty()) {
                    return false;
                }
                if (elements.size() > 1) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "More than one @Parser annotation found. Multiple parsers are not fully supported.");
                }

                // Process each @Parser annotation
                for (Element parserAnnotationElement : elements) {
                    processParser(parserAnnotationElement);
                }

                // Generate all tools.
                generateAllTools();
            }
        } catch (Throwable e) {
            //e.printStackTrace();
            // asi CHYBA v MAVEN ze nevie dostat Messager
            //processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());

            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return false;
    }

    private static boolean hasParserAnnotation(Set<? extends TypeElement> annotations) {
        return annotations.stream().anyMatch(
            typeElement -> typeElement.getQualifiedName().contentEquals(Parser.class.getName()));
    }

    private void processParser(Element parserAnnotationElement) {
        // Initialize properties for this parser
        properties = createParserProperties(baseProperties);

        // Disable class generating - annotation processor works on classes, don't generate new ones.
        properties.setProperty("yajco.generator.classgen.ClassGenerator", "false");

        Parser parserAnnotation = parserAnnotationElement.getAnnotation(Parser.class);
        LanguageModelBuilder modelBuilder = new LanguageModelBuilder(properties, processingEnv, rootElements, excludes);
        language = modelBuilder.createLanguageModel(parserAnnotationElement, parserAnnotation);

        printLanguage(language);

        // Generate compiler.
        if (!("false".equalsIgnoreCase(properties.getProperty("yajco.generateParser")))) {
            String parserClassName = parserAnnotation.className();
            generateCompiler(parserClassName);
        }
    }

    /**
     * Loads settings from settings file to Properties object.
     */
    private Properties loadSettingsFromSettingsFile() {
        Properties loadedProperties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream(PROPERTY_SETTINGS_FILE)) {
            loadedProperties.load(inputStream);
            logger.debug("Loaded config from file: {}", loadedProperties);
        } catch (Exception e) {
            // LOG it but don't do anything, it is not an error
            logger.info("Cannot find or load {} file in classpath. Will use only @Parser options.", PROPERTY_SETTINGS_FILE);
            logger.debug("Loading config file: {}", e.getLocalizedMessage());
        }
        return loadedProperties;
    }

    private static Properties createParserProperties(Properties baseProperties) {
        Properties parserProperties = new Properties();
        if (baseProperties != null) {
            parserProperties.putAll(baseProperties);
        }
        return parserProperties;
    }

    /**
     * Collects and prints names of all excluded elements, which will not not be processed by YAJCo.
     */
    private void processExcludedElements() {
        this.roundEnv.getElementsAnnotatedWith(Exclude.class).forEach(excludedElement -> {
            try {
                // Invokes exception to access class names.
                excludedElement.getAnnotation(Exclude.class).value();
            } catch (MirroredTypesException e) {
                for (TypeMirror type : e.getTypeMirrors()) {
                    excludes.add(type.toString());
                }
            }
        });
        this.printExcludedClassses();
    }

    /**
     * Prints names of all excluded elements.
     */
    private void printExcludedClassses() {
        for (String clazz : excludes) {
            System.out.println("====exclude---> " + clazz);
        }
    }

    /**
     * Prints created language.
     */
    private static void printLanguage(Language language) {
        Printer printer = new Printer();
        System.out.println("--------------------------------------------------------------------------------------------------------");
        printer.printLanguage(new PrintWriter(System.out), language);
        System.out.println("--------------------------------------------------------------------------------------------------------");
    }

    /**
     * Generates all providing tools.
     */
    private void generateAllTools() {
        Set<FilesGenerator> tools = ServiceFinder.findFilesGenerators(properties);
        for (FilesGenerator filesGenerator : tools) {
            filesGenerator.generateFiles(language, processingEnv.getFiler(), properties);
        }
    }

    /**
     * Generates compiler for created language.
     *
     * @param parserClassName Class name for generated parser.
     * @throws GeneratorException When no compiler generator is found in classpath.
     */
    private void generateCompiler(String parserClassName) throws GeneratorException {
        CompilerGenerator compilerGenerator = ServiceFinder.findCompilerGenerator();

        if (compilerGenerator != null) {
            if (parserClassName != null && !parserClassName.isEmpty()) {
                compilerGenerator.generateFiles(language, processingEnv.getFiler(), properties, parserClassName);
            } else {
                compilerGenerator.generateFiles(language, processingEnv.getFiler(), properties);
            }
        } else {
            throw new GeneratorException("No compiler generator in class path. Include service implementation of " +
                    CompilerGenerator.class.getName() + " in your classpath. (see java.util.ServiceLoader javadoc for details)");
        }
    }
}
