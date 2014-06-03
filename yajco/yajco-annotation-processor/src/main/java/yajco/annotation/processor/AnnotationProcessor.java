package yajco.annotation.processor;

import com.sun.tools.javac.code.Type.ClassType;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yajco.annotation.*;
import yajco.annotation.config.*;
import yajco.annotation.reference.*;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.CompilerGenerator;
import yajco.generator.util.ServiceFinder;
import yajco.model.*;
import yajco.model.pattern.*;
import yajco.model.pattern.impl.Factory;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.SetType;
import yajco.model.type.Type;
import yajco.model.utilities.XMLLanguageFormatHelper;
import yajco.printer.Printer;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({"yajco.annotation.config.Parser", "yajco.annotation.Exclude"})
//TODO - anotacia @Optional nie je funkcna, malo by generovat vyskytu @Optional generovat vsetky moznosti v pripade
//Ak pocet pouziti @Optional je x, potom pocet moznosti je: (x nad 0)+(x nad 1)+...+(x nad x-1)+(x nad x)
// (x nad y) = ( x! / ( (x-y)! * y! ) )
// pre x=1 -> 2, x=2 -> 4, x=3 -> 8, x=4 -> 16, x=5 -> 32
//Mozno by tato anotacia mala byt uplne zrusena
//
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
    private Set<String> excludes = new HashSet<String>();
    /**
     * Builded language.
     */
    private Language language/*
             * = new Language()
             */;

    private Set<Concept> conceptsToProcess = new HashSet<Concept>(); // set for concepts imported from previous JARs and needed for full analysis

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Iterator<? extends TypeElement> iterator = annotations.iterator();

        while (iterator.hasNext()) {
            TypeElement typeElement = iterator.next();
            if (typeElement.getQualifiedName().contentEquals(yajco.annotation.Exclude.class.getName())) {
                iterator.remove(); // leave only Parser annotation for later processing
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(yajco.annotation.Exclude.class)) {
            Exclude exclude = element.getAnnotation(yajco.annotation.Exclude.class);
            try {
                exclude.value(); // sposob ako sa dostat k nazvom Class, prv potrebne vyvolat vynimku
            } catch (MirroredTypesException e) {
                for (TypeMirror type : e.getTypeMirrors()) {
                    excludes.add(type.toString());
                }
            }
        }

        for (String clazz : excludes) {
            System.out.println("====exclude---> " + clazz);
        }

        properties = new Properties();
        // disable class generating - annotation processor works on classes, don't generate new ones
        properties.setProperty("yajco.generator.classgen.ClassGenerator", "false");

        try {
            InputStream inputStream = getClass().getResourceAsStream(PROPERTY_SETTINGS_FILE);
            properties.load(inputStream);
            logger.debug("Loaded config from file: {}", properties);
        } catch (Exception e) {
            // LOG it but don't do anything, it is not an error
            logger.info("Cannot find or load {} file in classpath. Will use only @Parser options.", PROPERTY_SETTINGS_FILE);
            logger.debug("Loading config file: {}", e.getLocalizedMessage());
            //throw new GeneratorException("Cannot load " + PROPERTY_SETTINGS_FILE, e);
        }

        if ("false".equalsIgnoreCase(properties.getProperty("yajco"))) {
            logger.info("Property 'yajco' set to false - terminating YAJCo tool !");
            return false;
        }

        this.roundEnv = roundEnv;
        try {
            if (annotations.size() == 1) {
                //logger.info("YAJCo parser generator {}", VERSION);
                System.out.println("YAJCo parser generator " + VERSION);

                //There is only one supported annotation type @Parser
                TypeElement annotationType = annotations.iterator().next();
                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationType);

                //find directory for saving generated files
                FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "temp.java");
                //targetDirectory = new File(fo.toUri()).getParentFile();

                //Parser generator works only with one @Parser annotation
                if (elements.size() > 1) {
                    //TODO: vypisat co vsetko anotoval aby to vedel odstranit
                    throw new GeneratorException("There should be only one annotation @Parser in the model.");
                }
                //Take the first annotation (the only one)
                Element parserAnnotationElement = elements.iterator().next();
                Parser parserAnnotation = parserAnnotationElement.getAnnotation(Parser.class);

                // Extract options from @Parser anntotation
                for (Option option : parserAnnotation.options()) {
                    properties.setProperty(option.name(), option.value());
                }

                if (!parserAnnotation.className().isEmpty()) {
                    properties.setProperty("yajco.className", parserAnnotation.className());
                }
                if (!parserAnnotation.mainNode().isEmpty()) {
                    properties.setProperty("yajco.mainNode", parserAnnotation.mainNode());
                }

                //Extract the main element, package or type can be annotated with @Parser
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
                    //TODO: vypisat co bolo anotovane
                    throw new GeneratorException("Annotation @Parser should annotate only with package, class, interface or enum.");
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
                language = new Language(mainElement);
                //add main package name == language name
                String languageName = mainElementName.substring(0, mainElementName.lastIndexOf('.'));
                System.out.println("---- mainElementName: " + mainElementName + " == languageName: " + languageName);
                if (!languageName.isEmpty()) {
                    language.setName(languageName);
                }
                //add language concepts from included JARs
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

                //Start processing with the main element
                System.out.println(" ? mainElement ? : " + mainElement);
                processTypeElement(mainElement);

                // add tokens and skips into language
                List<yajco.model.TokenDef> tokens = new ArrayList<yajco.model.TokenDef>();
                List<SkipDef> skips = new ArrayList<SkipDef>();
                for (yajco.annotation.config.TokenDef tokenDef : parserAnnotation.tokens()) {
                    tokens.add(new yajco.model.TokenDef(tokenDef.name(), tokenDef.regexp(), tokenDef));
                }
                for (Skip skip : parserAnnotation.skips()) {
                    skips.add(new SkipDef(skip.value(), skip));
                }
                // add default white space for skips if empty
                if (skips.isEmpty()) {
                    skips.add(new SkipDef("\\s"));
                }
                addToListAsSet(language.getSkips(), skips, true);
                addToListAsSet(language.getTokens(), tokens, true);
                language.setSettings(LanguageSetting.convertToLanguageSetting(properties));

                //Print recognized language to output
                Printer printer = new Printer();
                System.out.println("--------------------------------------------------------------------------------------------------------");
                printer.printLanguage(new PrintWriter(System.out), language);
                System.out.println("--------------------------------------------------------------------------------------------------------");

                //generate compiler
                if (!("false".equalsIgnoreCase(properties.getProperty("yajco.generateParser")))) {
                    String parserClassName = parserAnnotation.className();
                    generateCompiler(parserClassName);
                }

                // generates all new files
//                GeneratorHelper generatorHelper = new GeneratorHelper(language, targetDirectory, properties);
//                if (properties.containsKey("generateTools") && "true".equals(properties.getProperty("generateTools"))) {
//                    generatorHelper.generateAllExceptModelClassFiles();
//                }
                //generate all tools
                Set<FilesGenerator> tools = ServiceFinder.findFilesGenerators(properties);
                for (FilesGenerator filesGenerator : tools) {
                    filesGenerator.generateFiles(language, processingEnv.getFiler(), properties);
                }

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

    private Concept processTypeElement(TypeElement typeElement) {
        return processTypeElement(typeElement, null);
    }

    private Concept processTypeElement(TypeElement typeElement, Concept superConcept) {
        String name = typeElement.getQualifiedName().toString();
        System.out.println("---->>> Name: " + name + " [kind:" + typeElement.getKind() + "]");
        if (excludes.contains(name)) {
            //System.out.println("---->> NACHADZA SA V EXCLUDE TAK HO RUSIM !!!!");
            return null;
        }
        if (language.getName() != null && !language.getName().isEmpty() && name.startsWith(language.getName())) {
            name = name.substring(language.getName().length() + 1); // +1 because of dot after package name '.'
        }
        Concept concept = language.getConcept(name);
        if (concept != null) { //Already processed
            if (superConcept != null) { //Set parent
                concept.setParent(superConcept);
            }
            //TODO:toto som tu doplnil len docasne na vyskusanie pre podporu kompozicie jazykov, treba to cele prehodnotit, lebo sa to nachadza aj na konci metody
            //processDirectSubclasses(typeElement, concept);
            if (conceptsToProcess.contains(concept)) {
                conceptsToProcess.remove(concept);
            } else {
                return concept;
            }
        } else {
            //Create concept
            concept = new Concept(name, typeElement);
            concept.setParent(superConcept); //Set parent
            language.addConcept(concept);
        }

        if (typeElement.getKind() == ElementKind.ENUM) { //Enum type
            processEnum(concept, typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) { //Class
            System.out.println(" modifiers: " + typeElement.getModifiers());
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) { //Abstract class
                processAbstractClass(concept, typeElement);
            } else {  //Concrete class
                processConcreteClass(concept, typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.INTERFACE) { //Interface
            processInterface(concept, typeElement);
        } else {
            throw new GeneratorException("Not supported type in model '" + typeElement + "'");
        }

        //Add concept pattern from annotations (ConceptPattern)
        addPatternsFromAnnotations(typeElement, concept, ConceptPattern.class);
        processDirectSubclasses(typeElement, concept);
        return concept;
    }

    private void processEnum(Concept concept, TypeElement typeElement) {
        concept.addPattern(new yajco.model.pattern.impl.Enum());
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.ENUM_CONSTANT) {
                continue; //skip non enum constants
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

    private void processConcreteClass(Concept concept, TypeElement classElement) {
        //Abstract syntax
        System.out.println("starting ProcessConcreteClass method");
        for (Element element : classElement.getEnclosedElements()) {
            System.out.println("- enclosedElement: " + element.getSimpleName().toString() + "[" + element.getKind() + "]");
            if (element.getKind().isField()) {
                System.out.println("+++ " + classElement.toString() + "> " + element.toString());
                VariableElement fieldElement = (VariableElement) element;

                //Add only fields with property patterns (PropertyPattern)
                if (hasPatternAnnotations(fieldElement)) {
                    Property property = new Property(fieldElement.getSimpleName().toString(), getType(fieldElement.asType()), fieldElement);
                    addPatternsFromAnnotations(fieldElement, property, PropertyPattern.class);
                    concept.addProperty(property);
                }
            }
        }

        //Concrete syntax
        Set<ExecutableElement> constructors = getConstructorsAndFactoryMethods(classElement);
        for (ExecutableElement constructor : constructors) {
            Notation notation = new Notation(constructor);

            //Before annotation
            if (constructor.getAnnotation(Before.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(Before.class).value());
            }

            for (VariableElement paramElement : constructor.getParameters()) {
                processParameter(concept, notation, paramElement);
            }

            //After annotation
            if (constructor.getAnnotation(After.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(After.class).value());
            }

            concept.addNotation(notation);
            if (constructor.getKind() == ElementKind.METHOD) {
                notation.addPattern(new Factory(constructor.getSimpleName().toString()));
            }

            //TODO: odstranit pri prenesesni @Operator na triedu
            //Add concept pattern from annotations (Type)
            addPatternsFromAnnotations(constructor, concept, ConceptPattern.class);
        }
    }

    private void processAbstractClass(Concept concept, TypeElement typeElement) {
        //TODO: toto je len docasne pre testovanie a pokial sa ujasni ako to chceme
        //processConcreteClass(concept, typeElement);
    }

    private void processInterface(Concept concept, TypeElement typeElement) {
    }

    private void processParameter(Concept concept, Notation notation, VariableElement paramElement) {
        //Before annotation
        if (paramElement.getAnnotation(Before.class) != null) {
            addTokenParts(notation, paramElement.getAnnotation(Before.class).value());
        }

        String paramName = paramElement.getSimpleName().toString();
        TypeMirror typeMirror = paramElement.asType();
        References references = paramElement.getAnnotation(References.class);
        Token tokenAnnotation = paramElement.getAnnotation(Token.class);
        BindingNotationPart part;

        if (references != null) { //References annotation
            //TODO: zatial nie je podpora pre polia referencii, treba to vsak doriesit
            Type type = getSimpleType(typeMirror);
            part = new LocalVariablePart(paramName, type, paramElement);
            notation.addPart(part);

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
                //if names of notationPart and referenced property are identical, no need to fill property data to References pattern
                if (property.getName().equals(paramName)) {
                    property = null;
                }
                part.addPattern(new yajco.model.pattern.impl.References(referencedConcept, property, references));
            }
        } else { //Property reference
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
        }

        //Add notation part pattern from annotations (NotationPartPattern)
        addPatternsFromAnnotations(paramElement, part, NotationPartPattern.class);

        //After annotation
        if (paramElement.getAnnotation(After.class) != null) {
            addTokenParts(notation, paramElement.getAnnotation(After.class).value());
        }
    }

    private Property findReferencedProperty(VariableElement paramElement, Concept referencedConcept, String proposedName) {
        Element element = paramElement;
        //Go up on tree until you find class element
        while (element != null && !element.getKind().isClass()) {
            element = element.getEnclosingElement();
        }
        // Class element found
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

    private Type getType(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return new yajco.model.type.ArrayType(getSimpleType(((ArrayType) type).getComponentType()));
        } else if (isSpecifiedClassType(type, List.class)) {
            return getSpecifiedYajcoComponentType(type, ListType.class);
        } else if (isSpecifiedClassType(type, Set.class)) {
            return getSpecifiedYajcoComponentType(type, SetType.class);
        } else {
            return getSimpleType(type);
        }
    }

    private <T extends ComponentType> T getSpecifiedYajcoComponentType(TypeMirror type, Class<T> yajcoType) {
        if (type.getKind() != TypeKind.DECLARED) {
            throw new GeneratorException("Type " + type.toString() + " is not class or interface");
        }
        com.sun.tools.javac.util.List<com.sun.tools.javac.code.Type> types = ((ClassType) type).getTypeArguments();
        if (types.isEmpty()) {
            throw new GeneratorException("Not specified type for " + type.toString() + ", please use generics to specify inner type.");
        } else {
            try {
                Constructor constructor = yajcoType.getConstructor(Type.class);
                return (T) constructor.newInstance(getSimpleType(types.last()));
            } catch (NoSuchMethodException ex) {
                throw new GeneratorException("Cannot find constructor for " + yajcoType.getName() + " with only " + Type.class.getName() + " paramater!", ex);
            } catch (Exception ex) {
                throw new GeneratorException("Cannot create new object (" + yajcoType.getName() + ") needed for type " + type.toString(), ex);
            }
        }
    }

    private boolean isSpecifiedClassType(TypeMirror type, Class clazz) {
        TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        if (clazz != null && referencedTypeElement != null
                && referencedTypeElement.getQualifiedName().toString().equals(clazz.getName())) {
            return true;
        }
        return false;
    }

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
    private Set<ExecutableElement> getConstructorsAndFactoryMethods(TypeElement classElement) {
        Set<ExecutableElement> constructors = new HashSet<ExecutableElement>();
        Elements elementUtils = processingEnv.getElementUtils();
        for (Element element : elementUtils.getAllMembers(classElement)) {
            boolean isConstructor = element.getKind() == ElementKind.CONSTRUCTOR && element.getModifiers().contains(Modifier.PUBLIC) && element.getAnnotation(Exclude.class) == null;
            boolean isFactoryMethod = element.getKind() == ElementKind.METHOD && element.getModifiers().contains(Modifier.PUBLIC) && element.getAnnotation(FactoryMethod.class) != null && element.getAnnotation(Exclude.class) == null;

            if (isConstructor || isFactoryMethod) {
                constructors.add((ExecutableElement) element);
            }
        }

        return constructors;
    }

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
        Set<TypeElement> subclassElements = new HashSet<TypeElement>();
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
     * @param supertype supertype
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
                //Test superclass
                if (processingEnv.getTypeUtils().isSameType(typeElement.getSuperclass(), superType)) {
                    return true;
                }
                //Test interfaces
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
        for (int i = 0; i < values.length; i++) {
            notation.addPart(new TokenPart(values[i]));
        }
    }

    //TOTO je klucove pre otvorenost procesora, kopiruje vzor uvedeny v anotacii do modelu
    //TODO - navrhujem doplnit kontrolu podla typu vzoru
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

    private <T extends Pattern> void addPatternsFromAnnotations(Element element, PatternSupport<T> patternSupport, Class<T> patternClass) {
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

    private Pattern createObjectFromAnnotation(String mapsToClass, AnnotationMirror am) {
        try {
            Class<? extends Pattern> clazz = (Class<? extends Pattern>) Class.forName(mapsToClass);
            Pattern pattern = clazz.newInstance();
            //Copy from annotation into created object, according to the same names of annotation property and field
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(am);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                String name = entry.getKey().getSimpleName().toString();
                //For conversion see javax.​lang.​model.​element.​AnnotationValue
                //TODO: Does not convert: arrays, annotation and classes
                Object value = entry.getValue().getValue();
                System.out.println("  " + name + " = " + value);
                if (value instanceof VariableElement) {  //Enum value
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
            //TODO: upravit vypis
            throw new GeneratorException("Cannot instatite class for @Maps, class " + mapsToClass, e);
        }
    }

    private void generateCompiler(String parserClassName) throws GeneratorException {
        CompilerGenerator compilerGenerator = ServiceFinder.findCompilerGenerator();

        if (compilerGenerator != null) {
            if (parserClassName != null && !parserClassName.isEmpty()) {
                compilerGenerator.generateFiles(language, processingEnv.getFiler(), properties, parserClassName);
            } else {
                compilerGenerator.generateFiles(language, processingEnv.getFiler(), properties);
            }
        } else {
            throw new GeneratorException("No compiler generator in class path. Include service implementation of " + CompilerGenerator.class.getName() + " in your classpath. (see java.util.ServiceLoader javadoc for details)");
        }
    }

    private void processDirectSubclasses(TypeElement typeElement, Concept concept) {
        //Process direct subclasses
        System.out.print("DirectSubtypes of " + typeElement.getSimpleName() + " are: ");
        for (TypeElement subtypeElement : getDirectSubtypes(typeElement)) {
            System.out.print(subtypeElement.getSimpleName() + "  ");
            processTypeElement(subtypeElement, concept);
        }
        System.out.println();
    }

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
