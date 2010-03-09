package yajco.printer;

import yajco.model.type.PrimitiveType;
import yajco.model.type.Type;
import yajco.model.type.ReferenceType;
import java.io.PrintWriter;
import java.util.List;
import yajco.model.*;
import yajco.model.pattern.*;
import yajco.model.pattern.impl.*;
import yajco.model.pattern.impl.printer.Indent;
import yajco.model.pattern.impl.printer.NewLine;
import yajco.model.type.ArrayType;

/*TOTO BY MALO BYT GENEROVANE DOMINIKOVYM PRINTER GENERATOROM*/
public class Printer {
    public void printLanguage(PrintWriter writer, Language language) {
        if (language.getName() != null) {
            writer.print("language " + language.getName() + "\n\n");
            printConcepts(writer, language.getConcepts());
        } else {
            printConcepts(writer, language.getConcepts());
        }
        writer.flush();
    }

    private void printConcepts(PrintWriter writer, List<Concept> concepts) {
        for (Concept concept : concepts) {
            printConcept(writer, concept);
        }
    }
    
    private void printConcept(PrintWriter writer, Concept concept) {
        writer.print("concept " + concept.getName());

        if (concept.getParent() != null) {
            writer.print(" : " + concept.getParent().getName());
        }

        printPatterns(writer, concept.getPatterns());

        writer.println();

        if (concept.getAbstractSyntax() != null && concept.getAbstractSyntax().size() > 0) {
            writer.print("    AS: ");
            printProperties(writer, concept.getAbstractSyntax());
        }

        if (concept.getConcreteSyntax() != null && concept.getConcreteSyntax().size() > 0) {
            writer.print("    CS: ");
            printNotations(writer, concept.getConcreteSyntax());
        }

        writer.println();
    }

    /* ------------------------ Abstract Syntax ------------------------ */
    private void printProperties(PrintWriter writer, List<Property> properties) {
        boolean seperate = false;
        for (Property property : properties) {
            if (seperate) {
                writer.print(", ");
            }
            printProperty(writer, property);
            seperate = true;
        }

        writer.println();
    }

    private void printProperty(PrintWriter writer, Property property) {
        writer.print(property.getName());
        writer.print(" : ");
        printType(writer, property.getType());

        printPatterns(writer, property.getPatterns());
    }

    private void printType(PrintWriter writer, Type type) {
        if (type instanceof PrimitiveType) {
            printPrimitiveType(writer, (PrimitiveType) type);
        } else if (type instanceof ReferenceType) {
            printReferenceType(writer, (ReferenceType) type);
        } else if (type instanceof ArrayType) {
            printArrayType(writer, (ArrayType) type);
        } else {
            throw new PrinterException("Not supported type " + type.getClass());
        }
    }

    private void printPrimitiveType(PrintWriter writer, PrimitiveType type) {
        switch (type) {
            case BOOLEAN:
                writer.print("boolean");
                break;
            case INTEGER:
                writer.print("int");
                break;
            case REAL:
                writer.print("real");
                break;
            case STRING:
                writer.print("string");
                break;
        }
    }

    private void printReferenceType(PrintWriter writer, ReferenceType type) {
        writer.print(type.getConcept().getName());
    }

    private void printArrayType(PrintWriter writer, ArrayType arrayType) {
        writer.print("array of ");
        printType(writer, arrayType.getComponentType());
    }

    /* ------------------------ Concrete Syntax ------------------------ */
    private void printNotations(PrintWriter writer, List<Notation> notations) {
        boolean seperate = false;
        for (Notation notation : notations) {
            if (seperate) {
                writer.print("\n      | ");
            }
            printNotation(writer, notation);
            seperate = true;
        }
        writer.println();
    }

    private void printNotation(PrintWriter writer, Notation notation) {
        printNotationParts(writer, notation.getParts());
    }

    private void printNotationParts(PrintWriter writer, List<NotationPart> parts) {
        for (NotationPart part : parts) {
            printNotationPart(writer, part);
            writer.write(" ");
        }
    }

    private void printNotationPart(PrintWriter writer, NotationPart part) {
        if (part instanceof TokenPart) {
            print(writer, (TokenPart) part);
        } else if (part instanceof BindingNotationPart) {
            printBindingNotationPart(writer, (BindingNotationPart) part);
        }
    }

    private void print(PrintWriter writer, TokenPart part) {
        writer.print("\"" + part.getToken() + "\"");
    }

