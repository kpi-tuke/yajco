package yajco.classgen;

import de.hunsicker.jalopy.Jalopy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import tuke.pargen.GeneratorException;
import tuke.pargen.annotation.Associativity;
import yajco.generator.util.Utilities;
import yajco.model.BindingNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
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
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.Type;

public class ClassGenerator {

    private static String DEFAULT_PACKAGE_NAME = "test.model";

    private Language actualLanguage;

    public void generate(Language language, File directory) {
        if (directory == null || !directory.isDirectory()) {
            throw new GeneratorException("Supplied parameter is not directory.");
        }
        actualLanguage = language;

        Jalopy jalopy = new Jalopy();
        for (Concept concept : language.getConcepts()) {
            FileWriter writer = null;
            try {
                File file = new File(directory, concept.getName() + ".java");
                writer = new FileWriter(file);
                generate(concept, writer);
                jalopy.setInput(file);
                jalopy.setOutput(file);
                jalopy.format();
            } catch (IOException ex) {
                throw new GeneratorException("Problem writing file for concept "+concept.getName(), ex);
            } finally {
                try {
                    writer.close();
                } catch (IOException ex) {
                    throw new GeneratorException("Problem writing file for concept "+concept.getName(), ex);
                }
            }
        }
    }

    private void generate(Concept concept, Writer writer) {
        try {
            writePackage(writer);
            writeImports(concept, writer);
            writeClassBody(concept, writer);

            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Error writing class for concept " + concept.getName() + ".", ex);
        }
    }

    private void writeClassBody(Concept concept, Writer writer) throws IOException {
        for (ConceptPattern conceptPattern : concept.getPatterns()) {
            if (!(conceptPattern instanceof Operator)) {  //Operator sa pise ku konstruktorom aktualne
                writePattern(writer, conceptPattern);
            }
        }
        writer.write("public class " + concept.getName() + " ");
        if (concept.getParent() != null) {
            writer.write("extends " + concept.getParent().getName());
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
        writer.write("public " + concept.getName() + "(");
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
                        writer.write(getTypeName(part.getProperty().getType()) + " " + part.getProperty().getName());
                    } else if (notationPart instanceof LocalVariablePart) {
                        LocalVariablePart part = (LocalVariablePart) notationPart;
                        writer.write(getTypeName(part.getType()) + " " + part.getName());
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
            writer.write(getTypeName(type) + " ");
            writer.write("get" + Utilities.toUpperCaseIdent(property.getName()) + "() {");
            writer.write("return " + property.getName() + ";");
            writer.write("}");
            writer.write("public void ");
            writer.write("set" + Utilities.toUpperCaseIdent(property.getName()) + "(" + getTypeName(type) + " " + property.getName() + ") {");
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
            writer.write(getTypeName(type) + " ");
            writer.write(property.getName() + ";");
        }
    }

    private void writeImports(Concept concept, Writer writer) throws IOException {
        boolean arrayType = false;
        for (Property property : concept.getAbstractSyntax()) {
            Type type = property.getType();
            if (type instanceof ArrayType) {
                arrayType = true;
// TODO: az bude realne funkcne spravena moznost vytvarania roznych balikov pre koncepty v jednom jazyku, toto treba spojazdnit a odstranit duplicity
//                if (((ArrayType) type).getComponentType() instanceof ReferenceType) {
//                    writeReferenceTypeImport(writer, (ReferenceType) (((ArrayType) type).getComponentType()));
//                }
            } else if (type instanceof ReferenceType) {
//                writeReferenceTypeImport(writer, (ReferenceType) type);
            }
        }
        if (arrayType) {
            writer.write("import java.util.List;");
        }
        writer.write("import tuke.pargen.annotation.*;");
        writer.write("import tuke.pargen.annotation.reference.*;");
        writer.write("import yajco.annotation.printer.*;");
    }

    private void writeReferenceTypeImport(Writer writer, ReferenceType type) throws IOException {
        writer.write("import " + getFullConceptClassName(type.getConcept()) + ";");
    }

    private void writePackage(Writer writer) throws IOException {
        writer.write("package " + getPackageName() + ";");
    }

    private String getPackageName() {
        if (actualLanguage.getName() != null) {
            return actualLanguage.getName();
        } else {
            return DEFAULT_PACKAGE_NAME;
        }
    }

    private String getFullConceptClassName(Concept concept) {
        return getPackageName() + "." + concept.getName();
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
            writer.write("@References(value = " + references.getConcept().getName() + ".class");
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

    private String getTypeName(Type type) {
        StringBuilder str = new StringBuilder();
        if (type instanceof ArrayType) {
            str.append("List<");
            str.append(getTypeName(((ArrayType) type).getComponentType()));
            str.append(">");
        } else if (type instanceof PrimitiveType) {
            PrimitiveType primitive = (PrimitiveType) type;
            switch (primitive) {
                case BOOLEAN:
                    str.append("boolean");
                    break;
                case INTEGER:
                    str.append("int");
                    break;
                case REAL:
                    str.append("double");
                    break;
                case STRING:
                    str.append("String");
                    break;
            }
        } else if (type instanceof ReferenceType) {
            str.append(((ReferenceType) type).getConcept().getName());
        }
        return str.toString();
    }
}
