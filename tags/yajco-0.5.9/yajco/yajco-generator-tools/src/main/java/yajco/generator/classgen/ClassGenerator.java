package yajco.generator.classgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import yajco.GeneratorHelper;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.BindingNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.SkipDef;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.model.pattern.ConceptPattern;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PropertyPattern;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Factory;
import yajco.model.pattern.impl.Identifier;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Range;
import yajco.model.pattern.impl.References;
import yajco.model.pattern.impl.Separator;
import yajco.model.pattern.impl.Token;
import yajco.model.pattern.impl.printer.Indent;
import yajco.model.pattern.impl.printer.NewLine;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;
import yajco.model.type.Type;

public class ClassGenerator implements FilesGenerator {
    private static final String PROPERTY_ENABLER = "class";

    private Language actualLanguage;

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        String option = properties.getProperty(GeneratorHelper.GENERATE_TOOLS_KEY, "").toLowerCase();
        if ( !option.contains("all") && !option.contains(PROPERTY_ENABLER)) {
            System.out.println(getClass().getCanonicalName()+": Classes not generated - property disabled (set "+GeneratorHelper.GENERATE_TOOLS_KEY+" to '"+PROPERTY_ENABLER+"' or 'all')");
            return;
        } 
        if (language == null) {
            throw new NullPointerException("Parameter language is NULL.");
        }
        actualLanguage = language;

