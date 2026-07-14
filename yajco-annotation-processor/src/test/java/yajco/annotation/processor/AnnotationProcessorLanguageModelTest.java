package yajco.annotation.processor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import yajco.model.*;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PatternSupport;
import yajco.model.pattern.impl.*;
import yajco.model.type.*;

import java.util.List;

import static org.junit.Assert.*;
import static yajco.annotation.processor.AnnotationProcessorTestCompiler.SourceSpec;
import static yajco.annotation.processor.AnnotationProcessorTestCompiler.source;

public class AnnotationProcessorLanguageModelTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AnnotationProcessorTestCompiler compiler;

    @Before
    public void setUp() {
        compiler = new AnnotationProcessorTestCompiler(temporaryFolder);
    }

    private static @NonNull SourceSpec numberLiteralSource(@Nullable String parent) {
        String extendsPart = parent == null ? "" : " extends " + parent;
        return source("test.NumberLiteral",
            "package test;\n"
            + "import yajco.annotation.Token;\n"
            + "public class NumberLiteral" + extendsPart + " {\n"
            + "    public NumberLiteral(@Token(\"INT\") int value) {}\n"
            + "}\n");
    }

    @Test
    public void shouldCreateLanguageModelFromAnnotatedParserClass() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.lang.NumberLiteral",
            "package test.lang;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(\n"
                + "    tokens = @TokenDef(name = \"INT\", regexp = \"[0-9]+\"),\n"
                + "    options = @Option(name = \"yajco.generateParser\", value = \"false\")\n"
                + ")\n"
                + "public class NumberLiteral {\n"
                + "    @Before(\"number\")\n"
                + "    public NumberLiteral(@Token(\"INT\") int value) {\n"
                + "    }\n"
                + "}\n");

        assertEquals("test.lang", language.getName());
        assertToken(language, "INT", "[0-9]+");
        assertSkip(language, "\\s");

        Concept concept = requireConcept(language, "NumberLiteral");

        Property property = concept.getProperty("value");
        assertNotNull(property);
        assertEquals("yajco.model.type.PrimitiveType", property.getType().getClass().getName());

        Notation notation = requireSingleNotation(concept);
        assertEquals(2, notation.getParts().size());
        assertTokenPart(notation, 0, "number");
        PropertyReferencePart valuePart = assertNotationPart(notation, 1, PropertyReferencePart.class);
        assertEquals("value", valuePart.getProperty().getName());
    }

    @Test
    public void shouldAddMetadataAndCommentSkipsFromLanguageAnnotation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.robot.Robot",
            "package test.robot;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Language(\n"
                + "    name = \"robot\",\n"
                + "    description = \"Robot commands\",\n"
                + "    version = \"1.0\",\n"
                + "    fileExtensions = {\".robot\"}\n"
                + ")\n"
                + "@Parser(\n"
                + "    skips = {\n"
                + "        @Skip(whitespace=true),\n"
                + "        @Skip(lineComment=\"//\"),\n"
                + "        @Skip(blockComment={\"/*\", \"*/\"})\n"
                + "    },\n"
                + "    options = @Option(name = \"yajco.generateParser\", value = \"false\")\n"
                + ")\n"
                + "public class Robot {\n"
                + "    public Robot() {\n"
                + "    }\n"
                + "}\n");

        assertEquals("robot", language.getSetting("yajco.ir.languageName"));
        assertEquals("Robot commands", language.getSetting("yajco.ir.description"));
        assertEquals("1.0", language.getSetting("yajco.ir.version"));
        assertEquals(".robot", language.getSetting("yajco.ir.fileExtensions"));
        assertEquals("//", language.getSetting("yajco.ir.lineComment"));
        assertEquals("/*", language.getSetting("yajco.ir.blockComment.start"));
        assertEquals("*/", language.getSetting("yajco.ir.blockComment.end"));
        assertEquals("robot.ir.json", language.getSetting("yajco.ir.file"));
        assertEquals("ir", language.getSetting("yajco.generateTools"));

        assertSkip(language, "\\s");
        assertSkip(language, "//.*");
        assertSkip(language, "/\\*(?:(?!\\*/)[\\s\\S])*\\*/");
    }

    @Test
    public void shouldRecognizeInheritanceRelation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Expression",
                "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public abstract class Expression {}\n"),

            numberLiteralSource("Expression"),

            source("test.Add",
                "package test;\n"
                + "import yajco.annotation.Before;\n"
                + "public class Add extends Expression {\n"
                + "    public Add(Expression left, @Before(\"+\") Expression right) {}\n"
                + "}\n")
        );

        Concept expression = requireConcept(language, "Expression");
        requireConcept(language, "NumberLiteral");
        Concept add = requireConcept(language, "Add");
        assertEquals(expression, add.getParent());

    }

    @Test
    public void shouldRecognizeCompositionRelation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Expression",
                "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public abstract class Expression {}\n"),

            numberLiteralSource("Expression"),

            source("test.Add",
                "package test;\n"
                + "import yajco.annotation.Before;\n"
                + "public class Add extends Expression {\n"
                + "    public Add(Expression left, @Before(\"+\") Expression right) {}\n"
                + "}\n")
        );

        Concept expression = requireConcept(language, "Expression");
        Concept add = requireConcept(language, "Add");
        Property left = add.getProperty("left");
        assertNotNull(left);
        assertReferenceToConcept(expression, left.getType());
        Property right = add.getProperty("right");
        assertNotNull(right);
        assertReferenceToConcept(expression, right.getType());
    }

    @Test
    public void shouldRecognizeArrayCompositionMultiplicity() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Numbers",
                "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Numbers {\n"
                + "    public Numbers(NumberLiteral[] elements) {}\n"
                + "}\n"),

            numberLiteralSource(null)
        );

        Concept list = requireConcept(language, "Numbers");
        Concept numberLiteral = requireConcept(language, "NumberLiteral");
        Property elements = list.getProperty("elements");
        assertNotNull(elements);
        assertEquals(ArrayType.class, elements.getType().getClass());
        assertReferenceToConcept(numberLiteral, ((ComponentType) elements.getType()).getComponentType());
    }

    @Test
    public void shouldRecognizeListCompositionMultiplicity() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Numbers",
                "package test;\n"
                + "import java.util.List;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Numbers {\n"
                + "    public Numbers(List<NumberLiteral> elements) {}\n"
                + "}\n"),

            numberLiteralSource(null)
        );

        Concept list = requireConcept(language, "Numbers");
        Concept numberLiteral = requireConcept(language, "NumberLiteral");
        Property elements = list.getProperty("elements");
        assertNotNull(elements);
        assertEquals(ListType.class, elements.getType().getClass());
        assertReferenceToConcept(numberLiteral, ((ComponentType) elements.getType()).getComponentType());
    }

    @Test
    public void shouldRecognizeSetCompositionMultiplicity() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Numbers",
                "package test;\n"
                + "import java.util.Set;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Numbers {\n"
                + "    public Numbers(Set<NumberLiteral> elements) {}\n"
                + "}\n"),

            numberLiteralSource(null)
        );

        Concept list = requireConcept(language, "Numbers");
        Concept numberLiteral = requireConcept(language, "NumberLiteral");
        Property elements = list.getProperty("elements");
        assertNotNull(elements);
        assertEquals(SetType.class, elements.getType().getClass());
        assertReferenceToConcept(numberLiteral, ((ComponentType) elements.getType()).getComponentType());
    }

    @Test
    public void shouldApplyBeforeAndAfterOnConcept() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Number",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "@Before(\"begin\")\n"
                + "@After(\"end\")\n"
                + "public class Number {\n"
                + "    public Number(NumberLiteral element) {}\n"
                + "}\n"),
            numberLiteralSource(null)
        );
        Concept number = requireConcept(language, "Number");
        Notation notation = requireSingleNotation(number);
        assertEquals(3, notation.getParts().size());
        assertTokenPart(notation, 0, "begin");
        assertNotationPart(notation, 1, PropertyReferencePart.class);
        assertTokenPart(notation, 2, "end");
    }

    @Test
    public void shouldApplyBeforeAndAfterOnProperty() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Number",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Number {\n"
                + "    public Number(@Before(\"begin\") @After(\"end\") NumberLiteral element) {}\n"
                + "}\n"),
            numberLiteralSource(null)
        );
        Concept number = requireConcept(language, "Number");
        Notation notation = requireSingleNotation(number);
        assertEquals(3, notation.getParts().size());
        assertTokenPart(notation, 0, "begin");
        assertNotationPart(notation, 1, PropertyReferencePart.class);
        assertTokenPart(notation, 2, "end");
    }

    @Test
    public void shouldApplyBeforeAndAfterOnConceptAndProperties() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Numbers",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "@Before(\"begin\") @After(\"end\")"
                + "public class Numbers {\n"
                + "    public Numbers("
                + "        @Before(\"beforeFirst\") @After(\"afterFirst\") NumberLiteral first,"
                + "        @Before(\"beforeSecond\") @After(\"afterSecond\") NumberLiteral second) {}\n"
                + "}\n"),
            numberLiteralSource(null)
        );
        Concept number = requireConcept(language, "Numbers");
        Notation notation = requireSingleNotation(number);
        assertEquals(8, notation.getParts().size());
        assertTokenPart(notation, 0, "begin");
        assertTokenPart(notation, 1, "beforeFirst");
        assertNotationPart(notation, 2, PropertyReferencePart.class);
        assertTokenPart(notation, 3, "afterFirst");
        assertTokenPart(notation, 4, "beforeSecond");
        assertNotationPart(notation, 5, PropertyReferencePart.class);
        assertTokenPart(notation, 6, "afterSecond");
        assertTokenPart(notation, 7, "end");
    }

    @Test
    public void shouldApplySeparatorPattern() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Numbers",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Numbers {\n"
                + "    public Numbers(@Separator(\",\") NumberLiteral[] elements) {}\n"
                + "}\n"),

            numberLiteralSource(null)
        );
        Concept list = requireConcept(language, "Numbers");
        Notation notation = requireSingleNotation(list);
        PropertyReferencePart elementsPart = assertNotationPart(notation, 0, PropertyReferencePart.class);
        List<NotationPartPattern> patterns = elementsPart.getPatterns();
        assertEquals(1, patterns.size());
        assertTrue(patterns.get(0) instanceof Separator);
        assertEquals(",", ((Separator) patterns.get(0)).getValue());
    }

    @Test
    public void shouldMapRangePatternFromAnnotation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Program",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Program {\n"
                + "    public Program(@Range(minOccurs = 1, maxOccurs = 3) NumberLiteral[] statements) {}\n"
                + "}\n"),
            numberLiteralSource(null)
        );

        Concept program = requireConcept(language, "Program");
        Notation notation = requireSingleNotation(program);
        assertEquals(1, notation.getParts().size());
        PropertyReferencePart statements = assertNotationPart(notation, 0, PropertyReferencePart.class);
        Range range = assertPattern(statements, Range.class);
        assertEquals(1, range.getMinOccurs());
        assertEquals(3, range.getMaxOccurs());
    }

    @Test
    public void shouldMapOperatorAndParenthesesPatternsFromAnnotations() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Expression",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parentheses(left = \"(\", right = \")\")\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public abstract class Expression {}\n"),
            source("test.Add",
                "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.model.pattern.impl.Associativity;\n"
                + "public class Add extends Expression {\n"
                + "    @Operator(priority = 2, associativity = Associativity.RIGHT)\n"
                + "    public Add(Expression left, @Before(\"+\") Expression right) {}\n"
                + "}\n"),
            numberLiteralSource("Expression")
        );

        Concept expression = requireConcept(language, "Expression");
        Parentheses parentheses = assertPattern(expression, Parentheses.class);
        assertEquals("(", parentheses.getLeft());
        assertEquals(")", parentheses.getRight());

        Concept add = requireConcept(language, "Add");
        Operator operator = assertPattern(add, Operator.class);
        assertEquals(2, operator.getPriority());
        assertEquals(Associativity.RIGHT, operator.getAssociativity());
    }

    @Test
    public void shouldCreateReferenceBindingFromIdentifierAndReferencesPatterns() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Call",
                "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "import yajco.annotation.reference.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Call {\n"
                + "    private final Function function;\n"
                + "    public Call(@References(Function.class) String function) {\n"
                + "        this.function = null;\n"
                + "    }\n"
                + "}\n"),
            source("test.Function",
                "package test;\n"
                + "import yajco.annotation.reference.*;\n"
                + "public class Function {\n"
                + "    @Identifier\n"
                + "    private final String name;\n"
                + "    public Function(String name) {\n"
                + "        this.name = name;\n"
                + "    }\n"
                + "}\n")
        );

        Concept function = requireConcept(language, "Function");
        Property name = function.getProperty("name");
        assertNotNull(name);
        assertNotNull(name.getPattern(Identifier.class));

        Concept call = requireConcept(language, "Call");
        Property functionProperty = call.getProperty("function");
        assertNotNull(functionProperty);
        assertReferenceToConcept(function, functionProperty.getType());

        Notation notation = requireSingleNotation(call);
        assertEquals(1, notation.getParts().size());
        LocalVariablePart functionPart = assertNotationPart(notation, 0, LocalVariablePart.class);
        References references = assertPattern(functionPart, References.class);
        assertEquals(function, references.getConcept());
        assertNull(references.getProperty());

        Token token = assertPattern(functionPart, Token.class);
        assertEquals("IDENTIFIER", token.getName());
    }

    @Test
    public void shouldAddDefaultIdentifierTokenDefinitionForReferencesAnnotation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.Call",
                "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "import yajco.annotation.reference.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Call {\n"
                + "    public Call(@References(Function.class) String function) {\n"
                + "    }\n"
                + "}\n"),
            source("test.Function",
                "package test;\n"
                + "public class Function {\n"
                + "    public Function(String name) {\n"
                + "    }\n"
                + "}\n")
        );

        TokenDef identifierToken = language.getToken("IDENTIFIER");
        assertNotNull(identifierToken);
        assertEquals("[a-zA-Z_][a-zA-Z0-9_]*", identifierToken.getRegexp());
    }

    @Test
    public void shouldAddDefaultBooleanValuePatternForBooleanParameter() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.Switch",
            "package test;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Switch {\n"
                + "    public Switch(boolean enabled) {\n"
                + "    }\n"
                + "}\n");

        Concept concept = requireConcept(language, "Switch");
        Notation notation = requireSingleNotation(concept);
        PropertyReferencePart enabledPart = assertNotationPart(notation, 0, PropertyReferencePart.class);
        yajco.model.pattern.impl.BooleanValue booleanValue =
            assertPattern(enabledPart, yajco.model.pattern.impl.BooleanValue.class);
        assertEquals("true", booleanValue.getTrueToken());
        assertEquals("false", booleanValue.getFalseToken());
    }

    @Test
    public void shouldMapCustomBooleanValuePatternFromAnnotation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.Light",
            "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Light {\n"
                + "    public Light(@BooleanValue(trueToken = \"on\", falseToken = \"off\") boolean switchedOn) {\n"
                + "    }\n"
                + "}\n");

        Concept concept = requireConcept(language, "Light");
        Notation notation = requireSingleNotation(concept);
        PropertyReferencePart switchedOnPart = assertNotationPart(notation, 0, PropertyReferencePart.class);
        yajco.model.pattern.impl.BooleanValue booleanValue =
            assertPattern(switchedOnPart, yajco.model.pattern.impl.BooleanValue.class);
        assertEquals("on", booleanValue.getTrueToken());
        assertEquals("off", booleanValue.getFalseToken());
    }

    @Test
    public void shouldMapBooleanValueTokenArraysFromAnnotation() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.Light",
            "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Light {\n"
                + "    public Light(@BooleanValue(trueToken = {\"on\", \"true\"}, falseToken = {\"off\", \"false\"}) boolean switchedOn) {\n"
                + "    }\n"
                + "}\n");

        Concept concept = requireConcept(language, "Light");
        Notation notation = requireSingleNotation(concept);
        PropertyReferencePart switchedOnPart = assertNotationPart(notation, 0, PropertyReferencePart.class);
        yajco.model.pattern.impl.BooleanValue booleanValue =
            assertPattern(switchedOnPart, yajco.model.pattern.impl.BooleanValue.class);
        assertArrayEquals(new String[] {"on", "true"}, booleanValue.getTrueTokens());
        assertArrayEquals(new String[] {"off", "false"}, booleanValue.getFalseTokens());
    }

    @Test
    public void shouldMapFlagAnnotationToBooleanValuePattern() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.Member",
            "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Member {\n"
                + "    public Member(@Flag(\"final\") boolean isFinal, String name) {\n"
                + "    }\n"
                + "}\n");

        Concept concept = requireConcept(language, "Member");
        Notation notation = requireSingleNotation(concept);
        PropertyReferencePart isFinalPart = assertNotationPart(notation, 0, PropertyReferencePart.class);
        yajco.model.pattern.impl.BooleanValue booleanValue =
            assertPattern(isFinalPart, yajco.model.pattern.impl.BooleanValue.class);
        assertEquals("final", booleanValue.getTrueToken());
        assertEquals("", booleanValue.getFalseToken());
    }

    @Test
    public void shouldRejectBooleanValuePatternWithBothTokensEmpty() throws Exception {
        String diagnostics = compiler.compileExpectingFailure(
            source("test.InvalidFlag",
                "package test;\n"
                    + "import yajco.annotation.*;\n"
                    + "import yajco.annotation.config.*;\n"
                    + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                    + "public class InvalidFlag {\n"
                    + "    public InvalidFlag(@BooleanValue(trueToken = \"\", falseToken = \"\") boolean flag) {\n"
                    + "    }\n"
                    + "}\n"));

        assertTrue(diagnostics.contains("Boolean value pattern must define at least one non-empty token"));
    }

    @Test
    public void shouldIncludeFactoryMethodAndIgnoreExcludedConstructor() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.Statement",
            "package test;\n"
                + "import yajco.annotation.*;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public class Statement {\n"
                + "    @Exclude\n"
                + "    public Statement() {\n"
                + "    }\n"
                + "    @FactoryMethod\n"
                + "    @Before(\"noop\")\n"
                + "    public static Statement noop() {\n"
                + "        return new Statement();\n"
                + "    }\n"
                + "}\n");

        Concept statement = requireConcept(language, "Statement");
        Notation notation = requireSingleNotation(statement);
        assertEquals(1, notation.getParts().size());
        assertTokenPart(notation, 0, "noop");

        Factory factory = assertPattern(notation, Factory.class);
        assertEquals("noop", factory.getName());
    }

    private static @NonNull Concept requireConcept(Language language, String name) {
        Concept concept = language.getConcept(name);
        assertNotNull("Concept '" + name + "' not found", concept);
        return concept;
    }

    private void assertToken(Language language, String name, String regexp) {
        TokenDef token = language.getToken(name);
        assertNotNull("Expected token " + name, token);
        assertEquals(regexp, token.getRegexp());
    }

    private void assertSkip(Language language, String regexp) {
        for (SkipDef skip : language.getSkips()) {
            if (regexp.equals(skip.getRegexp())) {
                return;
            }
        }
        throw new AssertionError("Expected skip " + regexp + " in " + language.getSkips());
    }

    private static Notation requireSingleNotation(Concept concept) {
        assertEquals(1, concept.getConcreteSyntax().size());
        return concept.getConcreteSyntax().get(0);
    }

    private static <T extends NotationPart> T assertNotationPart(Notation notation, int index, Class<T> expectedType) {
        NotationPart notationPart = notation.getParts().get(index);
        assertTrue("Expected notation part type " + expectedType.getSimpleName() + " but was "
            + notationPart.getClass().getSimpleName(), expectedType.isInstance(notationPart));
        return expectedType.cast(notationPart);
    }

    private static void assertTokenPart(Notation notation, int index, String token) {
        TokenPart tokenPart = assertNotationPart(notation, index, TokenPart.class);
        assertEquals(token, tokenPart.getToken());
    }

    private static <P extends Pattern, S extends PatternSupport<P>, T extends P> T assertPattern(
        S patternSupport,
        Class<T> patternClass
    ) {
        T pattern = patternSupport.getPattern(patternClass);
        assertNotNull("Expected pattern " + patternClass.getSimpleName(), pattern);
        return pattern;
    }

    private static void assertReferenceToConcept(Concept concept, Type type) {
        assertEquals(ReferenceType.class, type.getClass());
        assertEquals(concept, ((ReferenceType) type).getConcept());
    }

    @Test
    public void shouldReportErrorForSkipBlockCommentWithWrongNumberOfElements() throws Exception {
        List<javax.tools.Diagnostic<? extends javax.tools.JavaFileObject>> errors =
            compiler.compileExpectingErrors(
                source("test.robot.Robot",
                    "package test.robot;\n"
                        + "import yajco.annotation.config.*;\n"
                        + "@Parser(\n"
                        + "    skips = { @Skip(blockComment={\"/*\"}) },\n"
                        + "    options = @Option(name = \"yajco.generateParser\", value = \"false\")\n"
                        + ")\n"
                        + "public class Robot {\n"
                        + "    public Robot() {}\n"
                        + "}\n"));

        assertTrue("Expected at least one error", errors.size() >= 1);
        boolean found = errors.stream().anyMatch(d ->
            d.getMessage(java.util.Locale.ROOT).contains("blockComment must have exactly 2 elements"));
        assertTrue("Expected error about blockComment element count, got: " + errors, found);
    }
}
