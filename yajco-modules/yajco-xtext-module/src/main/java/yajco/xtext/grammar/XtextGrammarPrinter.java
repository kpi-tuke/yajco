package yajco.xtext.grammar;

import yajco.model.*;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.*;
import yajco.model.type.*;
import yajco.xtext.commons.model.*;
import yajco.xtext.commons.regex.XtextRegexCompiler;
import yajco.xtext.commons.settings.XtextProjectSettings;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static yajco.xtext.commons.model.RuleUtils.makeCamelCaseName;

public class XtextGrammarPrinter {

    private static final String TERM = "_TERMINAL";

    private final XtextProjectSettings settings;
    private XtextRegexCompiler regexCompiler = new XtextRegexCompiler();

    private XtextGrammarModel grammarModel;

    public XtextGrammarPrinter(XtextGrammarModel grammarModel) {
        this.settings = XtextProjectSettings.getInstance();
        this.grammarModel = grammarModel;
    }

    public void print(PrintWriter printWriter) {
        if (settings.getMainNode() != null) {
            printWriter.write(grammarModel.getGrammarDeclaration());
            printWriter.write(grammarModel.getGenerateDeclaration());

            printRules(printWriter);
            printWsTerminal(printWriter, grammarModel.getWsTerminalBody());
            printTerminals(printWriter, grammarModel.getTerminalsMap());
        }
    }

    private void printRules(PrintWriter printWriter) {
        List<Rule> rules = grammarModel.getRules();

        for (int i = 0; i < rules.size(); i++) {
            Rule currentRule = rules.get(i);
            if (currentRule.getType() == RuleType.MAIN) {
                printMainRule(printWriter, currentRule);
            } else if (currentRule.getType() == RuleType.DECLARATOR) {
                printDeclaratorRule(printWriter, currentRule);
            } else if (currentRule.getType() == RuleType.OPERATOR) {
                printOperatorRule(printWriter, currentRule);
            } else if (currentRule.getType() == RuleType.RETURNER) {
                printReturnerRule(printWriter, currentRule);
            } else if (currentRule.getType() == RuleType.RETURNER_AGGREGATOR) {
                printReturnerAggregatorRule(printWriter, currentRule);
            } else if (currentRule.getType() == RuleType.PARENTHESES) {
                printParenthesesRule(printWriter, currentRule);
            }
        }
    }

    private void printParenthesesRule(PrintWriter printWriter, Rule currentRule) {
        printWriter.write(currentRule.getName() + " returns "
                + currentRule.getParent() + ": \n\t");
        printWriter.write(currentRule.getLeftParenthesis());
        printWriter.write(" " + currentRule.getParent() + " ");
        printWriter.write(currentRule.getRightParenthesis());
        printWriter.write(";\n\n");
    }

    private void printReturnerAggregatorRule(PrintWriter printWriter, Rule currentRule) {
        printWriter.write(currentRule.getName() + " returns "
                + currentRule.getParent() + ":\n\t");
        printReturnerAggregatorBody(currentRule, printWriter);
        printWriter.write("\n");
    }

    private void printReturnerRule(PrintWriter printWriter, Rule currentRule) {
        printWriter.write(currentRule.getName());
        if (currentRule.getParent() != null) {
            printWriter.write(" returns " + currentRule.getParent() + ":\n\t");
            printWriter.write("{" + currentRule.getName() + "} ");
        } else {
            printWriter.write(":\n\t");
        }
        printReturnerBody(currentRule, printWriter);
        printWriter.write("\n");
    }

    private void printOperatorRule(PrintWriter printWriter, Rule currentRule) {
        printWriter.write(currentRule.getName());
        if (currentRule.getParent() != null) {
            printWriter.write(" returns " + currentRule.getParent());
        }
        printWriter.write(":\n\t");
        printOperatorBody(currentRule, printWriter);
        printWriter.write("\n");
    }

