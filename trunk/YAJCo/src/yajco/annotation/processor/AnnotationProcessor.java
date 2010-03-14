package yajco.annotation.processor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type.ClassType;
import yajco.model.type.Type;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic.Kind;
import tuke.pargen.*;
import tuke.pargen.annotation.*;
import tuke.pargen.annotation.config.*;
import tuke.pargen.annotation.reference.*;
import yajco.model.*;
import yajco.model.pattern.*;
import yajco.printer.Printer;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("tuke.pargen.annotation.config.Parser")
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
    /** Version string. */
    private static final String VERSION = "0.3";

    /** Stored round environment. */
    private RoundEnvironment roundEnv;

    /**
     * Language.
     */
    private Language language = new Language();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.roundEnv = roundEnv;
        try {
            if (annotations.size() == 1) {
                System.out.println("YAJCo parser generator " + VERSION);

                //There is only one supported annotation type @Parser
                TypeElement annotationType = annotations.iterator().next();
                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationType);

                //Parser generator works only with one @Parser annotation
                if (elements.size() > 1) {
                    //TODO: vypisat co vsetko anotoval aby to vedel odstranit
                    throw new GeneratorException("There should be only one annotation @Parser in the model.");
                }
                //Take the first annotation (the only one)
                Element parserAnnotationElement = elements.iterator().next();
                Parser parserAnnotation = parserAnnotationElement.getAnnotation(Parser.class);

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

                //Start processing with the main element
                processTypeElement(mainElement);

                Printer printer = new Printer();
                System.out.println("--------------------------------------------------------------------------------------------------------");
                printer.printLanguage(new PrintWriter(System.out), language);
                System.out.println("--------------------------------------------------------------------------------------------------------");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
        }
        return false;
    }

    private Concept processTypeElement(TypeElement typeElement) {
        return processTypeElement(typeElement, null);
    }

    private Concept processTypeElement(TypeElement typeElement, Concept superConcept) {
        String name = typeElement.getSimpleName().toString();
        Concept concept = language.getConcept(name);
        if (concept != null) { //Already processed
            if (superConcept != null) { //Set parent
                concept.setParent(superConcept);
            }
            return concept;
        }

        //Create concept
        concept = new Concept(name);
        concept.setParent(superConcept); //Set parent
        language.addConcept(concept);

        if (typeElement.getKind() == ElementKind.ENUM) { //Enum type
            processEnum(concept, typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) { //Class
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

        //Process direct subclasses
        for (TypeElement subtypeElement : getDirectSubtypes(typeElement)) {
            processTypeElement(subtypeElement, concept);
        }
        return concept;
    }

    private void processEnum(Concept concept, TypeElement typeElement) {
        concept.addPattern(new yajco.model.pattern.impl.Enum());
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                NotationPart[] parts = {new TokenPart(element.getSimpleName().toString())};
                concept.addNotation(new Notation(parts));
            }
        }
    }

    private void processConcreteClass(Concept concept, TypeElement classElement) {
        //Abstract syntax
        for (Element element : classElement.getEnclosedElements()) {
            if (element.getKind().isField()) {
                System.out.println("+++ "+classElement.toString()+ "> "+element.toString());
                VariableElement fieldElement = (VariableElement) element;

                //Add only fields with property patterns (PropertyPattern)
                if (hasPatternAnnotations(fieldElement)) {
                    Property property = new Property(fieldElement.getSimpleName().toString(), getType(fieldElement.asType()));
                    addPatternsFromAnnotations(fieldElement, property, PropertyPattern.class);
                    concept.addProperty(property);
                }
            }
        }

        //Concrete syntax
        Set<ExecutableElement> constructors = getConstructorsAndFactoryMethods(classElement);
        for (ExecutableElement constructor : constructors) {
            Notation notation = new Notation();

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

            //TODO: odstranit pri prenesesni @Operator na triedu
            //Add concept pattern from annotations (Type)
            addPatternsFromAnnotations(constructor, concept, ConceptPattern.class);
        }
    }

    private void processAbstractClass(Concept concept, TypeElement typeElement) {
        //TODO: toto je len docasne pre testovanie a pokial sa ujasni ako to chceme
        processConcreteClass(concept, typeElement);
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
        BindingNotationPart part;

        if (references != null) { //References annotation
            //TODO: zatial nie je podpora pre polia referencii, treba to vsak doriesit
            Type type = getSimpleType(typeMirror);
            part = new LocalVariablePart(paramName, type);
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
                    property = findReferencedProperty(paramElement, referencedConcept);
                    if (property == null) {
                        String propertyName;
                        if (references.field().isEmpty()) {
                            propertyName = paramName;
                        } else {
                            propertyName = references.field();
                        }
                        property = new Property(propertyName,new yajco.model.type.ReferenceType(referencedConcept));
                    }
                    concept.addProperty(property);
                }
                //if names of notationPart and referenced property are identical, no need to fill property data to References pattern
                if (property.getName().toString().equals(paramName)) {
                    property = null;
                }
                part.addPattern(new yajco.model.pattern.impl.References(referencedConcept, property));
            }
        } else { //Property reference
            Property property = concept.getProperty(paramName);
            if (property == null) {
                property = new Property(paramName, getType(typeMirror), null);
                concept.addProperty(property);
            }

            part = new PropertyReferencePart(property);
            notation.addPart(part);
        }

        //Add notation part pattern from annotations (NotationPartPattern)
        addPatternsFromAnnotations(paramElement, part, NotationPartPattern.class);

        //After annotation
        if (paramElement.getAnnotation(After.class) != null) {
            addTokenParts(notation, paramElement.getAnnotation(After.class).value());
        }
    }

    private Property findReferencedProperty(VariableElement paramElement, Concept referencedConcept) {
        Element element = paramElement;
        //Go up on tree until you find class element
        while (!element.getKind().isClass() && element != null) {
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
                            return new Property(fieldElement.getSimpleName().toString(), referenceType);
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
        } else if (isSupportedCollection(type)) {
            return new yajco.model.type.ArrayType(getSimpleType(((ClassType) type).getTypeArguments().last()));
        } else {
            return getSimpleType(type);
        }
    }

    private boolean isSupportedCollection(TypeMirror type) {
        TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        if (referencedTypeElement != null && referencedTypeElement.getQualifiedName().toString().equals(List.class.getName())) {
            return true;
        }
        return false;
    }

    private Type getSimpleType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            switch (type.getKind()) {
                case BOOLEAN:
                    return yajco.model.type.PrimitiveType.BOOLEAN;
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                    return yajco.model.type.PrimitiveType.INTEGER;
                case FLOAT:
                case DOUBLE:
                    return yajco.model.type.PrimitiveType.REAL;
            }
        } else if (type.toString().equals(String.class.getName())) {
            return yajco.model.type.PrimitiveType.STRING;
        } else if (type.getKind() == TypeKind.DECLARED) {
            TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
            if (referencedTypeElement != null && isKnownClass(referencedTypeElement)) {
                return new yajco.model.type.ReferenceType(processTypeElement(referencedTypeElement));
            }
        }
        throw new GeneratorException("Unsupported simple type " + type);
    }

