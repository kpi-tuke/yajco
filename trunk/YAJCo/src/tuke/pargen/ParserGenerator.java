package tuke.pargen;

import javax.annotation.processing.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.javacc.parser.Main;
import tuke.pargen.annotation.*;
import tuke.pargen.annotation.config.*;
import tuke.pargen.javacc.Utilities;
import tuke.pargen.javacc.model.*;
import tuke.pargen.model.ModelBuilder;
import tuke.pargen.patterns.PatternMatcher;

//TODO: Pripravit to na post parsing action,
//Napr ked sa ma vytvorit uzol stromu
//respektive ked sa ukonci spracovanie aby som mohol vykonat nejake akcie na ASG
//TODO: Nefunguje Range (polia) na primitivnych typoch
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("tuke.pargen.annotation.config.Parser")
public class ParserGenerator extends AbstractProcessor {
    /** Version string. */
    private static final String VERSION = "0.2";

    /** Supported conversions. */
    private static final Conversions stringConversions = new Conversions();

    /**
     * Element annotated with Parser annotation (kind of the element is package).
     */
    private Element parserAnnotationElement;

    /**
     * Main element (kind of the element is class/type) of a start symbol defined in parser definition (mainNode attribute).
     */
    private TypeElement mainElement;

    /**
     * Map from nonterminal name to production (rhs).
     */
    private Map<String, Production> productions = new HashMap<String, Production>();

    /**
     * Map of operators.
     */
    //TODO: Nestaci si odpamatat
    private Map<TypeElement, Set<Integer>> operatorElements = new HashMap<TypeElement, Set<Integer>>();

    /**
     * Set of already processed elements.
     */
    private Set<TypeElement> processedElements = new HashSet<TypeElement>();

    /**
     * Velocity engine
     */
    private VelocityEngine velocityEngine = new VelocityEngine();

    private RoundEnvironment roundEnv;

    private Map<String, String> definedTokens = new LinkedHashMap<String, String>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.roundEnv = roundEnv;
        try {
            if (annotations.size() == 1) {
                System.out.println("YAJCo parser generator " + VERSION);

                TypeElement annotationType = annotations.iterator().next();
                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationType);
                //Parser generator works only with one @Parser annotation
                if (elements.size() > 1) {
                    throw new GeneratorException("There should be only one annotation @Parser in the model.");
                }
                parserAnnotationElement = elements.iterator().next();
                Parser parserAnnotation = parserAnnotationElement.getAnnotation(Parser.class);

                ElementKind parserAnnotationElemKind = parserAnnotationElement.getKind();
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
                } else if (parserAnnotationElemKind == ElementKind.CLASS || parserAnnotationElemKind == ElementKind.INTERFACE) {
                    mainElement = (TypeElement) parserAnnotationElement;
                    mainElementName = mainElement.asType().toString();
                } else {
                    throw new GeneratorException("Annotation @Parser can be used only with package, class or interface!");
                }

                // vytvorit zoznam vsetkych definovanych tokenov
                TokenDef[] allTokens = parserAnnotation.tokens();
                for (TokenDef t : allTokens) {
                    definedTokens.put(t.name(), t.regexp());
                }

                if (mainElement == null) {
                    throw new GeneratorException("Cannot find main node '" + parserAnnotation.mainNode() + "'");
                }

                ModelBuilder modelBuilder = new ModelBuilder(roundEnv, processingEnv, mainElement);
                PatternMatcher patternMatcher = PatternMatcher.getInstance();
                patternMatcher.setProcessingEnv(processingEnv);

                Throwable patternException = null;
                try {
                    patternMatcher.testModel(modelBuilder.getModelElements());
                } catch (Throwable e) {
                    patternException = e;
                }

                //Start processing with main element
                Throwable processingException = null;
                try {
                    processTypeElement(mainElement, 0);
                } catch (Throwable e) {
                    processingException = e;
                }

                if (patternException != null || processingException != null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("\n");

                    if (patternException != null) {
                        builder.append(patternException.getMessage());
                        builder.append("\n\n");
                    }
                    if (processingException != null) {
                        builder.append(processingException.getMessage());
                    }
                    throw new GeneratorException(builder.toString(), processingException);
                }

                //Generate and save content
                String parserQualifiedClassName = parserAnnotation.className();
                String parserClassName = parserQualifiedClassName.substring(parserQualifiedClassName.lastIndexOf('.') + 1);
                String parserPackageName = parserQualifiedClassName.substring(0, parserQualifiedClassName.lastIndexOf('.'));
                String parserJavaCCPackageName = parserPackageName + ".javacc";