    private void printDeclaratorRule(PrintWriter printWriter, Rule currentRule) {
        List<String> names = ((SuperRule) currentRule).getAllDeclaratorRulesNames();

        if (names.size() > 0) {
            printWriter.write(currentRule.getName());
            if (currentRule.getParent() != null && !currentRule.getParent().equals(currentRule.getName())) {
                printWriter.write(" returns " + currentRule.getParent());
            }
            printWriter.write(":\n\t");
            printWriter.write(String.join(" |\n\t", names));
            printWriter.write(";\n\n");
        }

    }

    private void printMainRule(PrintWriter printWriter, Rule currentRule) {
        printWriter.write(currentRule.getName());
        if (currentRule.getParent() != null) {
            printWriter.write(" returns " + currentRule.getParent());
        }
        printWriter.write(":\n\t");

        if (currentRule.getCs() != null && !currentRule.getCs().isEmpty()) {
            printReturnerBody(currentRule, printWriter);
        } else if (currentRule.getNext() != null) {
            printWriter.print(currentRule.getNext().getName());
            printWriter.write(";\n");
        }
        printWriter.write("\n");
    }

    private void printReturnerAggregatorBody(Rule currentRule, PrintWriter printWriter) {
        List<Rule> currRules = ((SuperRule) currentRule).getRules();
        if (currRules.size() > 1) {
            Rule lastRule = currRules.get(currRules.size() - 1);
            for (Rule rule : currRules) {
                printWriter.write(rule.getName());
                if (rule != lastRule) {
                    printWriter.write(" | \n\t");
                } else {
                    printWriter.write(";\n");
                }
            }
        } else {
            printNotations(printWriter, currRules.get(0));
            printWriter.write(";\n");
        }
    }

    private void printReturnerBody(Rule currentRule, PrintWriter printWriter) {
        printNotations(printWriter, currentRule);
        printWriter.write(";\n");
    }

    private void printOperatorBody(Rule currentRule, PrintWriter printWriter) {
        List<Rule> currRules = ((SuperRule) currentRule).getRules();
        boolean separate = false;
        for (Rule rule : currRules) {
            for (Node<NotationPart> branch : rule.getOptimizedNotations()) {
                if (branch.getData() instanceof TokenPart) {
                    if (separate) {
                        printWriter.print(" | \n\t");
                    }
                    printUnaryNotation(printWriter, rule, branch);
                    separate = true;
                }
            }
        }
        if (separate) {
            printWriter.print(" | \n\t");
        }
        if (currentRule.getNext() != null) {
            printWriter.write(currentRule.getNext().getName());
        }

        boolean secondSeparate = false;
        for (Rule rule : currRules) {
            for (Node<NotationPart> branch : rule.getOptimizedNotations()) {
                if (!(branch.getData() instanceof TokenPart)) {
                    if (!secondSeparate) {
                        printWriter.print(" ( ");
                    } else {
                        printWriter.print(" | \n\t");
                    }
                    printNonUnaryNotation(printWriter, rule, branch);
                    secondSeparate = true;
                }
            }
        }

        if (secondSeparate) {
            printEndOfAssociativy(printWriter, currRules.get(0).getAssociativity());
        } else {
            printWriter.print(";\n");
        }

    }


    private void printEndOfAssociativy(PrintWriter printWriter, Associativity associativity) {
        if (associativity == Associativity.LEFT || associativity == Associativity.AUTO) {
            printWriter.write(")*;\n");
        } else {
            printWriter.write(")?;\n");
        }
    }

    private void printTerminalDef(PrintWriter writer, String name, String body) {
        writer.print("terminal ");
        writer.print(name + TERM);
        writer.print(": \n\t");
        writer.print(body + ";\n\n");
    }

    private void printType(PrintWriter writer, Type type) {
        if (type instanceof PrimitiveType) {
            printPrimitiveType(writer, (PrimitiveType) type);
        } else if (type instanceof ReferenceType) {
            printReferenceType(writer, (ReferenceType) type);
        } else if (type instanceof ArrayType) {
            printArrayType(writer, (ArrayType) type);
        } else if (type instanceof ListType) {
            printListType(writer, (ListType) type);
        } else if (type instanceof SetType) {
            printSetType(writer, (SetType) type);
        } else {
            throw new RuntimeException("Not supported type " + type.getClass());
        }
    }