//    private yajco.model.pattern.impl.Range getRange(VariableElement paramElement, TypeMirror type) {
//        tuke.pargen.annotation.Range range = paramElement.getAnnotation(tuke.pargen.annotation.Range.class);
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
            boolean isFactoryMethod = element.getKind() == ElementKind.METHOD && element.getModifiers().contains(Modifier.PUBLIC) && element.getAnnotation(FactoryMethod.class) != null;

            if (isConstructor || isFactoryMethod) {
                constructors.add((ExecutableElement) element);
            }
        }

        return constructors;
    }

    private boolean isKnownClass(Element element) {
        if (element.getKind().isClass() || element.getKind().isInterface()) {
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
     * @param supertype supertype
     * @param element   element
     * @return true if superElement is the direct super class of the element.
     */
    private boolean isDirectSubtype(TypeElement superElement, Element element) {
        if (element.getAnnotation(Exclude.class) == null) {
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
                String mapsToClass = null;
                try {
                    mapsTo.value();
                } catch (MirroredTypeException e) {
                    mapsToClass = e.getTypeMirror().toString();
                }

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
                field.set(pattern, value);
            }
            return pattern;
        } catch (Exception e) {
            //TODO: upravit vypis
            throw new GeneratorException("Cannot instatite class for @Maps, class " + mapsToClass, e);
        }
    }
}
