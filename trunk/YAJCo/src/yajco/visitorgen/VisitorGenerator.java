package yajco.visitorgen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import tuke.pargen.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.model.type.ArrayType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;

public class VisitorGenerator {
    protected static final String TEMPLATE_PACKAGE = "templates";

    private final String template;

    protected final VelocityEngine velocityEngine = new VelocityEngine();

    public VisitorGenerator() {
        template = "Visitor.java.vm";
    }

    public void generate(Language language, Writer writer) throws GeneratorException {
        try {
            InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(TEMPLATE_PACKAGE + "/" + template), "utf-8");
            VelocityContext context = new VelocityContext();
            context.put("Utilities", Utilities.class);
            context.put("language", language);
            context.put("arrayTypeClassName", ArrayType.class.getCanonicalName());
            context.put("referenceTypeClassName", ReferenceType.class.getCanonicalName());
            context.put("primitiveTypeClassName", PrimitiveType.class.getCanonicalName());
            velocityEngine.evaluate(context, writer, "", reader);
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate visitor class",ex);
        }
    }

}