    //TODO change implementation of the method when primitive types in YAJCo are supported
    private void printPrimitiveType(PrintWriter writer, PrimitiveType type) {
        switch (type.getPrimitiveTypeConst()) {
            case BOOLEAN:
                writer.print("boolean");
                break;
            case INTEGER:
                writer.print("INT");
                break;
            case REAL:
                writer.print("real");
                break;
            case STRING:
                writer.print("ID");
                break;
        }
    }

    private void printReferenceType(PrintWriter writer, ReferenceType type) {
        writer.print(makeCamelCaseName(type.getConcept().getName()));
    }

    private void printArrayType(PrintWriter writer, ArrayType arrayType) {
        printType(writer, arrayType.getComponentType());
    }

    private void printListType(PrintWriter writer, ListType listType) {
        printType(writer, listType.getComponentType());
    }

    private void printSetType(PrintWriter writer, SetType setType) {
        printType(writer, setType.getComponentType());
    }

    private void printNotations(PrintWriter writer, Rule rule) {
        List<Node<NotationPart>> tree = rule.getOptimizedNotations();
        boolean seperate = false;
        List<Node<NotationPart>> empty = tree.stream().filter(branch -> branch.getData() == null).collect(Collectors.toList());
        List<Node<NotationPart>> nonempty = tree.stream().filter(branch -> branch.getData() != null).collect(Collectors.toList());

        if (!empty.isEmpty() && !nonempty.isEmpty()) {
            writer.print("(");
        }

        for (Node<NotationPart> branch : nonempty) {
            if (seperate) {
                writer.print(" |\n\t");
            }
            printTraverseTree(branch, writer, rule);
            seperate = true;
        }

        if (!empty.isEmpty() && !nonempty.isEmpty()) {
            writer.print(")*");
        }
    }

    private void printUnaryNotation(PrintWriter writer, Rule rule, Node<NotationPart> branch) {
        printTraverseTree(branch, writer, rule, true);
    }

    private void printNonUnaryNotation(PrintWriter writer, Rule rule, Node<NotationPart> branch) {
        printTraverseTree(branch, writer, rule, false);
    }

    private void printTraverseTree(Node<NotationPart> node, PrintWriter writer, Rule rule, boolean unary) {
        printNotationPart(writer, node.getData(), rule, unary);
        if (node.getChildren().size() > 1) {
            writer.write("(");
        }
        List<Node<NotationPart>> children = node.getChildren().stream().filter(nd -> nd.getData() != null).collect(Collectors.toList());
        boolean separate = false;
        for (Node<NotationPart> child : children) {
            if (separate) {
                writer.print(" | \n\t");
            }
            printTraverseTree(child, writer, rule, unary);
            separate = true;
        }

        if (node.getChildren().size() > 1) {
            writer.write(")");
        }
        if (node.getChildren().stream().anyMatch(nd -> nd.getData() == null)) {
            writer.write("?");
        }
    }

    private void printTraverseTree(Node<NotationPart> node, PrintWriter writer, Rule rule) {
        printTraverseTree(node, writer, rule, false);
    }

    private void printNotationPart(PrintWriter writer, NotationPart part, Rule rule, boolean unary) {
        if (part instanceof TokenPart) {
            printTokenNotationPart(writer, part);
        } else if (part instanceof BindingNotationPart) {
            if (rule.getType() == RuleType.OPERATOR) {
                printBindingNotationPartForOperator(writer, (BindingNotationPart) part, rule, unary);
            } else {
                printBindingNotationPartForReturner(writer, (BindingNotationPart) part);
                Type type;
                if (part instanceof PropertyReferencePart) {
                    type = ((PropertyReferencePart) part).getProperty().getType();
                } else {
                    type = ((LocalVariablePart) part).getType();
                }
                if (((BindingNotationPart) part).getPattern(Range.class) == null &&
                        (type instanceof ArrayType || type instanceof SetType || type instanceof ListType)
                ) {
                    if (((BindingNotationPart) part).getPatterns().stream().noneMatch(patt -> patt instanceof Separator)) {
                        writer.write("* ");
                    } else {
                        writer.write("? ");
                    }
                }
                printPatterns(writer, (BindingNotationPart) part);
            }
        }
    }

