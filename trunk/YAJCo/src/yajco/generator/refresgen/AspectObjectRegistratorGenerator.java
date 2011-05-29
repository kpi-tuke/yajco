package yajco.generator.refresgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import yajco.generator.GeneratorException;
import yajco.ReferenceResolver;
import yajco.generator.FilesGenerator;
import yajco.generator.util.Utilities;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.PropertyReferencePart;
import yajco.model.type.Type;

public class AspectObjectRegistratorGenerator implements FilesGenerator {

    private static String CLASS_NAME = "ObjectRegistrator";
    private static String PACKAGE_NAME = "parser";

    private Map<Concept, String> nameMap;

    public void generateFiles(Language language, File directory) {
        if (directory == null || !directory.isDirectory()) {
            throw new GeneratorException("Supplied parameter is not directory ("+directory.getAbsolutePath()+").");
        }
        generateAspectFile(language, directory);
        generateAspectConfigFile(language, directory);
    }

    private void generateAspectFile(Language language, File directory) {
        FileWriter writer = null;
        try {
            String filePath = Utilities.getLanguagePackageName(language)+"."+PACKAGE_NAME+ "." + CLASS_NAME;
            filePath = filePath.replace('.', File.separatorChar);
            filePath = filePath + ".java";
            File file = new File(directory, filePath);
            Utilities.createDirectories(file,false);
            writer = new FileWriter(file);
            generateAspectFile(language, writer);
            Utilities.formatCode(file);
        } catch (IOException ex) {
            throw new GeneratorException("Problem writing aspect java file.", ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing aspect java file.", ex);
            }
        }
    }

    private void generateAspectConfigFile(Language language, File directory) {
        FileWriter writer = null;
        try {
            String path = "META-INF";
            String filePath = path + File.separatorChar + "aop.xml";
            File file = new File(directory, filePath);
            Utilities.createDirectories(file,false);
            writer = new FileWriter(file);
            writer.write("<aspectj>\n");
            writer.write("<aspects>\n");
            writer.write("<aspect name=\"" + Utilities.getLanguagePackageName(language) + ".parser." + CLASS_NAME + "\"/>\n");
            writer.write("</aspects>\n");
            writer.write("</aspectj>");
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Problem writing aspect configuration file.", ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing aspect configuration file.", ex);
            }
        }
    }

    private void generateAspectFile(Language language, Writer writer) {
        try {
            nameMap = Utilities.createConceptUniqueNames(language);
            generatePackageName(language, writer);
            generateImports(language, writer);
            generateClass(language, writer);
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Problem generating ReferenceResolver class.", ex);
        }
    }

    private void generatePackageName(Language language, Writer writer) throws IOException {
        String packageName;
        packageName = Utilities.getLanguagePackageName(language) + ".parser";
        writer.write("package "+packageName+";");
    }

    private void generateImports(Language language, Writer writer) throws IOException {
        for (Concept concept : Utilities.getConceptsNeededForImport(nameMap)) {
            writer.write("import " + Utilities.getFullConceptClassName(language, concept) + ";");
        }
        writer.write("import " + Pointcut.class.getCanonicalName() + ";");
        writer.write("import " + After.class.getCanonicalName() + ";");
        writer.write("import " + Aspect.class.getCanonicalName() + ";");
        writer.write("import " + List.class.getCanonicalName() + ";");
        writer.write("import " + Set.class.getCanonicalName() + ";");
    }

    private void generateClass(Language language, Writer writer) throws IOException {
        generateClassDefinition(language, writer);
        writer.write("{");
        generatePointcuts(language, writer);
        writer.write("}");
    }

    private void generateClassDefinition(Language language, Writer writer) throws IOException {
        writer.write("@"+Aspect.class.getSimpleName()+" ");
        writer.write("public class " + CLASS_NAME);
    }

    private void generatePointcuts(Language language, Writer writer) throws IOException {
        String initPointcut = "initializeModelObject";
        List<NotationParameter> parameterList;
        writer.write("@" + Pointcut.class.getSimpleName());
        writer.write("(\"");
        writer.write("initialization(new(..))");
        writer.write("\")");
        writer.write("public void " + initPointcut + "() {}");

        for (Concept concept : language.getConcepts()) {
            for (Notation notation : concept.getConcreteSyntax()) {
                parameterList = getParameterList(notation);
                writer.write("@" + After.class.getSimpleName());
                writer.write("(\"");
                writer.write(initPointcut);
                writer.write(" && ");
                writer.write("target(" + Utilities.toLowerCaseIdent(concept.getConceptName()) + ")");
                writer.write(" && ");
                writer.write("args(");
                boolean separate = false;
                for (NotationParameter parameter : parameterList) {
                    if (separate) {
                        writer.write(", ");
                    }
                    writer.write(parameter.getName());
                    separate = true;
                }
                writer.write(")");
                writer.write("\")");
                writer.write("public void register" + Utilities.getMethodPartName(nameMap, concept) + concept.getConcreteSyntax().indexOf(notation));
                writer.write("(");
                writer.write(Utilities.getClassName(nameMap, concept) + " " + Utilities.toLowerCaseIdent(concept.getConceptName()));
                for (NotationParameter parameter : parameterList) {
                    writer.write(", ");
                    writer.write(parameter.getTypeName() + " " + parameter.getName());
                }
                writer.write(")");
                writer.write("{");
                writer.write(ReferenceResolver.class.getCanonicalName() + ".getInstance().register(");
                writer.write(Utilities.toLowerCaseIdent(concept.getConceptName()));
                for (NotationParameter parameter : parameterList) {
                    writer.write(", ");
                    writer.write("(Object)"); // pretypovanie
                    writer.write(parameter.getName());
                }
                writer.write(");");
                writer.write("}");
            }
        }

    }

    private List<NotationParameter> getParameterList(Notation notation) {
        List<NotationParameter> list = new ArrayList<NotationParameter>();
        for (NotationPart notationPart : notation.getParts()) {
            if (notationPart instanceof PropertyReferencePart) {
                PropertyReferencePart part = (PropertyReferencePart) notationPart;
                list.add(new NotationParameter(part.getProperty().getType(), part.getProperty().getName()));
            } else if (notationPart instanceof LocalVariablePart) {
                LocalVariablePart part = (LocalVariablePart) notationPart;
                list.add(new NotationParameter(part.getType(), part.getName()));
            }
        }
        return list;
    }

    private class NotationParameter {

        private Type type;
        private String name;

        public NotationParameter(Type type, String name) {
            this.type = type;
            this.name = name;
        }

        /**
         * @return the type
         */
        public Type getType() {
            return type;
        }

        public String getTypeName() {
            return Utilities.getTypeName(type);
        }

        /**
         * @return the name
         */
        public String getName() {
            return "p_"+name;
        }
    }
}
