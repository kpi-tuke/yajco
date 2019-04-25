package yajco.generator.visitorgen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.GeneratorHelper;
import yajco.generator.AbstractFileGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.Language;
import yajco.model.type.*;

public class VisitorGenerator extends AbstractFileGenerator {

    private static final String PROPERTY_ENABLER = "visitor";
    
    protected static final String TEMPLATE_PACKAGE = "templates";
    protected static final String VISITOR_PACKAGE = "visitor";
    protected static final String VISITOR_CLASS_NAME = "Visitor";
    private final String template;
    protected final VelocityEngine velocityEngine = new VelocityEngine();

    public VisitorGenerator() {
        template = "Visitor.java.vm";
    }

    @Override
    protected boolean shouldGenerate() {
        String option = properties.getProperty(GeneratorHelper.GENERATE_TOOLS_KEY, "").toLowerCase();
        if (!option.contains("all") && !option.contains(PROPERTY_ENABLER) && !option.contains("printer")) {
            System.out.println(getClass().getCanonicalName()+": Visitor not generated - property disabled (set "+GeneratorHelper.GENERATE_TOOLS_KEY+" to '"+PROPERTY_ENABLER+"' or 'all')");
            return false;
        }
        return true;
    }

    @Override
    public void generate(Language language, Writer writer) {
        try {
            InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(TEMPLATE_PACKAGE + "/" + template), "utf-8");
            VelocityContext context = new VelocityContext();
            context.put("Utilities", Utilities.class);
            context.put("ModelUtilities", yajco.model.utilities.Utilities.class);
            context.put("language", language);
            context.put("package", getPackageName());
            context.put("className", getClassName());
            context.put("arrayTypeClassName", ArrayType.class.getCanonicalName());
            context.put("listTypeClassName", ListType.class.getCanonicalName());
            context.put("setTypeClassName", SetType.class.getCanonicalName());
            context.put("referenceTypeClassName", ReferenceType.class.getCanonicalName());
            context.put("primitiveTypeClassName", PrimitiveType.class.getCanonicalName());
            context.put("optionalTypeClassName", OptionalType.class.getCanonicalName());
            velocityEngine.evaluate(context, writer, "", reader);
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate visitor class", ex);
        }
    }

    @Override
    public String getPackageName() {
        return VISITOR_PACKAGE;
    }

    @Override
    public String getFileName() {
        return getClassName() + ".java";
    }

    @Override
    public String getClassName() {
        return VISITOR_CLASS_NAME;
    }
}