    private void printTokenNotationPart(PrintWriter writer, NotationPart part) {
        TokenDef token = grammarModel.getToken(((TokenPart) part).getToken());
        if (token != null) {
            printToken(writer, token);
        } else {
            printToken(writer, (TokenPart) part);
        }
    }

    private void printBindingNotationPartForReturner(PrintWriter writer, BindingNotationPart part) {
        if (part instanceof PropertyReferencePart) {
            Type type = ((PropertyReferencePart) part).getProperty().getType();
            writer.write(" " + ((PropertyReferencePart) part).getProperty().getName() + " ");
            if (type instanceof ArrayType ||
                    type instanceof SetType ||
                    type instanceof ListType) {
                writer.write("+");
            }
            writer.write("=");

            String terminalName;
            Token token = (Token) part.getPattern(Token.class);
            if (token != null) {
                terminalName = token.getName();
            } else {
                terminalName = this.grammarModel.getTokens().stream().filter(tkn -> tkn.getName().equals(
                        ((PropertyReferencePart) part).getProperty().getName().toUpperCase()))
                        .map(TokenDef::getName).findFirst().orElse("");
            }
            if (!terminalName.isEmpty()) {
                writer.write(terminalName.toUpperCase() + TERM);
            } else {
                printType(writer, ((PropertyReferencePart) part).getProperty().getType());
            }
        } else if (part instanceof LocalVariablePart) {
            printLocalVariablePart(writer, (LocalVariablePart) part);
        }

    }

    private void printToken(PrintWriter writer, TokenDef part) {
        writer.print(" " + part.getName().toUpperCase() + TERM + " ");
    }

    private void printToken(PrintWriter writer, TokenPart part) {
        String name = findTokenFromString(part.getToken());
        if (name.equals(part.getToken())) {
            writer.print(" \"" + name + "\" ");
        } else {
            writer.print(" " + name.toUpperCase() + TERM + " ");
        }
    }

    private void printBindingNotationPartForOperator(PrintWriter writer, BindingNotationPart part, Rule rule, boolean unary) {
        if (part instanceof PropertyReferencePart) {
            String name = rule.getAs().get(0).getName();
            String lastName = rule.getAs().get(rule.getAs().size() - 1).getName();
            String actualName = ((PropertyReferencePart) part).getProperty().getName();
            if (unary) {
                String result = "{" + rule.getName() + "} "
                        + ((PropertyReferencePart) part).getProperty().getName() + " = ";
                writer.write(" " + result + " ");
                writer.write(rule.getSuperRule().getName() + " ");
            } else if (name.equals(actualName)) {
                String result = "{" + rule.getName() + "."
                        + ((PropertyReferencePart) part).getProperty().getName() + "=current}";
                writer.write(" " + result + " ");
            } else if (lastName.equals(actualName)) {
                if (rule.getAssociativity() == Associativity.LEFT || rule.getAssociativity() == Associativity.AUTO) {
                    writer.write(" " + actualName + "=");
                    if (rule.getSuperRule().getNext().getType() == RuleType.OPERATOR) {
                        writer.write(" " + rule.getSuperRule().getNext().getName() + " ");
                    } else {
                        if (rule.getSuperRule().getNext() != null) {
                            writer.write(" " + rule.getSuperRule().getNext().getName() + " ");
                        }
                    }
                } else if (rule.getAssociativity() == Associativity.RIGHT) {
                    writer.write(" " + actualName + "=");
                    writer.write(rule.getSuperRule().getName());
                } else {
                    writer.write(" " + actualName + "=");
                    if (rule.getSuperRule().getNext().getType() == RuleType.OPERATOR) {
                        writer.write(" " + rule.getSuperRule().getNext().getName() + " ");
                    } else {
                        if (rule.getSuperRule().getNext() != null) {
                            writer.write(" " + rule.getSuperRule().getNext().getName() + " ");
                        }
                    }
                }
            } else {
                String result = ((PropertyReferencePart) part).getProperty().getName() + "=" + rule.getName();
                writer.write(" " + result + " ");
            }
        } else if (part instanceof LocalVariablePart) {
            printLocalVariablePart(writer, (LocalVariablePart) part);
        }
    }