    private void printBindingNotationPart(PrintWriter writer, BindingNotationPart part) {
        if (part instanceof PropertyReferencePart) {
            printPropertyReferencePart(writer, (PropertyReferencePart) part);
        } else if (part instanceof LocalVariablePart) {
            printLocalVariablePart(writer, (LocalVariablePart) part);
        }

        printPatterns(writer, part.getPatterns());
    }

    private void printPropertyReferencePart(PrintWriter writer, PropertyReferencePart part) {
        writer.print(part.getProperty().getName());
    }

    private void printLocalVariablePart(PrintWriter writer, LocalVariablePart part) {
        writer.print(part.getName());
        writer.print(" : ");
        printType(writer, part.getType());
    }

    /* ------------------------ Patterns ------------------------ */
    private void printPatterns(PrintWriter writer, List<? extends Pattern> patterns) {
        if (patterns != null && patterns.size() > 0) {
            writer.print(" ");
            writer.print("{");

            boolean seperate = false;
            for (Pattern pattern : patterns) {
                if (seperate) {
                    writer.print(" ");
                }
                printPattern(writer, pattern);
                seperate = true;
            }

            writer.print("}");
        }
    }

    private void printPattern(PrintWriter writer, Pattern pattern) {
        if (pattern instanceof Identifier) {
            printIdentifier(writer, (Identifier) pattern);
        } else if (pattern instanceof Operator) {
            printOperator(writer, (Operator) pattern);
        } else if (pattern instanceof Parentheses) {
            printParentheses(writer, (Parentheses) pattern);
        } else if (pattern instanceof Range) {
            printRange(writer, (Range) pattern);
        } else if (pattern instanceof References) {
            printReferences(writer, (References) pattern);
        } else if (pattern instanceof Separator) {
            printSeparator(writer, (Separator) pattern);
        } else if (pattern instanceof Indent) {
            printIndent(writer, (Indent) pattern);
        } else if (pattern instanceof NewLine) {
            printNewLine(writer, (NewLine) pattern);
        } else if (pattern instanceof yajco.model.pattern.impl.Enum) {
            printEnum(writer, (yajco.model.pattern.impl.Enum) pattern);
        } else {
            throw new PrinterException("Not supported pattern " + pattern.getClass());
        }
    }

    private void printIdentifier(PrintWriter writer, Identifier pattern) {
        writer.print("Identifier");
    }

    private void printOperator(PrintWriter writer, Operator pattern) {
        writer.print("Operator");
        writer.print("(");

        writer.print("priority = " + pattern.getPriority());

        if (pattern.getAssociativity() != null && pattern.getAssociativity() != Associativity.AUTO) {
            writer.print(", ");
            writer.print("associativity = " + pattern.getAssociativity());
        }

        writer.print(")");
    }

    private void printParentheses(PrintWriter writer, Parentheses pattern) {
        writer.print("Parentheses");

        if (!"(".equals(pattern.getLeft()) || !")".equals(pattern.getRight())) {
            writer.print("(");
            writer.print("\"" + pattern.getLeft() + "\"");
            writer.print(", ");
            writer.print("\"" + pattern.getRight() + "\"");
            writer.print(")");
        }
    }

    private void printRange(PrintWriter writer, Range range) {
        writer.print("Range");
        writer.print("(");

        if (range.getMinOccurs() == 0 && range.getMaxOccurs() == Range.INFINITY) {
            writer.print("*");
        } else if (range.getMaxOccurs() == Range.INFINITY) {
            writer.print(range.getMinOccurs());
            writer.print("..*");
        } else {
            writer.print(range.getMinOccurs() + ".." + range.getMaxOccurs());
        }
        writer.print(")");
    }

    private void printReferences(PrintWriter writer, References pattern) {
        writer.print("References");
        writer.print("(");

        writer.print(pattern.getConcept().getName());

        if (pattern.getProperty() != null) {
            writer.print(", ");
            writer.print("property = " + pattern.getProperty().getName());
        }

        writer.print(")");
    }

    private void printSeparator(PrintWriter writer, Separator pattern) {
        writer.print("Separator");
        writer.print("(");
        writer.print("\"" + pattern.getValue() + "\"");
        writer.print(")");
    }

    private void printIndent(PrintWriter writer, Indent indent) {
        writer.print("Indent");
    }

    private void printNewLine(PrintWriter writer, NewLine newLine) {
        writer.print("NewLine");
    }

    private void printEnum(PrintWriter writer, yajco.model.pattern.impl.Enum enumPattern) {
        writer.print("Enum");
    }
}
