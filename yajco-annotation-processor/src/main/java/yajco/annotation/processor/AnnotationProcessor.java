package yajco.annotation.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yajco.annotation.*;
import yajco.annotation.config.Option;
import yajco.annotation.config.Parser;
import yajco.annotation.config.Skip;
import yajco.annotation.reference.References;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.util.ServiceFinder;
import yajco.model.*;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PatternSupport;
import yajco.model.pattern.impl.Factory;
import yajco.model.type.*;
import yajco.model.utilities.XMLLanguageFormatHelper;
import yajco.printer.Printer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.*;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Optional;

@SupportedSourceVersion(SourceVersion.RELEASE_11)
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
    private static final String VERSION = "0.5.1";
    private static final String PROPERTY_SETTINGS_FILE = "/yajco.properties";
    private static final Logger logger = LoggerFactory.getLogger("YAJCO Annotation Processor");
    /**
     * Stored round environment.
     */
    private RoundEnvironment roundEnv;
    private Properties properties;
    private Set<String> excludes = new HashSet<>();
    /**
     * Builded language.
     */
    private Language language;

    /**
     * Set for concepts imported from previous JARs and needed for full analysis.
     */
    private Set<Concept> conceptsToProcess = new HashSet<>();

    /**
     * Used for creation of string tokens defined by @StringToken annotation.
     */
    private int stringTokenId = 1;
    private static final String DEFAULT_STRING_TOKEN_NAME = "STRING_TOKEN";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Leave only @Parser annotation for later processing.
        annotations.removeIf(typeElement -> typeElement.getQualifiedName().contentEquals(Exclude.class.getName()));

        properties = new Properties();
        this.loadSettingsFromSettingsFile();

        // Check if YAJCo is not disabled.
        if ("false".equalsIgnoreCase(properties.getProperty("yajco"))) {
            logger.info("Property 'yajco' set to false - terminating YAJCo tool!");
            return false;
        }

        // Disable class generating - annotation processor works on classes, don't generate new ones.
        properties.setProperty("yajco.generator.classgen.ClassGenerator", "false");

        this.roundEnv = roundEnv;
        this.processExcludedElements();

        try {
            if (annotations.size() == 1) {
                // Check if only one @Parser annotation is used. Parser generator works only with one @Parser annotation.
                if (roundEnv.getElementsAnnotatedWith(yajco.annotation.config.Parser.class).size() != 1) {
                    System.err.println("Elements annotated with @Parser annotation:");
                    for (Element element : roundEnv.getElementsAnnotatedWith(yajco.annotation.config.Parser.class)) {
                        System.err.println(element.asType().toString());
                    }
                    throw new GeneratorException("There should be only one @Parser annotation in the model.");
                }

                //logger.info("YAJCo parser generator {}", VERSION);
                System.out.println("YAJCo parser generator " + VERSION);

                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(yajco.annotation.config.Parser.class);

                // Find directory for saving generated files.
                // FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "temp.java");
                // targetDirectory = new File(fo.toUri()).getParentFile();

                // Take the first annotation (the only one).
                Element parserAnnotationElement = elements.iterator().next();
                Parser parserAnnotation = parserAnnotationElement.getAnnotation(Parser.class);

                this.extractOptionsFromParserAnnotation(parserAnnotation);

                // Extract the main element, package or type can be annotated with @Parser.
                ElementKind parserAnnotationElemKind = parserAnnotationElement.getKind();
                TypeElement mainElement;
                String mainElementName;
                if (parserAnnotationElemKind == ElementKind.PACKAGE) {
                    mainElementName = parserAnnotation.mainNode();
                    if (mainElementName.indexOf('.') == -1) {
                        //TODO: Chyba v javac - ak nexistovala trieda tak ju vytvorilo ale nehlasilo null
                        // ak som zavolal pre "Expression" vratilo sice null ale potom hlasilo duplicate class
                        //mainElement = processingEnv.getElementUtils().getTypeElement(mainElementName);
                        //mozno treba niekde v tools nastavit aby negenerovalo po otazke triedu ak neexistuje,
                        //zda sa mi ze som to niekde videl v helpe
                        mainElementName = ((PackageElement) parserAnnotationElement).getQualifiedName() + "." + mainElementName;
                    }
                    mainElement = processingEnv.getElementUtils().getTypeElement(mainElementName);
                } else if (parserAnnotationElemKind == ElementKind.CLASS || parserAnnotationElemKind == ElementKind.INTERFACE || parserAnnotationElemKind == ElementKind.ENUM) {
                    mainElement = (TypeElement) parserAnnotationElement;
                    mainElementName = mainElement.asType().toString();
                    properties.setProperty("yajco.mainNode", mainElementName);
                } else {
                    System.err.println("@Parser annotation can't be used on [" + parserAnnotationElemKind.toString() + "]\n");
                    throw new GeneratorException("Annotation @Parser should annotate only package, class, interface or enum.");
                }

                if (mainElement == null) {
                    throw new GeneratorException("Main language concept not found '" + parserAnnotation.mainNode() + "'");
                }

                //Map element to lines
//                Trees trees = Trees.instance(processingEnv);
//                SourcePositions sc = trees.getSourcePositions();
//                System.out.println("trees=" + trees + ", sc=" + sc);
//                TreePath path = trees.getPath(mainElement);
//                Tree tree = trees.getTree(mainElement);
//                CompilationUnitTree cut = path.getCompilationUnit();
//                LineMap lm = cut.getLineMap();
//                System.out.println("mainElement=" + mainElement + ", class=" + mainElement.getClass());
//                System.out.println("path=" + path);
//                System.out.println("cut=" + cut.getClass());
//                System.out.println("tree=" + tree.getClass());
//                System.out.println("lm=" + lm);
//                long startPosition = sc.getStartPosition(cut, tree);
//                long endPosition = sc.getEndPosition(cut, tree);
//                System.out.println("uri=" + cut.getSourceFile().toUri());
//                System.out.printf("Position (%d,%d) to (%d,%d)\n", lm.getLineNumber(startPosition), lm.getColumnNumber(startPosition), lm.getLineNumber(endPosition), lm.getColumnNumber(endPosition));


                // Create language.
                language = new Language(mainElement);
                // Add main package name == language name.
                String languageName = mainElementName.substring(0, mainElementName.lastIndexOf('.'));
                System.out.println("---- mainElementName: " + mainElementName + " == languageName: " + languageName);
                if (!languageName.isEmpty()) {
                    language.setName(languageName);
                }

                // Add language concepts from included JARs.
                addLanguageConceptsFromIncludedJARs();

                // Start processing with the main element.
                System.out.println(" ? mainElement ? : " + mainElement);
                processTypeElement(mainElement);

                // Add tokens and skips into language.
                addTokensAndSkipsIntoLanguage(parserAnnotation);

                // Convert properties to language settings.
                language.setSettings(LanguageSetting.convertToLanguageSetting(properties));

                // Print recognized language to output.
                printLanguage();

                // Generate compiler.
                if (!("false".equalsIgnoreCase(properties.getProperty("yajco.generateParser")))) {
                    String parserClassName = parserAnnotation.className();
                    generateCompiler(parserClassName);
                }

                // generates all new files
//            GeneratorHelper generatorHelper = new GeneratorHelper(language, targetDirectory, properties);
//            if (properties.containsKey("generateTools") && "true".equals(properties.getProperty("generateTools"))) {
//                generatorHelper.generateAllExceptModelClassFiles();
//            }

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

    /**
     * Loads settings from settings file to Properties object.
     */
    private void loadSettingsFromSettingsFile() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(PROPERTY_SETTINGS_FILE);
            properties.load(inputStream);
            logger.debug("Loaded config from file: {}", properties);
        } catch (Exception e) {
            // LOG it but don't do anything, it is not an error
            logger.info("Cannot find or load {} file in classpath. Will use only @Parser options.", PROPERTY_SETTINGS_FILE);
            logger.debug("Loading config file: {}", e.getLocalizedMessage());
        }
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
     * Collects all options from @Parser annotation and store them to Properties object. Sets className and mainNode
     * if they are defined in @Parser annotation.
     *
     * @param parserAnnotation @Parser annotation object.
     */
    private void extractOptionsFromParserAnnotation(Parser parserAnnotation) {
        for (Option option : parserAnnotation.options()) {
            properties.setProperty(option.name(), option.value());
        }

        if (!parserAnnotation.className().isEmpty()) {
            properties.setProperty("yajco.className", parserAnnotation.className());
        }
        if (!parserAnnotation.mainNode().isEmpty()) {
            properties.setProperty("yajco.mainNode", parserAnnotation.mainNode());
        }
    }

    /**
     * Loads language concepts, skips and tokens from languages included as JAR files.
     */
    private void addLanguageConceptsFromIncludedJARs() {
        List<Language> includedLanguages = XMLLanguageFormatHelper.getAllLanguagesFromXML();
        System.out.println("Loaded external language specifications: " + includedLanguages.size());
        for (Language incLang : includedLanguages) {
            System.out.print("Loaded from JAR: ");
            for (Concept concept : incLang.getConcepts()) {
                System.out.print(concept.getName() + ", ");
            }
            System.out.println();

            addToListAsSet(language.getSkips(), incLang.getSkips(), true);
            addToListAsSet(language.getTokens(), incLang.getTokens(), true);
            conceptsToProcess.addAll(incLang.getConcepts());
            language.getConcepts().addAll(incLang.getConcepts());
        }
    }

    /**
     * Adds tokens and skips defined in @Parser annotation into language.
     *
     * @param parserAnnotation @Parser annotation object
     */
    private void addTokensAndSkipsIntoLanguage(Parser parserAnnotation) {
        List<TokenDef> tokens = new ArrayList<>();
        List<SkipDef> skips = new ArrayList<>();
        for (yajco.annotation.config.TokenDef tokenDef : parserAnnotation.tokens()) {
            tokens.add(new TokenDef(tokenDef.name(), tokenDef.regexp(), tokenDef));
        }
        for (Skip skip : parserAnnotation.skips()) {
            skips.add(new SkipDef(skip.value(), skip));
        }

        // Add default white space for skips if empty.
        if (skips.isEmpty()) {
            skips.add(new SkipDef("\\s"));
        }
        addToListAsSet(language.getSkips(), skips, true);
        addToListAsSet(language.getTokens(), tokens, true);
    }

    /**
     * Prints created language.
     */
    private void printLanguage() {
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
     * Processes language model elements.
     *
     * @param typeElement Language model element to process.
     * @return Processed language concept.
     */
    private Concept processTypeElement(TypeElement typeElement) {
        return processTypeElement(typeElement, null);
    }

    /**
     * Processes language model elements and creates unique language concepts for all of them.
     *
     * @param typeElement Language model element to process.
     * @param superConcept Parent concept.
     * @return Processed language concept.
     */
    private Concept processTypeElement(TypeElement typeElement, Concept superConcept) {
        String name = typeElement.getQualifiedName().toString();
        System.out.println("---->>> Name: " + name + " [kind:" + typeElement.getKind() + "]");
        if (excludes.contains(name)) {
            return null;
        }

        if (language.getName() != null && !language.getName().isEmpty() && name.startsWith(language.getName())) {
            name = name.substring(language.getName().length() + 1); // +1 because of dot after package name '.'
        }

        Concept concept = language.getConcept(name);
        if (concept != null) { // Already processed
            if (superConcept != null) { // Set parent
                concept.setParent(superConcept);
            }
            // TODO:toto som tu doplnil len docasne na vyskusanie pre podporu kompozicie jazykov, treba to cele prehodnotit, lebo sa to nachadza aj na konci metody
            //processDirectSubclasses(typeElement, concept);
            if (conceptsToProcess.contains(concept)) {
                conceptsToProcess.remove(concept);
            } else {
                return concept;
            }
        } else {
            // Create concept.
            concept = new Concept(name, typeElement);
            concept.setParent(superConcept); //Set parent
            language.addConcept(concept);
        }

        processTypeElementAccordingToKind(typeElement, concept);

        // Add concept pattern from annotations (ConceptPattern).
        addPatternsFromAnnotations(typeElement, concept);
        processDirectSubclasses(typeElement, concept);
        return concept;
    }

    /**
     * Processes language model element according to its kind.
     *
     * @param typeElement Language model element.
     * @param concept Language concept for representing language model element.
     */
    private void processTypeElementAccordingToKind(TypeElement typeElement, Concept concept) {
        if (typeElement.getKind() == ElementKind.ENUM) { // Enum type
            processEnum(concept, typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) { // Class
            System.out.println(" modifiers: " + typeElement.getModifiers());
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) { // Abstract class
                processAbstractClass(concept, typeElement);
            } else {  //Concrete class
                processConcreteClass(concept, typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.INTERFACE) { // Interface
            processInterface(concept, typeElement);
        } else {
            throw new GeneratorException("Not supported type in model '" + typeElement + "'");
        }
    }

    /**
     * Processes Enum language model elements.
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processEnum(Concept concept, TypeElement typeElement) {
        concept.addPattern(new yajco.model.pattern.impl.Enum());
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.ENUM_CONSTANT) {
                continue; // Skip non enum constants.
            }

            Token tokenAnnotation = element.getAnnotation(Token.class);
            Notation notation = new Notation(typeElement);
            TokenPart tokenPart;
            if (tokenAnnotation != null) {
                tokenPart = new TokenPart(tokenAnnotation.value(), tokenAnnotation);
            } else {
                tokenPart = new TokenPart(element.getSimpleName().toString(), element);
            }
            notation.addPart(tokenPart);
            concept.addNotation(notation);
        }
    }

    /**
     * Processes concrete classes of language model.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void processConcreteClass(Concept concept, TypeElement classElement) {
        defineAbstractSytax(concept, classElement);
        defineConcreteSyntax(concept, classElement);
    }

    /**
     * Parses concepts concrete syntax.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void defineConcreteSyntax(Concept concept, TypeElement classElement) {
        Set<ExecutableElement> constructors = getConstructorsAndFactoryMethods(classElement);
        for (ExecutableElement constructor : constructors) {
            Notation notation = new Notation(constructor);

            // @Before annotation.
            if (constructor.getAnnotation(Before.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(Before.class).value());
            }

            for (VariableElement paramElement : constructor.getParameters()) {
                processParameter(concept, notation, paramElement);
            }

            // @After annotation.
            if (constructor.getAnnotation(After.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(After.class).value());
            }

            concept.addNotation(notation);
            if (constructor.getKind() == ElementKind.METHOD) {
                notation.addPattern(new Factory(constructor.getSimpleName().toString()));
            }

            //TODO: odstranit pri prenesesni @Operator na triedu
            // Add concept pattern from annotations (Type).
            addPatternsFromAnnotations(constructor, concept);
        }
    }

    /**
     * Parses concepts abstract syntax.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void defineAbstractSytax(Concept concept, TypeElement classElement) {
        System.out.println("starting ProcessConcreteClass method");
        for (Element element : classElement.getEnclosedElements()) {
            System.out.println("- enclosedElement: " + element.getSimpleName().toString() + "[" + element.getKind() + "]");
            if (element.getKind().isField()) {
                System.out.println("+++ " + classElement.toString() + "> " + element.toString());
                VariableElement fieldElement = (VariableElement) element;

                // Add only fields with property patterns (PropertyPattern).
                if (hasPatternAnnotations(fieldElement)) {
                    Property property = new Property(fieldElement.getSimpleName().toString(), getType(fieldElement.asType()), fieldElement);
                    addPatternsFromAnnotations(fieldElement, property);
                    concept.addProperty(property);
                }
            }
        }
    }

    /**
     * Processes abstract classes of language model elements
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processAbstractClass(Concept concept, TypeElement typeElement) {
        //TODO: toto je len docasne pre testovanie a pokial sa ujasni ako to chceme
        //processConcreteClass(concept, typeElement);
    }

    /**
     * Processes interfaces of language model elements
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processInterface(Concept concept, TypeElement typeElement) {
    }

    /**
     * Processes parameters of constructor or factory method in language model.
     *
     * @param concept Language concept.
     * @param notation Notation of language concept.
     * @param paramElement Parameter of constructor or factory method.
     */
    private void processParameter(Concept concept, Notation notation, VariableElement paramElement) {
        Type type = getType(paramElement.asType());

        if (type instanceof OptionalType) {
            OptionalPart optionalPart = processOptionalParameter(concept, paramElement);
            notation.addPart(optionalPart);
        } else {
            // @Before annotation.
            if (paramElement.getAnnotation(Before.class) != null) {
                addTokenParts(notation, paramElement.getAnnotation(Before.class).value());
            }

            String paramName = paramElement.getSimpleName().toString();
            TypeMirror typeMirror = paramElement.asType();
            References references = paramElement.getAnnotation(References.class);
            Token tokenAnnotation = paramElement.getAnnotation(Token.class);
            StringToken stringTokenAnnotation = paramElement.getAnnotation(StringToken.class);
            BindingNotationPart part = null;

            if (references != null) { // @References annotation.
                //TODO: zatial nie je podpora pre polia referencii, treba to vsak doriesit
                type = getSimpleType(typeMirror);
                LocalVariablePart localVariablePart = new LocalVariablePart(paramName, type, paramElement);
                notation.addPart(localVariablePart);

                part = processReferencedConcept(concept, paramElement, references, localVariablePart);
            } else { // Property reference.
                Property property = concept.getProperty(paramName);
                if (property == null) {
                    property = new Property(paramName, getType(typeMirror), null);
                    concept.addProperty(property);
                }

                part = new PropertyReferencePart(property, paramElement);
                notation.addPart(part);
            }

            if (tokenAnnotation != null) {
                part.addPattern(new yajco.model.pattern.impl.Token(tokenAnnotation.value(), tokenAnnotation));
            } else if (stringTokenAnnotation != null) {
                TokenDef tokenDef = createStringTokenDef(stringTokenAnnotation);
                part.addPattern(new yajco.model.pattern.impl.Token(tokenDef.getName(), tokenAnnotation));
            }

            // Add notation part pattern from annotations (NotationPartPattern).
            addPatternsFromAnnotations(paramElement, part);

            // @After annotation.
            if (paramElement.getAnnotation(After.class) != null) {
                addTokenParts(notation, paramElement.getAnnotation(After.class).value());
            }
        }
    }

    /**
     * Processes optional parameters.
     *
     * @param concept Language concept.
     * @param paramElement Parameter of constructor or factory method.
     * @return OptionalPart
     */
    private OptionalPart processOptionalParameter(Concept concept, VariableElement paramElement) {
        OptionalPart optionalPart = new OptionalPart(null);

        // @Before annotation.
        if (paramElement.getAnnotation(Before.class) != null) {
            for (String value : paramElement.getAnnotation(Before.class).value()) {
                optionalPart.addPart(new TokenPart(value));
            }
        }

        String paramName = paramElement.getSimpleName().toString();
        TypeMirror typeMirror = paramElement.asType();
        References references = paramElement.getAnnotation(References.class);
        Token tokenAnnotation = paramElement.getAnnotation(Token.class);
        StringToken stringTokenAnnotation = paramElement.getAnnotation(StringToken.class);
        BindingNotationPart part;

        if (references != null) { // @References annotation.
            Type type;
            List<? extends TypeMirror> types = ((DeclaredType) typeMirror).getTypeArguments();
            if (processingEnv.getTypeUtils().asElement(typeMirror).toString().equals(Optional.class.getName())) {
                type = getSimpleType(types.get(types.size() - 1));
            } else {
                type = getSimpleType(typeMirror);
            }
            LocalVariablePart localVariablePart = new LocalVariablePart(paramName, type, paramElement);
            optionalPart.addPart(localVariablePart);
            part = processReferencedConcept(concept, paramElement, references, localVariablePart);
        } else { // Property reference.
            Property property = concept.getProperty(paramName);
            if (property == null) {
                property = new Property(paramName, getType(typeMirror), null);
                concept.addProperty(property);
            }

            part = new PropertyReferencePart(property, paramElement);
            optionalPart.addPart(part);
        }

        if (tokenAnnotation != null) {
            part.addPattern(new yajco.model.pattern.impl.Token(tokenAnnotation.value(), tokenAnnotation));
        } else if (stringTokenAnnotation != null) {
            TokenDef tokenDef = createStringTokenDef(stringTokenAnnotation);
            part.addPattern(new yajco.model.pattern.impl.Token(tokenDef.getName(), tokenAnnotation));
        }

        // Add notation part pattern from annotations (NotationPartPattern).
        addPatternsFromAnnotations(paramElement, part);

        // @After annotation.
        if (paramElement.getAnnotation(After.class) != null) {
            for (String value : paramElement.getAnnotation(After.class).value()) {
                optionalPart.addPart(new TokenPart(value));
            }
        }

        return optionalPart;
    }

    /**
     * Creates token for string token and adds it to language.
     *
     * @param stringTokenAnnotation StringToken annotation.
     * @return TokenDef
     */
    private TokenDef createStringTokenDef(StringToken stringTokenAnnotation) {
        String regex = formatStringTokenRegex(stringTokenAnnotation);
        TokenDef tokenDef = null;

        for (TokenDef token: language.getTokens()) {
            if (token.getName().contains(DEFAULT_STRING_TOKEN_NAME) && token.getRegexp().equals(regex)) {
                // Use existing string token with the same regex.
                tokenDef = token;
            }
        }

        if (tokenDef == null) {
            tokenDef = new TokenDef(DEFAULT_STRING_TOKEN_NAME + "_" + stringTokenId++, regex);
        }

        // Add string token to language.
        addToListAsSet(language.getTokens(), Collections.singletonList(tokenDef),false);

        return tokenDef;
    }

    /**
     * Formats regex from @StringToken annotation. Regex is used to match strings.
     *
     * @param stringTokenAnnotation StringToken annotation
     * @return Regex for matching strings.
     */
    private String formatStringTokenRegex(StringToken stringTokenAnnotation) {
        String delimiter = stringTokenAnnotation.delimiter();

        if (delimiter.equals("\"") || delimiter.equals("'") || delimiter.equals("\\")) {
            delimiter = "\\" + stringTokenAnnotation.delimiter();
        }

        return delimiter + "(?:\\\\" + delimiter + "|[^" + delimiter + "])*?" + delimiter;
    }

    /**
     * Processes referenced concept.
     *
     * @param concept Language concept.
     * @param paramElement Parameter of constructor or factory method.
     * @param references References annotation.
     * @param part  Local variable notation part.
     * @return Binding notation part.
     */
    private BindingNotationPart processReferencedConcept(Concept concept, VariableElement paramElement, References references, LocalVariablePart part) {
        String paramName = paramElement.getSimpleName().toString();
        try {
            references.value();
        } catch (MirroredTypeException e) {
            TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(e.getTypeMirror());
            Concept referencedConcept = processTypeElement(referencedTypeElement);
            Property property = null;
            if (!references.field().isEmpty()) {
                property = concept.getProperty(references.field());
            }
            if (property == null) {
                property = findReferencedProperty(paramElement, referencedConcept, references.field());
                if (property == null) {
                    String propertyName;
                    if (references.field().isEmpty()) {
                        propertyName = paramName;
                    } else {
                        propertyName = references.field();
                    }
                    property = new Property(propertyName, new yajco.model.type.ReferenceType(referencedConcept, referencedTypeElement), e);
                }
                concept.addProperty(property);
            }
            // If names of notationPart and referenced property are identical, no need to fill property data to References pattern.
            if (property.getName().equals(paramName)) {
                property = null;
            }
            part.addPattern(new yajco.model.pattern.impl.References(referencedConcept, property, references));
        }
        return part;
    }

    /**
     * Finds referenced property.
     *
     * @param paramElement Parameter of constructor or factory method in language model.
     * @param referencedConcept Referenced concept.
     * @param proposedName Referenced field name.
     * @return Referenced property.
     */
    private Property findReferencedProperty(VariableElement paramElement, Concept referencedConcept, String proposedName) {
        Element element = paramElement;

        // Go up on tree until you find class element.
        while (element != null && !element.getKind().isClass()) {
            element = element.getEnclosingElement();
        }

        // Class element found.
        if (element != null) {
            for (Element elem : element.getEnclosedElements()) {
                if (elem.getKind().isField()) {
                    VariableElement fieldElement = (VariableElement) elem;
                    Type fieldType = getType(fieldElement.asType());
                    if (fieldType instanceof yajco.model.type.ReferenceType) {
                        yajco.model.type.ReferenceType referenceType = (yajco.model.type.ReferenceType) fieldType;
                        if (referenceType.getConcept().equals(referencedConcept)) {
                            if (!proposedName.isEmpty() && !fieldElement.getSimpleName().toString().equals(proposedName)) {
                                continue;
                            }
                            return new Property(fieldElement.getSimpleName().toString(), referenceType, fieldElement);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds YAJCo model type of parameter.
     *
     * @param type Element type.
     * @return YAJCo model type of parameter.
     */
    private Type getType(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return new yajco.model.type.ArrayType(getSimpleType(((ArrayType) type).getComponentType()));
        } else if (isSpecifiedClassType(type, List.class)) {
            return getSpecifiedYajcoComponentType(type, ListType.class);
        } else if (isSpecifiedClassType(type, Set.class)) {
            return getSpecifiedYajcoComponentType(type, SetType.class);
        } else if (isSpecifiedClassType(type, Optional.class)) {
            return getSpecifiedYajcoComponentType(type, OptionalType.class);
        } else {
            return getSimpleType(type);
        }
    }

    /**
     * Finds YAJCo component type of element.
     *
     * @param type Type of language model element.
     * @param yajcoType YAJCo model type.
     * @param <T>
     * @return YAJCo component type of element.
     */
    private <T extends ComponentType> T getSpecifiedYajcoComponentType(TypeMirror type, Class<T> yajcoType) {
        if (type.getKind() != TypeKind.DECLARED) {
            throw new GeneratorException("Type " + type.toString() + " is not class or interface");
        }

        System.out.println("************************ " + type.getKind());

        List<? extends TypeMirror> types = ((DeclaredType) type).getTypeArguments();

        if (types.isEmpty()) {
            throw new GeneratorException("Not specified type for " + type.toString() + ", please use generics to specify inner type.");
        } else {
            try {
                Constructor constructor = yajcoType.getConstructor(Type.class);
                if (processingEnv.getTypeUtils().asElement(type).toString().equals(Optional.class.getName())) {
                    // Component type as Optional. For example Optional<String[]>
                    return (T) (constructor.newInstance(getType(types.get(types.size() - 1))));
                }
                return (T) constructor.newInstance(getSimpleType(types.get(types.size() - 1)));
            } catch (NoSuchMethodException ex) {
                throw new GeneratorException("Cannot find constructor for " + yajcoType.getName() + " with only " + Type.class.getName() + " paramater!", ex);
            } catch (Exception ex) {
                throw new GeneratorException("Cannot create new object (" + yajcoType.getName() + ") needed for type " + type.toString(), ex);
            }
        }
    }

    private boolean isSpecifiedClassType(TypeMirror type, Class clazz) {
        TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        return clazz != null && referencedTypeElement != null
                && referencedTypeElement.getQualifiedName().toString().equals(clazz.getName());
    }

    /**
     * Finds YAJCo model type of argument.
     *
     * @param type Type of language model element.
     * @return YAJCo model type of argument.
     */
    private Type getSimpleType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            PrimitiveTypeConst primTypeConst = PrimitiveTypeConst.INTEGER;
            switch (type.getKind()) {
                case BOOLEAN:
                    primTypeConst = yajco.model.type.PrimitiveTypeConst.BOOLEAN;
                    break;
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                    primTypeConst = yajco.model.type.PrimitiveTypeConst.INTEGER;
                    break;
                case FLOAT:
                case DOUBLE:
                    primTypeConst = yajco.model.type.PrimitiveTypeConst.REAL;
                    break;
            }
            return new yajco.model.type.PrimitiveType(primTypeConst, type);
        } else if (type.toString().equals(String.class.getName())) {
            return new yajco.model.type.PrimitiveType(PrimitiveTypeConst.STRING, type);
        } else if (type.toString().equals(Boolean.class.getName())) {
            return new yajco.model.type.PrimitiveType(PrimitiveTypeConst.BOOLEAN, type);
        } else if (type.toString().equals(Byte.class.getName())
                || type.toString().equals(Short.class.getName())
                || type.toString().equals(Integer.class.getName())
                || type.toString().equals(Long.class.getName())) {
            return new yajco.model.type.PrimitiveType(PrimitiveTypeConst.INTEGER, type);
        } else if (type.toString().equals(Float.class.getName()) || type.toString().equals(Double.class.getName())) {
            return new yajco.model.type.PrimitiveType(PrimitiveTypeConst.REAL, type);
        } else if (type.getKind() == TypeKind.DECLARED) {
            TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
            System.out.println("getSimpleType(): referencedTypeElement: " + referencedTypeElement);
            if (referencedTypeElement != null && isKnownClass(referencedTypeElement)) {
                return new yajco.model.type.ReferenceType(processTypeElement(referencedTypeElement), referencedTypeElement);
            }
        }
        throw new GeneratorException("Unsupported simple type " + type + " [" + type.getKind() + "]");
    }

//    private yajco.model.pattern.impl.Range getRange(VariableElement paramElement, TypeMirror type) {
//        yajco.annotation.Range range = paramElement.getAnnotation(yajco.annotation.Range.class);
//        if (type.getKind() == TypeKind.ARRAY) {
//            if (range != null) {
//                return new yajco.model.pattern.impl.Range(range.minOccurs(), range.maxOccurs());
//            }
//        } else {
//            if (range != null) {
//                throw new GeneratorException("@Range should be applied only on array or list  type " + paramElement);
//            }
//        }
//        return null;
//    }

    /**
     * Finds all constructors nad factory methods in language model class.
     *
     * @param classElement Language model class.
     * @return Set of executable elements (constructors and factory methods).
     */
    private Set<ExecutableElement> getConstructorsAndFactoryMethods(TypeElement classElement) {
        Set<ExecutableElement> constructors = new HashSet<>();
        for (Element element : processingEnv.getElementUtils().getAllMembers(classElement)) {
            boolean isConstructor = isConstructor(element);
            boolean isFactoryMethod = isFactoryMethod(element);

            if (isConstructor || isFactoryMethod) {
                constructors.add((ExecutableElement) element);
            }
        }

        return constructors;
    }

    /**
     * Checks if language model element is constructor.
     *
     * @param element Language model element.
     * @return If element is constructor.
     */
    private boolean isConstructor(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR && element.getModifiers().contains(Modifier.PUBLIC)
                && element.getAnnotation(Exclude.class) == null;
    }

    /**
     * Checks if language model element is factory method.
     *
     * @param element Language model element.
     * @return If element is factory method.
     */
    private boolean isFactoryMethod(Element element) {
        return element.getKind() == ElementKind.METHOD && element.getModifiers().contains(Modifier.PUBLIC)
                && element.getAnnotation(FactoryMethod.class) != null && element.getAnnotation(Exclude.class) == null;
    }

    /**
     * Finds if element is already known class.
     *
     * @param element Language model element
     * @return If element is already known class.
     */
    private boolean isKnownClass(Element element) {
        if (element.getKind().isClass() || element.getKind().isInterface()) {
            if (language.getConcept(((TypeElement) element).getQualifiedName().toString()) != null) {
                return true;
            }
            for (Element elem : roundEnv.getRootElements()) {
                if ((elem.getKind().isClass() || elem.getKind().isInterface()) && elem.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the set of direct subtypes of the passes element.
     *
     * @param typeElement type element
     * @return set of direct subtypes of the element.
     */
    private Set<TypeElement> getDirectSubtypes(TypeElement typeElement) {
        Set<TypeElement> subclassElements = new HashSet<>();
        for (Element element : roundEnv.getRootElements()) {
            if (isDirectSubtype(typeElement, element)) {
                subclassElements.add((TypeElement) element);
            }
        }

        return subclassElements;
    }

    /**
     * Returns true if superElement is the direct super class of the element.
     * Otherwise returns false.
     *
     * @param superElement supertype
     * @param element element
     * @return true if superElement is the direct super class of the element.
     */
    private boolean isDirectSubtype(TypeElement superElement, Element element) {
        Exclude excludeAnnotation = element.getAnnotation(Exclude.class);
        int excludeAnnotationsLength = 0;
        try {
            if (excludeAnnotation != null) {
                excludeAnnotation.value();
            }
        } catch (MirroredTypesException e) {
            excludeAnnotationsLength = e.getTypeMirrors().size();
        }
        if (excludeAnnotation == null || excludeAnnotationsLength > 0) {
            if (element.getKind().isClass() || element.getKind().isInterface()) {
                TypeMirror superType = superElement.asType();
                TypeElement typeElement = (TypeElement) element;
                // Test superclass.
                if (processingEnv.getTypeUtils().isSameType(typeElement.getSuperclass(), superType)) {
                    return true;
                }
                // Test interfaces.
                for (TypeMirror type : typeElement.getInterfaces()) {
                    if (processingEnv.getTypeUtils().isSameType(type, superType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addTokenParts(Notation notation, String[] values) {
        for (String value : values) {
            notation.addPart(new TokenPart(value));
        }
    }

    //TOTO je klucove pre otvorenost procesora, kopiruje vzor uvedeny v anotacii do modelu
    //TODO - navrhujem doplnit kontrolu podla typu vzoru
    /**
     * Finds if language model elements annotation is annotated with @MapsTo annotations.
     *
     * @param element Language model element.
     * @param <T>
     * @return If elements annotation contains annotated with @MapsTo annotations.
     */
    private <T extends Pattern> boolean hasPatternAnnotations(Element element) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            Element annotationElement = processingEnv.getTypeUtils().asElement(am.getAnnotationType());
            MapsTo mapsTo = annotationElement.getAnnotation(MapsTo.class);
            if (mapsTo != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds patterns from annotations to language.
     *
     * @param element Language model element.
     * @param patternSupport Language concept.
     * @param <T>
     */
    private <T extends Pattern> void addPatternsFromAnnotations(Element element, PatternSupport<T> patternSupport) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            Element annotationElement = processingEnv.getTypeUtils().asElement(am.getAnnotationType());
            MapsTo mapsTo = annotationElement.getAnnotation(MapsTo.class);
            if (mapsTo != null) {
                System.out.println("Processing annotation pattern: " + am);
                String mapsToClass = mapsTo.value();
                //String mapsToClass = null;
//                try {
//                    mapsTo.value();
//                } catch (MirroredTypeException e) {
//                    mapsToClass = e.getTypeMirror().toString();
//                }

                patternSupport.addPattern((T) createObjectFromAnnotation(mapsToClass, am));
            }
        }
    }

    /**
     * Creates object from annotation.
     *
     * @param mapsToClass Class that reflects annotation function.
     * @param am Annotation
     * @return Pattern
     */
    private Pattern createObjectFromAnnotation(String mapsToClass, AnnotationMirror am) {
        try {
            Class<? extends Pattern> clazz = (Class<? extends Pattern>) Class.forName(mapsToClass);
            Pattern pattern = clazz.newInstance();
            // Copy from annotation into created object, according to the same names of annotation property and field.
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(am);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                String name = entry.getKey().getSimpleName().toString();
                // For conversion see javax.​lang.​model.​element.​AnnotationValue
                // TODO: Does not convert: arrays, annotation and classes
                Object value = entry.getValue().getValue();
                System.out.println("  " + name + " = " + value);
                if (value instanceof VariableElement) {  // Enum value
                    VariableElement enumField = (VariableElement) value;
                    value = Enum.valueOf((Class<? extends Enum>) Class.forName(enumField.asType().toString()), enumField.getSimpleName().toString());
                }
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                System.out.println(" >" + name + " = " + field.get(pattern));
                field.set(pattern, value);
                System.out.println(" >>" + name + " = " + field.get(pattern));
            }
            return pattern;
        } catch (Exception e) {
            // TODO: upravit vypis
            throw new GeneratorException("Cannot instantiate class for @MapsTo, class " + mapsToClass, e);
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


    /**
     * Processes direct subclasses of language model element.
     *
     * @param typeElement Language model element.
     * @param concept Language concept representing language model element.
     */
    private void processDirectSubclasses(TypeElement typeElement, Concept concept) {
        System.out.print("DirectSubtypes of " + typeElement.getSimpleName() + " are: ");
        for (TypeElement subtypeElement : getDirectSubtypes(typeElement)) {
            System.out.print(subtypeElement.getSimpleName() + "  ");
            processTypeElement(subtypeElement, concept);
        }
        System.out.println();
    }

    /**
     * Adds new items to original list according to overwrite flag.
     *
     * @param originalList Original list.
     * @param newItems List of new items.
     * @param overwrite Overwrite flag.
     * @param <T>
     */
    private <T> void addToListAsSet(List<T> originalList, List<T> newItems, boolean overwrite) {
        for (T item : newItems) {
            if (originalList.contains(item)) {
                if (overwrite) {
                    // dolezite je to, aby item.equals fungoval spravne
                    originalList.remove(item); // odstrani povodny
                    originalList.add(item); // vlozi novy
                }
            } else {
                originalList.add(item);
            }
        }
    }

}
