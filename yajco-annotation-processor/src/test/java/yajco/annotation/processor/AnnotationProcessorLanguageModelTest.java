package yajco.annotation.processor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import yajco.model.*;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.Separator;
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
    public void createsLanguageModelFromAnnotatedParserClass() throws Exception {
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

        assertEquals(1, concept.getConcreteSyntax().size());
        Notation notation = concept.getConcreteSyntax().get(0);
        assertEquals(2, notation.getParts().size());
        assertTokenPart(notation.getParts().get(0), "number");
        assertTrue(notation.getParts().get(1) instanceof PropertyReferencePart);
        assertEquals("value", ((PropertyReferencePart) notation.getParts().get(1)).getProperty().getName());
    }

    @Test
    public void languageAnnotationAddsMetadataAndCommentSkipsToModel() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            "test.robot.Robot",
            "package test.robot;\n"
                + "import yajco.annotation.config.*;\n"
                + "@Language(\n"
                + "    name = \"robot\",\n"
                + "    description = \"Robot commands\",\n"
                + "    version = \"1.0\",\n"
                + "    fileExtensions = {\".robot\"},\n"
                + "    lineComment = \"//\",\n"
                + "    blockComment = {\"/*\", \"*/\"}\n"
                + ")\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
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
    public void inheritanceRelationIsRecognized() throws Exception {
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
    public void compositionRelationIsRecognized() throws Exception {
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
    public void compositionArrayMultiplicityIsRecognized() throws Exception {
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
    public void compositionListMultiplicityIsRecognized() throws Exception {
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
    public void compositionSetMultiplicityIsRecognized() throws Exception {
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
    public void beforeAndAfterOnConcept() throws Exception {
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
        assertEquals(1, number.getConcreteSyntax().size());
        Notation notation = number.getConcreteSyntax().get(0);
        assertEquals(3, notation.getParts().size());
        assertTokenPart(notation.getParts().get(0), "begin");
        assertTrue(notation.getParts().get(1) instanceof PropertyReferencePart);
        assertTokenPart(notation.getParts().get(2), "end");
    }

    @Test
    public void beforeAndAfterOnProperty() throws Exception {
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
        assertEquals(1, number.getConcreteSyntax().size());
        Notation notation = number.getConcreteSyntax().get(0);
        assertEquals(3, notation.getParts().size());
        assertTokenPart(notation.getParts().get(0), "begin");
        assertTrue(notation.getParts().get(1) instanceof PropertyReferencePart);
        assertTokenPart(notation.getParts().get(2), "end");
    }

    @Test
    public void beforeAndAfterOnConceptAndProperties() throws Exception {
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
        assertEquals(1, number.getConcreteSyntax().size());
        Notation notation = number.getConcreteSyntax().get(0);
        assertEquals(8, notation.getParts().size());
        assertTokenPart(notation.getParts().get(0), "begin");
        assertTokenPart(notation.getParts().get(1), "beforeFirst");
        assertTrue(notation.getParts().get(2) instanceof PropertyReferencePart);
        assertTokenPart(notation.getParts().get(3), "afterFirst");
        assertTokenPart(notation.getParts().get(4), "beforeSecond");
        assertTrue(notation.getParts().get(5) instanceof PropertyReferencePart);
        assertTokenPart(notation.getParts().get(6), "afterSecond");
        assertTokenPart(notation.getParts().get(7), "end");
    }

    @Test
    public void separator() throws Exception {
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
        Notation notation = list.getConcreteSyntax().get(0);
        NotationPart notationPart = notation.getParts().get(0);
        assertTrue(notationPart instanceof PropertyReferencePart);
        List<NotationPartPattern> patterns = ((PropertyReferencePart)notationPart).getPatterns();
        assertEquals(1, patterns.size());
        assertTrue(patterns.get(0) instanceof Separator);
        assertEquals(",", ((Separator) patterns.get(0)).getValue());
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

    private void assertTokenPart(NotationPart notationPart, String token) {
        assertTrue(notationPart instanceof TokenPart);
        assertEquals(token, ((TokenPart) notationPart).getToken());
    }

    private static void assertReferenceToConcept(Concept concept, Type type) {
        assertEquals(ReferenceType.class, type.getClass());
        assertEquals(concept, ((ReferenceType) type).getConcept());
    }
}