                //Generate javacc grammar file
                FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserJavaCCPackageName, "grammar.jj");
                Writer writer = new BufferedWriter(fo.openWriter());
                Model model = new Model(parserJavaCCPackageName, parserClassName != null ? parserClassName.trim() : "",
                        parserAnnotation.skips(), definedTokens, parserAnnotation.options(),
                        productions.get(getNonterminal(mainElement, 0)), productions.values().toArray(new Production[]{}));
                model.generate(writer);
                writer.close();
                URI grammarURI = fo.toUri();

                //Generate ebnf grammar file
                fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserJavaCCPackageName, "grammar.ebnf");
                writer = fo.openWriter();
                writer.write(model.toString());
                writer.close();

                //Generate exception class
                fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, "ParseException.java");
                writer = fo.openWriter();
                writer.write(generateExceptionClass(parserPackageName));
                writer.close();

                //Generate parser class
                fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserPackageName, parserClassName + ".java");
                writer = fo.openWriter();
                writer.write(generateParserClass(parserClassName, parserPackageName, parserJavaCCPackageName, mainElementName));
                writer.close();

                //Test token definitions
                testTokensDefinitions(parserAnnotation.skips());

                //Generate token manager class
                fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, parserJavaCCPackageName, parserClassName + "TokenManager.java");
                writer = fo.openWriter();
                writer.write(generateTokenManagerClass(parserClassName, parserPackageName, parserJavaCCPackageName, mainElementName, parserAnnotation.skips()));
                writer.close();

                //Use javacc
                File file;
                if (!grammarURI.isAbsolute()) {
                    System.err.println("Path is not absolute " + grammarURI);
                    file = new File(grammarURI.toString());
                } else {
                    file = new File(grammarURI);
                }

                System.out.println("YAJCo: Generating output to '" + grammarURI + "'");
                String[] args = {"-OUTPUT_DIRECTORY=" + file.getParent(), file.toString()};
                Main.mainProgram(args);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
        }
        return false;
    }

    private String generateExceptionClass(String parserPackageName) throws IOException {
        StringWriter writer = new StringWriter();

        VelocityContext context = new VelocityContext();
        context.put("parserPackageName", parserPackageName);

        velocityEngine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream("templates/ParserException.javavm"), "utf-8"));

        return writer.toString();
    }

    private String generateParserClass(String parserClassName, String parserPackageName, String parserJavaCCPackageName, String mainElementName) throws IOException {
        StringWriter writer = new StringWriter();

        VelocityContext context = new VelocityContext();
        context.put("parserClassName", parserClassName);
        context.put("parserPackageName", parserPackageName);
        context.put("parserJavaCCPackageName", parserJavaCCPackageName);
        context.put("mainElementName", mainElementName);

        velocityEngine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream("templates/Parser.javavm"), "utf-8"));

        return writer.toString();
    }

    private String generateTokenManagerClass(String parserClassName, String parserPackageName, String parserJavaCCPackageName, String mainElementName, Skip[] skips) throws IOException {
        StringWriter writer = new StringWriter();

        VelocityContext context = new VelocityContext();
        context.put("Utilities", Utilities.class);
        context.put("parserClassName", parserClassName);
        context.put("parserJavaCCPackageName", parserJavaCCPackageName);
        context.put("tokens", definedTokens);
        context.put("skips", skips);

        velocityEngine.evaluate(context, writer, "", new InputStreamReader(getClass().getResourceAsStream("templates/TokenManager.javavm"), "utf-8"));

        return writer.toString();
    }

    private void processTypeElement(TypeElement typeElement, int paramNumber) {
        if (processedElements.contains(typeElement)) { //Already processed
            return;
        }
        if (typeElement.getAnnotation(Exclude.class) != null) { //Exclude
            return;
        }
        processedElements.add(typeElement);
        Expansion expansion;

        if (typeElement.getKind() == ElementKind.ENUM) { //Enum type
            expansion = processEnum(typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) { //Class
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) { //Abstract class
                expansion = processAbstractClassOrInterface(typeElement, paramNumber);
            } else {  //Concrete class
                expansion = processConcreteClass(typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.INTERFACE) { //Interface
            expansion = processAbstractClassOrInterface(typeElement, paramNumber);
        } else {
            throw new GeneratorException("Not supported type in model '" + typeElement + "'");
        }

        Production production = new Production(typeElement.getSimpleName().toString(), typeElement.getQualifiedName().toString(), expansion);
        productions.put(typeElement.getSimpleName().toString(), production);
    }

    private Expansion processEnum(TypeElement classElement) {
        List<Expansion> expansions = new ArrayList<Expansion>();
        for (Element element : classElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                String token = null;
                if (element.getAnnotation(Token.class) != null) {
                    token = element.getAnnotation(Token.class).value();
                }
                if (token == null) {
                    token = element.getSimpleName().toString();
                }
                if (!definedTokens.containsKey(token)) {
                    definedTokens.put(token, token);
                }

                expansions.add(new Terminal(
                        null,
                        "return " + classElement.getQualifiedName() + "." + element.getSimpleName() + ";",
                        token == null ? element.getSimpleName().toString() : token,
                        null));
            }
        }

        return new Choice(expansions.toArray(new Expansion[]{}));
    }

    private Expansion processConcreteClass(TypeElement classElement) {
        List<Expansion> sequences = new ArrayList<Expansion>();
        List<ExecutableElement> alternatives = getConstructorsAndFactoryMethods(classElement);

        int paramNumber = 0;
        for (ExecutableElement alternative : alternatives) {
            paramNumber++;
            List<Expansion> expansions = new ArrayList<Expansion>();

            //Before annotation
            if (alternative.getAnnotation(Before.class) != null) {
                expansions.addAll(tokensAsExpansions(alternative.getAnnotation(Before.class).value()));
            }

            StringBuffer code = new StringBuffer();
            boolean separator = false;
            for (VariableElement paramElement : alternative.getParameters()) {
                expansions.add(processParam(paramElement, paramNumber));
                if (separator) {
                    code.append(", ");
                }
                code.append(paramElement.getSimpleName() + "_" + paramNumber);
                separator = true;
            }

            //After annotation
            if (alternative.getAnnotation(After.class) != null) {
                expansions.addAll(tokensAsExpansions(alternative.getAnnotation(After.class).value()));
            }

            String argsCode = code.toString().trim();
            if ("".equals(argsCode)) {
                argsCode = "";
            } else {
                argsCode = ", (Object)" + code.toString();
            }

            String referenceResolverAction;
            if (alternative.getKind() == ElementKind.CONSTRUCTOR) {
                referenceResolverAction = "return tuke.pargen.ReferenceResolver.getInstance().register(new " + classElement.getQualifiedName().toString() + "( " + code.toString() + ")" + argsCode + ");";
            } else {
                Matcher matcher = Pattern.compile("([^()]+)(?:(.*))?").matcher(alternative.getSimpleName().toString());
                matcher.matches();
                String factoryMethodName = matcher.group(1);
                referenceResolverAction = "return tuke.pargen.ReferenceResolver.getInstance().register(" + classElement.getQualifiedName().toString() + "." + factoryMethodName + "( " + code.toString() + ")" + argsCode + ");";
            }

            Sequence sequence = new Sequence(
                    null,
                    referenceResolverAction,
                    expansions.toArray(new Expansion[]{}));
            sequences.add(sequence);
        }

        Choice choice = new Choice(sequences.toArray(new Expansion[]{}));

        //Find subclasses if there are any, create choice
        List<Expansion> expansionsInheritedClasses = new ArrayList<Expansion>();
        generateProductionsForSubtypes(classElement, getDirectSubtypes(classElement), expansionsInheritedClasses, paramNumber);
        if (expansionsInheritedClasses.size() != 0) {
            expansionsInheritedClasses.add(choice);

            return new Choice(
                    "  " + classElement.asType() + " _value = null;\n",
                    "return _value;",
                    null,
                    expansionsInheritedClasses.toArray(new Expansion[]{}));
        } else {
            return choice;
        }
    }

    private Expansion processAbstractClassOrInterface(TypeElement typeElement, int paramNumber) {
        //Find operators - direct subclasses
        Map<Integer, List<TypeElement>> priorityMap = findOperatorsInSubtypes(typeElement, null);

        //Process priority map for abstract class
        if (priorityMap != null) {
            //Add to operator elements with the lowest priority
            operatorElements.put(typeElement, priorityMap.keySet());
            processPriorityMap(typeElement, priorityMap, paramNumber);
        }

        //Create production for subclasses
        List<Expansion> expansions = new ArrayList<Expansion>();
        generateProductionsForSubtypes(typeElement, getDirectSubtypes(typeElement), expansions, paramNumber);

        //Parenthesis for operator
        Parentheses parentheses = typeElement.getAnnotation(Parentheses.class);
        if (parentheses != null) {
            expansions.add(new Sequence(
                    new Terminal(createTerminal(parentheses.left())),
                    new NonTerminal(getNonterminal(typeElement, paramNumber), "_value"),
                    new Terminal(createTerminal(parentheses.right()))));
        }

        //Lookahead
        String lookahead = null;
        if (typeElement.getAnnotation(Lookahead.class) != null) {
            lookahead = typeElement.getAnnotation(Lookahead.class).value();
        }

        return new Choice(
                "  " + typeElement.asType() + " _value = null;\n",
                "return _value;",
                lookahead,
                expansions.toArray(new Expansion[]{}));
    }

    private Map<Integer, List<TypeElement>> findOperatorsInSubtypes(TypeElement typeElement, Map<Integer, List<TypeElement>> priorityMap) {
        Set<TypeElement> subclassElements = getDirectSubtypes(typeElement);
        for (Element element : subclassElements) {
            //TODO: to preco tu ostalo? nie su vsetky potomkovia priami?
            if (isDirectSubtype(typeElement, element)) {
                TypeElement subclassElement = (TypeElement) element;
                if (subclassElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    priorityMap = findOperatorsInSubtypes(subclassElement, priorityMap);
                    //continue;
                } else {
                    if (isOperatorType(subclassElement)) {
                        ExecutableElement constructor = getConstructorElement(subclassElement);
                        int priority = constructor.getAnnotation(Operator.class).priority();
                        if (priorityMap == null) {
                            priorityMap = new TreeMap<Integer, List<TypeElement>>();
                        }
                        List<TypeElement> operatorList = priorityMap.get(priority);
                        if (operatorList == null) {
                            operatorList = new ArrayList<TypeElement>();
                            priorityMap.put(priority, operatorList);
                        }
                        operatorList.add(subclassElement);
                    }
                }
            }
        }

        return priorityMap;
    }

    private void generateProductionsForSubtypes(TypeElement typeElement, Set<TypeElement> subclassElements, List<Expansion> expansions, int paramNumber) {
        TypeElement subtypeElement = null;
        for (Element element : subclassElements) {
            if (isDirectSubtype(typeElement, element) && !isOperatorType((TypeElement) element)) {
                subtypeElement = (TypeElement) element;
                if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                    // Daná trieda je abstraktná, tak ju v hierarchii preskočme a poďme na ďalšiu úroveň
                    // v poradí
                    // ak daná trieda nemá žiadnych potomkov, tak spracovanie nie je nutné
                    if (getDirectSubtypes(subtypeElement).size() == 0) {
//						System.out.println("#### najdena trieda bez potomkov: " + subtypeElement);
                        continue;
                    }
                    // prejdime na ďalšiu úroveň v poradí
                    generateProductionsForSubtypes(subtypeElement, getDirectSubtypes(subtypeElement), expansions, paramNumber);
                    // pridajme neterminál pre danú triedu do pravidiel - budeme ho potrebovať
                    //processTypeElement(subclassElement);
                    //expansions.add(new NonTerminal(subclassElement.getSimpleName().toString(), "_value"));
                } else {
                    // Trieda nie je abstraktná, takže vytvorme príslušný neterminálny symbol
                    expansions.add(new NonTerminal(getNonterminal(subtypeElement, paramNumber), "_value"));
                }
            }
        }
    }

    //TODO: Ako je to s prioritou prefixnych a postfixnych operatorov?
    private void processPriorityMap(TypeElement operatorTypeElement, Map<Integer, List<TypeElement>> priorityMap, int paramNumber) {
        for (int priority : priorityMap.keySet()) {
            List<TypeElement> operatorList = priorityMap.get(priority);

            ExecutableElement constructorElement = getConstructorElement(operatorList.get(0));
            int arity = getArity(operatorTypeElement, constructorElement);
            yajco.model.pattern.impl.Associativity associativity = yajco.model.pattern.impl.Associativity.AUTO;

            //Test validity
            for (TypeElement subclassElement : operatorList) {
                constructorElement = getConstructorElement(subclassElement);
                if (arity != getArity(operatorTypeElement, constructorElement)) {
                    throw new GeneratorException("All operators of type '" + operatorTypeElement +
                            "' with the same priority must have the same arity (difference found in '" + subclassElement + "')");
                }
                yajco.model.pattern.impl.Associativity operatorAssociativity = constructorElement.getAnnotation(Operator.class).associativity();
                if (associativity == yajco.model.pattern.impl.Associativity.AUTO) {
                    associativity = operatorAssociativity;
                }
                if (operatorAssociativity != yajco.model.pattern.impl.Associativity.AUTO) {
                    if (associativity != yajco.model.pattern.impl.Associativity.AUTO && associativity != operatorAssociativity) {
                        throw new GeneratorException("All operators of type '" + operatorTypeElement + "' with the same priority must have the same association type (difference found in '" + subclassElement + "')");
                    }
                }
                if (arity == 0) {
                    throw new GeneratorException("Nulary operators are not supported '" + subclassElement + "'");
                }
                boolean prefixed = hasPrefix(operatorTypeElement, constructorElement);
                boolean postfixed = hasPostfix(operatorTypeElement, constructorElement);
                if (prefixed && postfixed) {
                    throw new GeneratorException("The operator of type '" + subclassElement + "' is prefixed and postfixed at the same time. Remove the @Operator annotation");
                }
                if (arity == 1) {
                    if (!prefixed && !postfixed) {
                        throw new GeneratorException("Unary prefix operator of type '" + subclassElement + "' should be prefixed or postfixed");
                    }
                    if (associativity == yajco.model.pattern.impl.Associativity.LEFT && prefixed) {
                        throw new GeneratorException("Unary prefix operator of type '" + subclassElement + "' should not be left-associative");
                    }
                    if (associativity == yajco.model.pattern.impl.Associativity.RIGHT && postfixed) {
                        throw new GeneratorException("Unary postfix operator of type '" + subclassElement + "' should not be right-associative");
                    }

                    if (associativity == yajco.model.pattern.impl.Associativity.AUTO) {
                        if (prefixed) {
                            associativity = yajco.model.pattern.impl.Associativity.RIGHT;
                        }
                        if (postfixed) {
                            associativity = yajco.model.pattern.impl.Associativity.LEFT;
                        }
                    } /* else {
                    if (associativity == Associativity.RIGHT && prefixed) {
                    throw new GeneratorException("Nary right-associative operator of type " + subclassElement + " should not be prefixed");
                    }
                    if (associativity == Associativity.LEFT && postfixed) {
                    throw new GeneratorException("N-ary left-associative operator of type " + subclassElement + " should not be postfixed");
                    }
                    } */
                }

            }

            //Set auto associativity to left if it is not set
            if (associativity == yajco.model.pattern.impl.Associativity.AUTO) {
                associativity = yajco.model.pattern.impl.Associativity.LEFT;
            }

            //Declarations
            StringBuilder decl = new StringBuilder();
            for (int i = 1; i <= arity; i++) {
                decl.append("  " + operatorTypeElement.getQualifiedName() + " _node" + i + " = null;\n");
            }

            String highestPriorityNonterminal = getNonterminal(operatorTypeElement, paramNumber);
            String nextPriorityNonterminal = getHigherPriorityNonterminal(priority, operatorTypeElement, priorityMap.keySet());
            String currentPriorityNonterminal = operatorTypeElement.getSimpleName().toString() + priority;
            Expansion expansion = null;
            if (arity == 1) {
                if (associativity == yajco.model.pattern.impl.Associativity.LEFT) {
                    expansion = new Sequence(decl.toString(), "return _node1;",
                            new NonTerminal(nextPriorityNonterminal, "_node1"),
                            new ZeroOrMany(generatePostfixOptions(null, null, operatorList, operatorTypeElement, paramNumber)));
                } else if (associativity == yajco.model.pattern.impl.Associativity.RIGHT) {
                    expansion = new Choice(decl.toString(), "return _node1;",
                            generatePrefixOptions(currentPriorityNonterminal, operatorList, operatorTypeElement, paramNumber),
                            new NonTerminal(nextPriorityNonterminal, "_node1"));
                } else if (associativity == yajco.model.pattern.impl.Associativity.NONE) {
                    Expansion prefixExpansion = generatePrefixOptions(nextPriorityNonterminal, operatorList, operatorTypeElement, paramNumber);
                    Expansion postfixExpansion = generatePostfixOptions(null, null, operatorList, operatorTypeElement, paramNumber);
                    if (prefixExpansion != null && postfixExpansion != null) {
                        expansion = new Choice(decl.toString(), "return _node1;",
                                new Sequence(
                                new NonTerminal(nextPriorityNonterminal, "_node1"),
                                new ZeroOrOne(postfixExpansion)),
                                prefixExpansion);
                    } else if (prefixExpansion != null) {
                        expansion = new Choice(decl.toString(), "return _node1;",
                                prefixExpansion,
                                new NonTerminal(nextPriorityNonterminal, "_node1"));
                    } else {
                        expansion = new Sequence(decl.toString(), "return _node1;",
                                new NonTerminal(nextPriorityNonterminal, "_node1"),
                                new ZeroOrOne(postfixExpansion));
                    }
                }
            } else {
                //TODO: podpora len pre infixne binarne operatory
                //TODO: Co ak by bol format A -> '+' A A, resp. A -> A A '-'
                //TODO: dorobit aritu > 2
                //Ak je ohranicene medzi dvoma terminalmi/neterminalny treba dat highestpriority
                //A -> '?' A ':' A
                if (associativity == yajco.model.pattern.impl.Associativity.LEFT) {
                    expansion = new Sequence(decl.toString(), "return _node1;",
                            new NonTerminal(nextPriorityNonterminal, "_node1"),
                            new ZeroOrMany(generatePostfixOptions(highestPriorityNonterminal, nextPriorityNonterminal, operatorList, operatorTypeElement, paramNumber)));
                } else if (associativity == yajco.model.pattern.impl.Associativity.RIGHT) {
                    expansion = new Sequence(decl.toString(), "return _node1;",
                            new NonTerminal(nextPriorityNonterminal, "_node1"),
                            new ZeroOrOne(generatePostfixOptions(highestPriorityNonterminal, currentPriorityNonterminal, operatorList, operatorTypeElement, paramNumber)));
                } else if (associativity == yajco.model.pattern.impl.Associativity.NONE) {
                    expansion = new Sequence(decl.toString(), "return _node1;",
                            new NonTerminal(nextPriorityNonterminal, "_node1"),
                            new ZeroOrOne(generatePostfixOptions(highestPriorityNonterminal, nextPriorityNonterminal, operatorList, operatorTypeElement, paramNumber)));
                }
            }
            Production production = new Production(operatorTypeElement.getSimpleName().toString() + priority, operatorTypeElement.getQualifiedName().toString(), expansion);
            productions.put(operatorTypeElement.getSimpleName().toString() + priority, production);
        }
    }

    private Expansion generatePostfixOptions(
            String highestPriorityNonterminal, String nonterminal, List<TypeElement> operatorList, TypeElement operatorTypeElement, int paramNumber) {
        List<Expansion> oExpansions = new ArrayList<Expansion>();
        for (TypeElement subclassElement : operatorList) {
            ExecutableElement constructorElement = getConstructorElement(subclassElement);
            if (hasPostfix(operatorTypeElement, constructorElement) || nonterminal != null) {
                List<Expansion> sExpansions = new ArrayList<Expansion>();
                StringBuilder code = new StringBuilder();
                StringBuilder params = new StringBuilder();
                code.append("_node1 = tuke.pargen.ReferenceResolver.getInstance().register(new " + subclassElement.getQualifiedName().toString() + "(");
                boolean separator = false;
                int index = 0;
                List<? extends VariableElement> parameters = constructorElement.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement paramElement = parameters.get(i);
                    if (separator) {
                        params.append(", ");
                    }

                    separator = true;
                    if (isOperatorType(operatorTypeElement, paramElement)) {
                        //TODO: Spracovat ak je to subclass a nie priamo trieda operatora
						/*if (!isSameOperatorType(classElement, paramElement)) {
                        //						System.out.println("------>> Postfix - spracuvavam triedu: " + getTypeElementFrom(paramElement.asType()));
                        processTypeElement(getTypeElementFrom(paramElement.asType()));
                        }*/
                        //System.out.println(">>>>>>>>>>>>>>>> " + paramElement.asType());
                        index++;
                        if (index == 1) {
                            if (paramElement.getAnnotation(After.class) != null) {
                                sExpansions.addAll(tokensAsExpansions(paramElement.getAnnotation(After.class).value()));
                            }
                        } else {
                            if (paramElement.getAnnotation(Before.class) != null) {
                                sExpansions.addAll(tokensAsExpansions(paramElement.getAnnotation(Before.class).value()));
                            }

                            if (i == parameters.size() - 1 && !hasPostfix(operatorTypeElement, constructorElement)) {
                                sExpansions.add(new NonTerminal(nonterminal, "_node" + index));
                            } else {
                                sExpansions.add(new NonTerminal(highestPriorityNonterminal, "_node" + index));
                            }
                            if (paramElement.getAnnotation(After.class) != null) {
                                sExpansions.addAll(tokensAsExpansions(paramElement.getAnnotation(After.class).value()));
                            }
                        }
                        // add type casting if necessary

                        if (isSameType(operatorTypeElement.asType(), paramElement.asType())) {
                            params.append("_node" + index);
                        } else {
                            params.append("(" + paramElement.asType() + ")" + "_node" + index);
                        }
                    } else {
                        sExpansions.add(processParam(paramElement, paramNumber));
                        params.append(paramElement.getSimpleName().toString());
                    }
                }

                code.append(params + "), (Object)" + params);
                code.append(");");
                if (constructorElement.getAnnotation(After.class) != null) {
                    sExpansions.addAll(tokensAsExpansions(constructorElement.getAnnotation(After.class).value()));
                }
                oExpansions.add(
                        new Sequence(null, code.toString(), sExpansions.toArray(new Expansion[]{})));
            }
        }

        if (oExpansions.size() == 0) {
            return null;
        } else if (oExpansions.size() > 1) {
            return new Choice(oExpansions.toArray(new Expansion[]{}));
        } else {
            return oExpansions.get(0);
        }
    }

    private Expansion generatePrefixOptions(String nonterminal, List<TypeElement> operatorList, TypeElement operatorTypeElement, int paramNumber) {
        List<Expansion> oExpansions = new ArrayList<Expansion>();
        int type = 0;
        StringBuilder code = new StringBuilder();
        code.append("switch(_type) {");
        for (TypeElement subclassElement : operatorList) {
            ExecutableElement constructorElement = getConstructorElement(subclassElement);
            if (hasPrefix(operatorTypeElement, constructorElement)) {
                List<Expansion> sExpansions = new ArrayList<Expansion>();
                if (constructorElement.getAnnotation(Before.class) != null) {
                    sExpansions.addAll(tokensAsExpansions(constructorElement.getAnnotation(Before.class).value()));
                }
                type++;
                StringBuilder params = new StringBuilder();
                code.append("case " + type + ": _node1 = tuke.pargen.ReferenceResolver.getInstance().register(new " + subclassElement.getQualifiedName().toString() + "(");
                boolean separator = false;
                int index = 0;
                List<? extends VariableElement> parameters = constructorElement.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement paramElement = parameters.get(i);
                    if (separator) {
                        params.append(", ");
                    }
                    separator = true;
                    if (isOperatorType(operatorTypeElement, paramElement)) {
                        //TODO: Spracovat ak je to subclass a nie priamo trieda operatora
                        //System.out.println(">>>>>>>>>>>>>>>> " + paramElement.asType());
                        index++;
                        if (paramElement.getAnnotation(Before.class) != null) {
                            sExpansions.addAll(tokensAsExpansions(paramElement.getAnnotation(Before.class).value()));
                        }
                        // add type casting if necessery
                        if (isSameType(operatorTypeElement.asType(), paramElement.asType())) {
                            params.append("_node" + index);
                        } else {
                            params.append("(" + paramElement.asType() + ")" + "_node" + index);
                        }
                    } else {
                        sExpansions.add(processParam(paramElement, paramNumber));
                        params.append(paramElement.getSimpleName());
                    }
                }

                code.append(params + "), (Object)" + params);
                code.append("); break; ");

                oExpansions.add(new Sequence(null, "_type = " + type + ";", sExpansions.toArray(new Expansion[]{})));
            }
        }

        code.append("};");

        Expansion expansion;

        if (oExpansions.size() == 0) {
            return null;
        } else if (oExpansions.size() > 1) {
            expansion = new Choice(oExpansions.toArray(new Expansion[]{}));
        } else {
            expansion = oExpansions.get(0);
        }

        Expansion nonTerminal = new NonTerminal(nonterminal, "_node1");
        expansion =
                new Sequence("  int _type = 0;\n", code.toString(), expansion, nonTerminal);

        return expansion;
    }

    private Expansion processParam(VariableElement paramElement, int paramNumber) {
        List<Expansion> expansions = new ArrayList<Expansion>();

        //Before annotation
        if (paramElement.getAnnotation(Before.class) != null) {
            expansions.addAll(tokensAsExpansions(paramElement.getAnnotation(Before.class).value()));
        }

        //Parameter
        TypeMirror paramElemType = paramElement.asType();
        String paramElemTypeString = paramElemType.toString();

        if (paramElemType instanceof ArrayType || paramElemTypeString.startsWith(
                "java.util.List") || paramElemTypeString.startsWith("java.util.Set")) {
            expansions.add(processArrayTypeCode(paramElement, paramNumber));
        } else {
            String variableName = paramElement.getSimpleName() + "_" + paramNumber;
            expansions.add(processSimpleTypeCode(paramElement, variableName, paramElement.asType(), "", paramNumber));
        }

        //After annotation
        if (paramElement.getAnnotation(After.class) != null) {
            expansions.addAll(tokensAsExpansions(paramElement.getAnnotation(After.class).value()));
        }

        //Optional annotation
        Expansion expansion = new Sequence(expansions.toArray(new Expansion[]{}));

        if (paramElement.getAnnotation(Optional.class) != null) {
            expansion = new ZeroOrOne(expansion);
        }

        return expansion;
    }

    private Expansion processSimpleTypeCode(VariableElement paramElement, String variableName, TypeMirror type, String code, int paramNumber) {
        if (stringConversions.containsConversion(type.toString()) ||
                paramElement.getAnnotation(Token.class) != null) {  //Terminal = conversion exists or param has @Token
            return generateTeminal(paramElement, variableName, type, code);
        } else { //Nonterminal
            return generateNonteminal(paramElement, variableName, type, code, paramNumber);
        }
    }

    private Terminal generateTeminal(VariableElement paramElement, String variableName, TypeMirror type, String code) {
        String token = "";
        if (paramElement.getAnnotation(Token.class) != null) {
            token = paramElement.getAnnotation(Token.class).value();
        }
        if ("".equals(token)) {
            String paramElementName = paramElement.getSimpleName().toString();
            token = toUpperCaseNotation(paramElementName);
            if (!definedTokens.containsValue(token) && paramElementName.endsWith("s")) {
                token = toUpperCaseNotation(paramElementName.substring(0, paramElementName.length() - 1));
            }
        }

        if (stringConversions.containsConversion(type.toString())) {
            String conversion = stringConversions.getConversion(type.toString());
            String defaultValue = stringConversions.getDefaultValue(type.toString());
            Formatter decl = new Formatter();
            decl.format("  %s %s = %s;\n", type, variableName, defaultValue);
            decl.format("  Token _token%s = null;\n", variableName);

            Formatter codet = new Formatter();
            codet.format("%s = ", variableName);
            codet.format(conversion, "_token" + variableName + ".image");
            codet.format(";");
            codet.format("%s", code);

            return new Terminal(decl.toString(), codet.toString(), token, "_token" + variableName);
        } else {
            throw new GeneratorException("Unsuported parameter '" + paramElement + " : " + paramElement.asType() +
                    "' in element '" + paramElement.getEnclosingElement().getEnclosingElement() + "'");
        }
    }

    private Expansion generateNonteminal(VariableElement paramElement, String variableName, TypeMirror type, String code, int paramNumber) {
        Element element = processingEnv.getTypeUtils().asElement(type);
        if (element != null) {
            //TODO: Musi to byt vsetko v jedno baliku, co keby som chcel pridat jazyk so jazyka
            if (element.getKind() == ElementKind.ENUM || //Enum
                    isKnownClass(element)) { //Non terminal
                return new NonTerminal(
                        "  " + type + " " + variableName + " = null;\n",
                        code,
                        getNonterminal((TypeElement) element, paramNumber),
                        variableName);
            } else {
                throw new GeneratorException("1Unsuported parameter '" + paramElement + " : " + paramElement.asType() +
                        "' in element '" + paramElement.getEnclosingElement().getEnclosingElement() + "'");
            }
        } else {
            throw new GeneratorException("-- Unsuported parameter '" + paramElement + " : " + paramElement.asType() +
                    "' in element '" + paramElement.getEnclosingElement().getEnclosingElement() + "'");
        }
    }

    //TODO: Dorobit na List
    //TODO: Moze byt pole v poli
    private Expansion processArrayTypeCode(VariableElement paramElement, int paramNumber) {
        int from = 0;
        int to = Range.INFINITY;
        String separator = "";
        TypeMirror type;
        TypeMirror baseComponentType;
        TypeMirror componentType;
        boolean isArray = paramElement.asType() instanceof ArrayType;
        boolean isList = paramElement.asType().toString().startsWith("java.util.List");
        boolean isPrimitive = false;

        if (isArray) {
            type = paramElement.asType();
            baseComponentType = ((ArrayType) type).getComponentType();
            isPrimitive = baseComponentType.getKind().isPrimitive();
            if (isPrimitive) {
                componentType = processingEnv.getTypeUtils().boxedClass((PrimitiveType) baseComponentType).asType();
            } else {
                componentType = baseComponentType;
            }
        } else {
            Matcher matcher = Pattern.compile("(.+)<(.+)>").matcher(paramElement.asType().toString());
            matcher.matches();
            type = processingEnv.getElementUtils().getTypeElement(matcher.group(1)).asType();
            componentType = processingEnv.getElementUtils().getTypeElement(matcher.group(2)).asType();
            baseComponentType = componentType;
        }

        if (paramElement.getAnnotation(Range.class) != null) {
            from = paramElement.getAnnotation(Range.class).minOccurs();
            to = paramElement.getAnnotation(Range.class).maxOccurs();

        }
        if (paramElement.getAnnotation(Separator.class) != null) {
            separator = paramElement.getAnnotation(Separator.class).value();
        }

        StringBuilder decl = new StringBuilder();
        String variableName = paramElement.getSimpleName() + "_" + paramNumber;
        if (isArray) {
            decl.append("  " + baseComponentType + "[] " + variableName + " = null;\n");
        } else if (isList) {
            decl.append("  java.util.List<" + componentType + "> " + variableName + " = null;\n");
        } else {
            decl.append("  java.util.Set<" + componentType + "> " + variableName + " = null;\n");
        }

        if (isArray || isList) {
            decl.append("  java.util.List<" + componentType + "> _list" + variableName + " = new java.util.ArrayList<" + componentType + ">();\n");
        } else {
            decl.append("  java.util.Set<" + componentType + "> _list" + variableName + " = new java.util.HashSet<" + componentType + ">();\n");
        }
        StringBuilder code = new StringBuilder();
        code.append("_list" + variableName + ".add(_item" + variableName + ");");
        StringBuilder ccode = new StringBuilder();
        if (isArray && isPrimitive) {
            ccode.append(variableName + " = new " + baseComponentType + "[_list" + variableName + ".size()]; for (int i = 0; i < _list" + variableName + ".size(); i++) { " + variableName + "[i] = _list" + variableName + ".get(i); }");
        } else if (isArray && !isPrimitive) {
            ccode.append(variableName + " = _list" + variableName + ".toArray(new " + componentType + "[] {});");
        } else {
            ccode.append(variableName + " = _list" + variableName + ";");
        }

        Expansion separatorTerminal = "".equals(separator) ? null : new Terminal(createTerminal(separator));
        Expansion sexpansion = processSimpleTypeCode(paramElement, "_item" + variableName, componentType, code.toString(), paramNumber);
        decl.append(sexpansion.getDecl());
        sexpansion.setDecl(null);

        //Lookahead
        String lookahead = null;
        if (paramElement.getAnnotation(Lookahead.class) != null) {
            lookahead = paramElement.getAnnotation(Lookahead.class).value();
        }

        if (from == 0 && to == Range.INFINITY && !"".equals(separator)) {
            return new ZeroOrOne(
                    decl.toString(),
                    ccode.toString(),
                    new Sequence(sexpansion,
                    new ZeroOrMany(null, null, lookahead,
                    new Sequence(separatorTerminal, sexpansion))));
        } else {
            List<Expansion> expansions = new ArrayList<Expansion>();
            for (int i = 0; i < from; i++) {
                if (i > 0 && !"".equals(separator)) {
                    expansions.add(separatorTerminal);
                }
                expansions.add(sexpansion);
            }
            if (to == Range.INFINITY) {
                if (!"".equals(separator)) {
                    expansions.add(
                            new ZeroOrMany(null, null, lookahead,
                            new Sequence(separatorTerminal, sexpansion)));
                } else {
                    expansions.add(new ZeroOrMany(null, null, lookahead, sexpansion));
                }
            } else {
                if (from < to) {
                    Expansion expansion;
                    if (from > 0 && !"".equals(separator)) {
                        expansion = new Sequence(separatorTerminal, sexpansion);
                    } else {
                        expansion = sexpansion;
                    }
                    expansion = new ZeroOrOne(expansion);
                    for (int i = from + 1; i < to; i++) {
                        if (!"".equals(separator)) {
                            expansion = new Sequence(separatorTerminal, sexpansion, expansion);
                        } else {
                            expansion = new Sequence(sexpansion, expansion);
                        }
                        expansion = new ZeroOrOne(expansion);
                    }
                    expansions.add(expansion);
                }
                //TODO: chyba ak plati, ze to<from
            }
            return new Sequence(
                    decl.toString(),
                    ccode.toString(),
                    expansions.toArray(new Expansion[]{}));
        }
    }

    private ExecutableElement getConstructorElement(TypeElement classElement) {
        Elements elementUtils = processingEnv.getElementUtils();
        for (Element element : elementUtils.getAllMembers(classElement)) {
            if (element.getKind() == ElementKind.CONSTRUCTOR && element.getAnnotation(Exclude.class) == null) {
                return (ExecutableElement) element;
            }
        }

        throw new GeneratorException("Suitable constructor not found for type '" + classElement + "'");
    }

    private List<ExecutableElement> getConstructorsAndFactoryMethods(TypeElement classElement) {
        List<ExecutableElement> constructors = new ArrayList<ExecutableElement>();
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

    private boolean isDirectSubtype(TypeElement supertype, Element element) {
        if (element.getAnnotation(Exclude.class) == null) {
            if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE || element.getKind() == ElementKind.ENUM) {
                TypeMirror superType = supertype.asType();
                if (processingEnv.getTypeUtils().isSameType(((TypeElement) element).getSuperclass(), superType)) {
                    return true;
                }
                for (TypeMirror type : ((TypeElement) element).getInterfaces()) {
                    if (processingEnv.getTypeUtils().isSameType(type, superType)) {
                        return true;
                    }
                }
            }
        }
        return false;
//		return element.getKind() == ElementKind.CLASS && element.getAnnotation(Exclude.class) == null &&
//				processingEnv.getTypeUtils().isSameType(((TypeElement) element).getSuperclass(), superclass.asType());
    }

    private boolean isOperatorType(TypeElement typeElement) {
        ExecutableElement constructor = getConstructorElement(typeElement);
        return constructor.getAnnotation(Operator.class) != null;
    }

    private String getNonterminal(TypeElement typeElement, int paramNumber) {
        processTypeElement(typeElement, paramNumber);

        if (operatorElements.containsKey(typeElement)) {
            return typeElement.getSimpleName() + operatorElements.get(typeElement).iterator().next().toString();
        }

        return typeElement.getSimpleName().toString();
    }

    private String getHigherPriorityNonterminal(int current, TypeElement classElement, Set<Integer> priorities) {
        Iterator<Integer> iterator = priorities.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(current)) {
                break;
            }

        }
        if (iterator.hasNext()) {
            return classElement.getSimpleName() + String.valueOf(iterator.next());
        }

        return classElement.getSimpleName().toString();
    }

    private int getArity(TypeElement operatorClassElement, ExecutableElement constructor) {
        int count = 0;
        for (VariableElement paramElement : constructor.getParameters()) {
            if (isOperatorType(operatorClassElement, paramElement)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasPrefix(TypeElement operatorClassElement, ExecutableElement constructor) {
        if (constructor.getAnnotation(Before.class) != null) {
            return true;
        }
        VariableElement paramElement = constructor.getParameters().get(0);
        if (paramElement.getAnnotation(Before.class) != null) {
            return true;
        }
        // OTAZKA: To tu je preco?
        if (!isOperatorType(operatorClassElement, paramElement)) {
            return true;
        }
        return false;
    }

    private boolean hasPostfix(TypeElement operatorClassElement, ExecutableElement constructor) {
        if (constructor.getAnnotation(After.class) != null) {
            return true;
        }
        VariableElement paramElement = constructor.getParameters().get(constructor.getParameters().size() - 1);
        if (paramElement.getAnnotation(After.class) != null) {
            return true;
        }
        if (!isOperatorType(operatorClassElement, paramElement)) {
            return true;
        }
        return false;
    }

    private boolean isOperatorType(TypeElement typeElement, VariableElement paramElement) {
        return processingEnv.getTypeUtils().isSubtype(paramElement.asType(), typeElement.asType());
    }

    private boolean isSameType(TypeMirror type1, TypeMirror type2) {
        return processingEnv.getTypeUtils().isSameType(type1, type2);
    }

    private List<Expansion> tokensAsExpansions(String[] tokens) {
        List<Expansion> expansion = new ArrayList<Expansion>();
        for (String token : tokens) {
            expansion.add(new Terminal(createTerminal(token)));
        }

        return expansion;
    }

    private String createTerminal(String token) {
        if (!definedTokens.containsValue(token)) {
            definedTokens.put(token, token);
//                String mapToken = definedTokens.get(token);
//                if (mapToken == null) {
//                    throw new GeneratorException(String.format("Token with name or regular expression \"%s\" was not defined!", token));
//                }
//                token = mapToken;
        }
        return token;
    }

    private boolean isKnownClass(Element typeElement) {
        if (typeElement.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE) {
            for (Element element : roundEnv.getRootElements()) {
                if ((element.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE) && typeElement.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<TypeElement> getDirectSubtypes(TypeElement classElement) {
        Set<TypeElement> subclassElements = new HashSet<TypeElement>();
        for (Element element : roundEnv.getRootElements()) {
            if (isDirectSubtype(classElement, element)) {
                subclassElements.add((TypeElement) element);
            }
        }

        return subclassElements;
    }

    private String toUpperCaseNotation(String camelNotation) {
        StringBuilder sb = new StringBuilder(camelNotation.length() + 10);
        boolean change = true;
        for (int i = 0; i < camelNotation.length(); i++) {
            char c = camelNotation.charAt(i);
            change = !change && Character.isUpperCase(c);
            if (change) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
            change = Character.isUpperCase(c);
        }
        return sb.toString();
    }

    public static Conversions getStringConversions() {
        return stringConversions;
    }

    private void testTokensDefinitions(Skip[] skips) {
        for (Map.Entry<String, String> entry : definedTokens.entrySet()) {
            String pattern = entry.getValue();
            try {
                if (entry.getKey().equals(pattern)) {
                    pattern = Utilities.encodeStringIntoRegex(entry.getValue());
                }
                Pattern.compile(pattern);
            } catch (Exception e) {
                throw new GeneratorException("The definition of token '" + entry.getKey() + "' is not valid '" + pattern + "'", e);
            }
        }

        for (Skip skip : skips) {
            try {
                Pattern.compile(skip.value());
            } catch (Exception e) {
                throw new GeneratorException("The definition of skip token is not valid '" + skip.value() + "'", e);
            }
        }
    }
}
