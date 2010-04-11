package yajco.printergen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import tuke.pargen.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenPart;
import yajco.model.pattern.PatternSupport;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Range;
import yajco.model.pattern.impl.References;
import yajco.model.pattern.impl.Separator;
import yajco.model.pattern.impl.printer.Indent;
import yajco.model.pattern.impl.printer.NewLine;
import yajco.model.type.ArrayType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;

public class PrettyPrinterGenerator {

    protected static final String TEMPLATE_PACKAGE = "templates";
    private final String template;
    protected final VelocityEngine velocityEngine = new VelocityEngine();

    public PrettyPrinterGenerator() {
        template = "Printer.java.vm";
    }

    public void generate(Language language, Writer writer) throws GeneratorException {
        try {
            InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(TEMPLATE_PACKAGE + "/" + template), "utf-8");
            VelocityContext context = new VelocityContext();
            context.put("Utilities", Utilities.class);
            context.put("language", language);
            context.put("arrayTypeClassName", ArrayType.class.getCanonicalName());
            context.put("listTypeClassName", ListType.class.getCanonicalName());
            context.put("setTypeClassName", SetType.class.getCanonicalName());
            context.put("referenceTypeClassName", ReferenceType.class.getCanonicalName());
            context.put("primitiveTypeClassName", PrimitiveType.class.getCanonicalName());
            context.put("tokenPartClassName", TokenPart.class.getCanonicalName());
            context.put("propertyReferencePartClassName", PropertyReferencePart.class.getCanonicalName());
            context.put("localVariablePartClassName", LocalVariablePart.class.getCanonicalName());
            context.put("referencesPatternClassName", References.class.getCanonicalName());
            context.put("separatorPatternClassName", Separator.class.getCanonicalName());
            context.put("parenthesesPatternClassName", Parentheses.class.getCanonicalName());
            context.put("patternSupportClassName", PatternSupport.class.getCanonicalName());
            context.put("newLinePatternClassName", NewLine.class.getCanonicalName());
            context.put("indentPatternClassName", Indent.class.getCanonicalName());
            context.put("rangePatternClassName", Range.class.getCanonicalName());
            context.put("rangePatternInfinityValue", Range.INFINITY);

            velocityEngine.evaluate(context, writer, "", reader);

            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate visitor class", ex);
        }
    }

    public void generate(Language language, File file) {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            generate(language, writer);
            Utilities.formatCode(file);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write to file " + file.getAbsolutePath(), ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Cannot close writer for file " + file.getAbsolutePath(), ex);
            }
        }
    }
}
