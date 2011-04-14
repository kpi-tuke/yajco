package yajco.visitorgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.generator.GeneratorException;
import yajco.generator.AbstractFileGenerator;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.model.type.ArrayType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;

public class VisitorGenerator extends AbstractFileGenerator{
    protected static final String TEMPLATE_PACKAGE = "templates";
    protected static final String VISITOR_PACKAGE = "visitor";
    protected static final String VISITOR_CLASS_NAME = "Visitor";

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
            context.put("package", getPackageName());
            context.put("className", getClassName());
            context.put("arrayTypeClassName", ArrayType.class.getCanonicalName());
            context.put("listTypeClassName", ListType.class.getCanonicalName());
            context.put("setTypeClassName", SetType.class.getCanonicalName());
            context.put("referenceTypeClassName", ReferenceType.class.getCanonicalName());
            context.put("primitiveTypeClassName", PrimitiveType.class.getCanonicalName());
            velocityEngine.evaluate(context, writer, "", reader);
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate visitor class",ex);
        }
    }

    @Override
    public String getPackageName() {
        return VISITOR_PACKAGE;
}

    @Override
    public String getFileName() {
        return getClassName()+".java";
    }

    @Override
    public String getClassName() {
        return VISITOR_CLASS_NAME;
    }


}