        generatePackageInfoFile(filer);
        generateConceptFiles(filer);
    }

    private void generateConceptFiles(Filer filer) throws GeneratorException {
        Writer writer = null;
        for (Concept concept : actualLanguage.getConcepts()) {
            try {
                String filePath = yajco.model.utilities.Utilities.getFullConceptClassName(actualLanguage, concept);
//                filePath = filePath.replace('.', File.separatorChar);
//                filePath = filePath + ".java";
//                String path = filePath.substring(0, filePath.lastIndexOf(File.separatorChar));
//                File file = new File(directory, path);
//                if (!file.exists()) {
//                    if (!file.mkdirs()) {
//                        throw new GeneratorException("Cannot create directory: " + file.getAbsolutePath());
//                    }
//                }
//                file = new File(directory, filePath);
                JavaFileObject fileObject = filer.createSourceFile(filePath);
                
                writer = fileObject.openWriter();
                generate(concept, writer);
                try {
                    Utilities.formatCode(new File(new URI("file:///").resolve(fileObject.toUri())));
                } catch (URISyntaxException e) {
                    System.err.println("Cannot create absolute path for file "+fileObject.toUri().toString());
                    throw new GeneratorException("Cannot create absolute URI for file "+fileObject.toUri().toString(), e);
                }
                System.out.println("Success generating file: " + filePath);
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing file for concept " + concept.getConceptName(), ex);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException ex) {
                    throw new GeneratorException("Problem writing file for concept " + concept.getConceptName(), ex);
                }
            }
        }
    }

    private void generatePackageInfoFile(Filer filer) {
        Writer writer = null;
        try {
            JavaFileObject fileObject = filer.createSourceFile(yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage)
                                                      + ".package-info");
//            File packageInfoFile = new File(directory,
//                    yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage).replace('.', File.separatorChar)
//                    + File.separatorChar + "package-info.java");
            writer = fileObject.openWriter();
            generatePackageInfo(writer);
            //Jalopy odstranuje anotaciu @Parser, zatial som dal formatovanie priamo do predchadzajucej metody, je to lahke formatovanie
            //Utilities.formatCode(packageInfoFile);
            System.out.println("Success generating file: " + fileObject.getName());
        } catch (IOException ex) {
            throw new GeneratorException("Problem writing file for package-info.java", ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing file for package-info.java", ex);
            }
        }
    }

    private void generatePackageInfo(Writer writer) {
        try {
            writer.write("@" + yajco.annotation.config.Parser.class.getSimpleName());
            writer.write("(\n");
            writer.write("    className = \"" + yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage) + ".parser.Parser\",\n");
            writer.write("    mainNode = \"" + yajco.model.utilities.Utilities.getFullConceptClassName(actualLanguage, actualLanguage.getConcepts().get(0)) + "\",\n");
            //Tokens
            writer.write("    tokens = ");
            writer.write("{\n");
            boolean comma = false;
            for (TokenDef tokenDef : actualLanguage.getTokens()) {
                if (comma) {
                    writer.write(",\n");
                }
                writer.write("        @TokenDef(name = \"" + tokenDef.getName() + "\", regexp = \"" + tokenDef.getRegexp() + "\")");
                comma = true;
            }
            writer.write("\n    },\n");
            //Skips
            writer.write("    skips = ");
            writer.write("{\n");
            comma = false;
            for (SkipDef skip : actualLanguage.getSkips()) {
                if (comma) {
                    writer.write(",\n");
                }
                writer.write("        @Skip(\"" + skip.getRegexp() + "\")");
                comma = true;
            }
            writer.write("\n    }\n");
            writer.write(")\n");
            //Package + imports
            writer.write("package " + yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage) + ";\n\n");
            writer.write("import " + yajco.annotation.config.Parser.class.getCanonicalName() + ";\n");
            writer.write("import " + yajco.annotation.config.TokenDef.class.getCanonicalName() + ";\n");
            writer.write("import " + yajco.annotation.config.Skip.class.getCanonicalName() + ";\n");

            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Error writing package-info.java", ex);
        }
    }

    private void generate(Concept concept, Writer writer) {
        try {
            writePackage(concept, writer);
            writeImports(concept, writer);
            writeClassBody(concept, writer);

            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Error writing class for concept " + concept.getConceptName() + ".", ex);
        }
    }

    private void writeClassBody(Concept concept, Writer writer) throws IOException {
        for (ConceptPattern conceptPattern : concept.getPatterns()) {
            if (conceptPattern instanceof yajco.model.pattern.impl.Enum) {
                writer.write("public enum " + concept.getConceptName() + " ");
                writer.write("{");
                boolean comma = false;
                for (Notation notation : concept.getConcreteSyntax()) {
                    for (NotationPart notationPart : notation.getParts()) {
                        if (notationPart instanceof TokenPart) {
                            if (comma) {
                                writer.write(", ");
                            }
                            writer.write(((TokenPart) notationPart).getToken());
                            comma = true;
                        }
                    }
                }
                writer.write("}");
                return;
            } else if (!(conceptPattern instanceof Operator)) {  //Operator sa pise ku konstruktorom aktualne
                writePattern(writer, conceptPattern);
            }
        }
        writer.write("public class " + concept.getConceptName() + " ");
        if (concept.getParent() != null) {
            writer.write("extends " + concept.getParent().getConceptName());
        }
        writer.write(" {");
        writePrivateFields(concept, writer);
        writeConstructors(concept, writer);
        writeGettersSetters(concept, writer);
        writer.write("}");
    }

    private void writeConstructorBody(Writer writer, Notation notation) throws IOException {
        Factory factory = (Factory) notation.getPattern(Factory.class);
        writer.write("{");
        if (factory != null) {
            //TODO: body not generated for factory method
            System.err.println("YAJCo cannot automatically generate method body for factory method \""+factory.getName()+"\" !!!");
            writer.write("\n//TODO: Implement factory method!!!\n");
            writer.write("throw new UnsupportedOperationException(\"Factory method "+factory.getName()+" not implemented.\");");
        } else { // classic constructor
            for (NotationPart notationPart : notation.getParts()) {
                if (notationPart instanceof PropertyReferencePart) {
                    PropertyReferencePart part = (PropertyReferencePart) notationPart;
                    writer.write("this." + part.getProperty().getName() + " = " + part.getProperty().getName() + ";");
                }
            }
        }
        writer.write("}");
    }

    private void writeConstructorDefinition(Concept concept, Writer writer, Notation notation) throws IOException {
        ConceptPattern operator = concept.getPattern(Operator.class);
        if (operator != null) {
            writePattern(writer, operator);
        }
        Factory factory = (Factory) notation.getPattern(Factory.class);

        List<String> list = new ArrayList<String>();
        boolean separate = false;
        for (NotationPart notationPart : notation.getParts()) {
            if (notationPart instanceof TokenPart) {
                list.add(((TokenPart) notationPart).getToken());
            } else {
                list.clear();
            }
        }
        if (!list.isEmpty()) {
            writer.write("@After(");
            writeStringListContent(list, writer);
            writer.write(")");
            list.clear();
        }
        writer.write("public ");
        if (factory == null) { // create classic constructor
            writer.write(concept.getConceptName());
        } else {    // create factory method
            writer.write("static " + concept.getConceptName() + " " + factory.getName());
        }

        writer.write("(");

        for (NotationPart notationPart : notation.getParts()) {
            if (notationPart instanceof TokenPart) {
                list.add(((TokenPart) notationPart).getToken());
            } else {
                if (separate) {
                    writer.write(", ");
                }
                if (!list.isEmpty()) {
                    writer.write("@Before(");
                    writeStringListContent(list, writer);
                    writer.write(") ");
                    list.clear();
                }
                if (notationPart instanceof BindingNotationPart) {
                    BindingNotationPart bindPart = (BindingNotationPart) notationPart;
                    for (NotationPartPattern notationPartPattern : bindPart.getPatterns()) {
                        writePattern(writer, notationPartPattern);
                    }
                    if (notationPart instanceof PropertyReferencePart) {
                        PropertyReferencePart part = (PropertyReferencePart) notationPart;
                        writer.write(Utilities.getTypeName(part.getProperty().getType()) + " " + part.getProperty().getName());
                    } else if (notationPart instanceof LocalVariablePart) {
                        LocalVariablePart part = (LocalVariablePart) notationPart;
                        writer.write(Utilities.getTypeName(part.getType()) + " " + part.getName());
                    }
                    separate = true;
                }
            }
        }
        writer.write(")");
    }

    private void writeGettersSetters(Concept concept, Writer writer) throws IOException {
        for (Property property : concept.getAbstractSyntax()) {
            Type type = property.getType();
            writer.write("public ");
            writer.write(Utilities.getTypeName(type) + " ");
            writer.write("get" + Utilities.toUpperCaseIdent(property.getName()) + "() {");
            writer.write("return " + property.getName() + ";");
            writer.write("}");
            writer.write("public void ");
            writer.write("set" + Utilities.toUpperCaseIdent(property.getName()) + "(" + Utilities.getTypeName(type) + " " + property.getName() + ") {");
            writer.write("this." + property.getName() + " = " + property.getName() + ";");
            writer.write("}");
        }
    }

    private void writePrivateFields(Concept concept, Writer writer) throws IOException {
        for (Property property : concept.getAbstractSyntax()) {
            for (PropertyPattern propertyPattern : property.getPatterns()) {
                writePattern(writer, propertyPattern);
            }
            writer.write("private ");
            Type type = property.getType();
            writer.write(Utilities.getTypeName(type) + " ");
            writer.write(property.getName() + ";");
        }
    }

    private void writeImports(Concept concept, Writer writer) throws IOException {
        boolean listType = false;
        boolean setType = false;
        Set<Concept> importConcepts = new HashSet<Concept>();
        for (Property property : concept.getAbstractSyntax()) {
            Type type = property.getType();
            processTypeToConceptSet(concept, type, importConcepts);
            if (type instanceof ListType) {
                listType = true;
            } else if (type instanceof SetType) {
                setType = true;
            }
        }
        //importConcepts.remove(concept);
        for (Concept importConcept : importConcepts) {
            writeConceptImport(writer, importConcept);
        }
        if (listType) {
            writer.write("import java.util.List;");
        }
        if (setType) {
            writer.write("import java.util.Set;");
        }
        writer.write("import yajco.annotation.*;");
        writer.write("import yajco.annotation.reference.*;");
        writer.write("import yajco.annotation.printer.*;");
        writer.write("import yajco.model.pattern.impl.Associativity;");
    }

    private void processTypeToConceptSet(Concept actualConcept, Type type, Set<Concept> conceptList) {
        if (type instanceof ComponentType) {
            processTypeToConceptSet(actualConcept, ((ComponentType) type).getComponentType(), conceptList);
        } else if (type instanceof ReferenceType) {
            Concept referencedConcept = ((ReferenceType) type).getConcept();
            if (!referencedConcept.getSubPackage().equals(actualConcept.getSubPackage())) {
                conceptList.add(referencedConcept);
            }
        }
    }

    private void writeConceptImport(Writer writer, Concept concept) throws IOException {
        writer.write("import " + yajco.model.utilities.Utilities.getFullConceptClassName(actualLanguage, concept) + ";");
    }

    private void writePackage(Concept concept, Writer writer) throws IOException {
        String packageName;
        if (concept.getSubPackage().isEmpty()) {
            packageName = yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage);
        } else {
            packageName = yajco.model.utilities.Utilities.getLanguagePackageName(actualLanguage) + "." + concept.getSubPackage();
        }
        writer.write("package " + packageName + ";");
    }

    private void writeConstructors(Concept concept, Writer writer) throws IOException {
        for (Notation notation : concept.getConcreteSyntax()) {
            writeConstructorDefinition(concept, writer, notation);
            writeConstructorBody(writer, notation);
        }
    }

    private void writeStringListContent(List<String> list, Writer writer) throws IOException {
        if (list.size() > 1) {
            writer.write("{");
        }
        boolean comma = false;
        for (String string : list) {
            if (comma) {
                writer.write(",");
            }
            writer.write("\"" + string + "\"");
            comma = true;
        }
        if (list.size() > 1) {
            writer.write("}");
        }
    }

    private void writePattern(Writer writer, Pattern pattern) throws IOException {
        if (pattern instanceof Identifier) {
            Identifier ident = (Identifier) pattern;
            writer.write("@Identifier");
            if (ident.getUnique() != null && !ident.getUnique().isEmpty()) {
                writer.write(("(unique=\"" + ident.getUnique() + "\")"));
            }
        } else if (pattern instanceof Operator) {
            Operator operator = (Operator) pattern;
            writer.write("@Operator(priority = " + operator.getPriority());
            if (operator.getAssociativity() != null && operator.getAssociativity() != Associativity.AUTO) {
                writer.write(", associativity = Associativity." + operator.getAssociativity().name());
            }
            writer.write(")");
        } else if (pattern instanceof Parentheses) {
            writer.write("@Parentheses");
        } else if (pattern instanceof Range) {
            Range range = (Range) pattern;
            writer.write("@Range(minOccurs = " + range.getMinOccurs());
            if (range.getMaxOccurs() != Range.INFINITY) {
                writer.write(", maxOccurs = " + range.getMaxOccurs());
            }
            writer.write(")");
        } else if (pattern instanceof References) {
            References references = (References) pattern;
            writer.write("@References(value = " + references.getConcept().getConceptName() + ".class");
            if (references.getProperty() != null) {
                writer.write(", field = \"" + references.getProperty().getName() + "\"");
            }
            writer.write(")");
        } else if (pattern instanceof Separator) {
            Separator separator = (Separator) pattern;
            writer.write("@Separator(\"" + separator.getValue() + "\"");
            writer.write(")");
        } else if (pattern instanceof Indent) {
            writer.write("@Indent");
        } else if (pattern instanceof NewLine) {
            writer.write("@NewLine");
        } else if(pattern instanceof Token) {
            Token token = (Token)pattern;
            writer.write("@Token(\"" + token.getName() + "\")");
        } else {
            throw new GeneratorException("Not known pattern type: " + pattern.getClass().getCanonicalName());
        }
        writer.write(" ");
    }
}
