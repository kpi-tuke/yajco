package yajco.annotation.processor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import yajco.model.*;

import static org.junit.Assert.*;
import static yajco.annotation.processor.AnnotationProcessorTestCompiler.source;

public class AnnotationProcessorLanguageModelTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AnnotationProcessorTestCompiler compiler;

    @Before
    public void setUp() {
        compiler = new AnnotationProcessorTestCompiler(temporaryFolder);
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

        Concept concept = language.getConcept("NumberLiteral");
        assertNotNull(concept);

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
    public void multipleConceptsAreSupported() throws Exception {
        Language language = compiler.compileAndReadLanguageModel(
            source("test.expr.Expression",
                "package test.expr;\n"
                + "import yajco.annotation.config.Option;\n"
                + "import yajco.annotation.config.Parser;\n"
                + "@Parser(options = @Option(name = \"yajco.generateParser\", value = \"false\"))\n"
                + "public abstract class Expression {}\n"),

            source("test.expr.NumberLiteral",
                "package test.expr;\n"
                + "import yajco.annotation.Token;\n"
                + "public class NumberLiteral extends Expression {\n"
                + "    public NumberLiteral(@Token(\"INT\") int value) {}\n"
                + "}\n"),

            source("test.expr.Add",
                "package test.expr;\n"
                + "import yajco.annotation.Before;\n"
                + "public class Add extends Expression {\n"
                + "    public Add(Expression left, @Before(\"+\") Expression right) {}\n"
                + "}\n")
        );

        assertNotNull(language.getConcept("Expression"));
        assertNotNull(language.getConcept("NumberLiteral"));
        assertNotNull(language.getConcept("Add"));
        assertEquals("Expression", language.getConcept("Add").getParent().getName());
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
}
