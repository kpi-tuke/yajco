package yajco.classgen;

import de.hunsicker.jalopy.Jalopy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tuke.pargen.GeneratorException;
import yajco.model.pattern.impl.Associativity;
import yajco.generator.util.Utilities;
import yajco.model.BindingNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.model.pattern.ConceptPattern;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.PropertyPattern;
import yajco.model.pattern.impl.Identifier;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Range;
import yajco.model.pattern.impl.References;
import yajco.model.pattern.impl.Separator;
import yajco.model.pattern.impl.printer.Indent;
import yajco.model.pattern.impl.printer.NewLine;
import yajco.model.type.ArrayType;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;
import yajco.model.type.Type;

public class ClassGenerator {

    private Language actualLanguage;

    public void generate(Language language, File directory) {
        if (directory == null || !directory.isDirectory()) {
            throw new GeneratorException("Supplied parameter is not directory.");
        }
        actualLanguage = language;

        generatePackageInfoFile(directory);
        generateConceptFiles(directory);
    }

    private void generateConceptFiles(File directory) throws GeneratorException {
        FileWriter writer = null;
        for (Concept concept : actualLanguage.getConcepts()) {
            try {
                String filePath = Utilities.getFullConceptClassName(actualLanguage, concept);
                filePath = filePath.replace('.', File.separatorChar);
                filePath = filePath + ".java";
                String path = filePath.substring(0, filePath.lastIndexOf(File.separatorChar));
                File file = new File(directory, path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        throw new GeneratorException("Cannot create directory: " + file.getAbsolutePath());
                    }
                }
                file = new File(directory, filePath);
                writer = new FileWriter(file);
                generate(concept, writer);
                Utilities.formatCode(file);
                System.out.println("Success generating file: " + filePath);
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing file for concept " + concept.getConceptName(), ex);
            } finally {
                try {
                    writer.close();
                } catch (IOException ex) {
                    throw new GeneratorException("Problem writing file for concept " + concept.getConceptName(), ex);
                }
            }
        }
    }

    private FileWriter generatePackageInfoFile(File directory) throws GeneratorException {
        FileWriter writer = null;
        try {
            File packageInfoFile = new File(directory,
                    Utilities.getLanguagePackageName(actualLanguage).replace('.', File.separatorChar)
                    + File.separatorChar + "package-info.java");
            writer = new FileWriter(packageInfoFile);
            generatePackageInfo(writer);
            //Jalopy odstranuje anotaciu @Parser, zatial som dal formatovanie priamo do predchadzajucej metody, je to lahke formatovanie
            //Utilities.formatCode(packageInfoFile);
            System.out.println("Success generating file: " + packageInfoFile.getPath());
        } catch (IOException ex) {
            throw new GeneratorException("Problem writing file for package-info.java", ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing file for package-info.java", ex);
            }
        }
        return writer;
    }

    private void generatePackageInfo(Writer writer) {
        try {
            writer.write("@"+tuke.pargen.annotation.config.Parser.class.getSimpleName());
            writer.write("(\n");
            writer.write("    className = \""+Utilities.getLanguagePackageName(actualLanguage)+".parser.Parser\",\n");
            writer.write("    mainNode = \""+Utilities.getFullConceptClassName(actualLanguage, actualLanguage.getConcepts().get(0))+"\",\n");
            //Tokens
            writer.write("    tokens = ");
            writer.write("{\n");
            boolean comma = false;
            for (TokenDef tokenDef : actualLanguage.getTokens()) {
                if (comma) {
                    writer.write(",\n");
                }
                writer.write("        @TokenDef(name = \""+tokenDef.getName()+"\", regexp = \""+tokenDef.getRegexp()+"\")");
                comma = true;
            }
            writer.write("\n    },\n");
            //Skips
            writer.write("    skips = ");
            writer.write("{\n");
            comma = false;
            for (String skip : actualLanguage.getSkips()) {
                if (comma) {
                    writer.write(",\n");
                }
                writer.write("        @Skip(\""+skip+"\")");
                comma = true;
            }
            writer.write("\n    }\n");
            writer.write(")\n");
            //Package + imports
            writer.write("package "+Utilities.getLanguagePackageName(actualLanguage)+";\n\n");
            writer.write("import "+tuke.pargen.annotation.config.Parser.class.getCanonicalName()+";\n");
            writer.write("import "+tuke.pargen.annotation.config.TokenDef.class.getCanonicalName()+";\n");
            writer.write("import "+tuke.pargen.annotation.config.Skip.class.getCanonicalName()+";\n");

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
            if (conceptPattern instanceof Enum) {
                writer.write("public enum " + concept.getConceptName() + " ");
                writer.write("{");
                boolean comma = false;
                for (Notation notation : concept.getConcreteSyntax()) {
                    for (NotationPart notationPart : notation.getParts()) {
                        if (notationPart instanceof TokenPart) {
                            if (comma) {
                                writer.write(", ");
                            }
                            writer.write(((TokenPart)notationPart).getToken());
                            comma = true;
                        }
                    }
                }
                writer.write("}");
                return;
            }
            else if (!(conceptPattern instanceof Operator)) {  //Operator sa pise ku konstruktorom aktualne
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
        writer.write("{");
        for (NotationPart notationPart : notation.getParts()) {
            if (notationPart instanceof PropertyReferencePart) {
                PropertyReferencePart part = (PropertyReferencePart) notationPart;
                writer.write("this." + part.getProperty().getName() + " = " + part.getProperty().getName() + ";");
            }
        }
        writer.write("}");
    }

    private void writeConstructorDefinition(Concept concept, Writer writer, Notation notation) throws IOException {
        ConceptPattern operator = concept.getPattern(Operator.class);
        if (operator != null) {
            writePattern(writer, operator);
        }
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
        writer.write("public " + concept.getConceptName() + "(");
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
        writer.write("import tuke.pargen.annotation.*;");
        writer.write("import tuke.pargen.annotation.reference.*;");
        writer.write("import yajco.annotation.printer.*;");
    }

    private void processTypeToConceptSet(Concept actualConcept, Type type, Set<Concept> conceptList) {
        if (type instanceof ComponentType) {
            processTypeToConceptSet(actualConcept, ((ComponentType)type).getComponentType(), conceptList);
        } else if (type instanceof ReferenceType) {
            Concept referencedConcept = ((ReferenceType)type).getConcept();
            if (!referencedConcept.getSubPackage().equals(actualConcept.getSubPackage())) {
                conceptList.add(referencedConcept);
            }
        }
    }

    private void writeConceptImport(Writer writer, Concept concept) throws IOException {
        writer.write("import " + Utilities.getFullConceptClassName(actualLanguage, concept) + ";");
    }

    private void writePackage(Concept concept, Writer writer) throws IOException {
        String packageName;
        if (concept.getSubPackage().isEmpty()) {
            packageName = Utilities.getLanguagePackageName(actualLanguage);
        } else {
            packageName = Utilities.getLanguagePackageName(actualLanguage) + "." + concept.getSubPackage().substring(0, concept.getSubPackage().length()-1);
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
        for (String string : list) {
            writer.write("\"" + string + "\"");
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
        } else {
            throw new GeneratorException("Not known pattern type: "+pattern.getClass().getCanonicalName());
        }
        writer.write(" ");
    }
    
}