    private void printLocalVariablePart(PrintWriter writer, LocalVariablePart part) {
        References pattern = (References) part.getPattern(References.class);
        if (pattern.getProperty() != null && pattern.getProperty().getName() != null && !pattern.getProperty().getName().isEmpty()) {
            writer.print(pattern.getProperty().getName());
        } else {
            writer.print(part.getName());
        }
        writer.print("=");
        Token tokenPattern = (Token) part.getPattern(Token.class);
        writer.print("[" + makeCamelCaseName(pattern.getConcept().getName()));
        String name;
        if (tokenPattern != null) {
            name = tokenPattern.getName().toUpperCase() + TERM;
        } else {
            name = this.grammarModel.getTokens().stream().filter(token -> token.getName().equals(
                    part.getName().toUpperCase()))
                    .map(TokenDef::getName).findFirst().orElse("");
            name += TERM;
        }
        writer.write(" | " + name + "]");
    }

    private void printPatterns(PrintWriter writer, BindingNotationPart part) {
        List<? extends Pattern> patterns = (part).getPatterns();
        if (patterns != null && patterns.size() > 0) {
            for (Pattern pattern : patterns) {
                if (pattern instanceof Range) {
                    switch (((Range) pattern).getMinOccurs()) {
                        case 0:
                            if (patterns.stream().noneMatch(patt -> patt instanceof Separator)) {
                                writer.write("*");
                            } else {
                                writer.write("?");
                            }
                            break;
                        case 1:
                            if (patterns.stream().noneMatch(patt -> patt instanceof Separator)) {
                                writer.write("+");
                            }
                            break;
                    }
                }
                if (pattern instanceof Separator) {
                    writer.write(" (");
                    String separatorName = this.grammarModel.getTokens().stream().filter(token -> token.getName().equals(
                            ((Separator) pattern).getValue().toUpperCase()))
                            .map(TokenDef::getName).findFirst().orElse("");
                    if (!separatorName.isEmpty()) {
                        writer.write(" " + separatorName.toUpperCase() + TERM + " ");
                    } else {
                        String name = findTokenFromString(((Separator) pattern).getValue());
                        if (name.equals(((Separator) pattern).getValue())) {
                            writer.write(" \"" + name + "\" ");
                        } else {
                            writer.write(" " + name.toUpperCase() + TERM + " ");
                        }
                    }
                    printBindingNotationPartForReturner(writer, part);
                    writer.write(")*");
                }
            }
        }
    }

    private String findTokenFromString(String str) {
        return this.grammarModel.getTokens().stream()
                .filter(token ->
                        str.equals(regexCompiler.compileRegex(token.getRegexp())
                                .substring(regexCompiler.compileRegex(token.getRegexp()).indexOf('\'') + 1,
                                        regexCompiler.compileRegex(token.getRegexp()).lastIndexOf('\''))))
                .map(TokenDef::getName).findFirst().orElse(str);
    }

    private void printTerminals(PrintWriter writer, Map<String, String> terminals) {
        terminals.keySet().forEach((String key) -> printTerminalDef(writer, key, terminals.get(key)));
        writer.println();
    }

    private void printWsTerminal(PrintWriter writer, String body) {
        if (body != null && !body.isEmpty()) {
            writer.print("terminal WS: \n\t(" + body + ")+;\n\n");
        }
    }
}
