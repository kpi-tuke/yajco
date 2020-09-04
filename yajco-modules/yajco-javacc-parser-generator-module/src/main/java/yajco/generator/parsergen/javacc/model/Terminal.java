package yajco.generator.parsergen.javacc.model;

import java.util.Set;
import javax.lang.model.element.Element;

public class Terminal extends Expansion {
    private Element element;

    private String variable;

    private String token;

    public Terminal(String decl, String code, String token, String variable) {
        super(decl, code, null);
        this.token = token;
        this.variable = variable;
    }

    public Terminal(String token) {
        this.element = null;
        this.variable = null;
        this.token = token;
    }

    public String getVariable() {
        return variable;
    }

    public String getToken() {
        return token;
    }

    @Override
    public ExpansionType getType() {
        return ExpansionType.TERMINAL;
    }

    @Override
    public String generateExpansion(int level, boolean withCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(spaces(level));
        if (withCode && variable != null) {
            sb.append(variable).append(" = ");
        }
        sb.append("<").append(yajco.generator.util.Utilities.encodeStringIntoTokenName(token)).append(">");
        if (withCode) {
            sb.append(generateCode());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "<" + token + ">";
    }

    @Override
    protected Set<String> first(int n) {
        if (n == 1) {
            return newSet(token);
        } else {
            return newSet();
        }
    }

    @Override
    protected int getShortestLength() {
        return 1;
    }
//    private void buildTerminal() {
//        ElementKind kind = this.element.getKind();
//
//        if (kind == ElementKind.ENUM_CONSTANT) {
//            // vztvarame terminal z enum konstanty
//            buildEnumTerminal();
//        } else if (kind == ElementKind.PARAMETER) {
//            buildParamTerminal();
//        }
//    }
//    private void buildEnumTerminal() {
//        TypeElement classElement = (TypeElement) this.element.getEnclosingElement();
//
//        String terminalCode, terminalToken;
//
//        Token tokenAnnotation = this.element.getAnnotation(Token.class);
//        if (tokenAnnotation != null) {
//            terminalToken = tokenAnnotation.value();
//        } else {
//            terminalToken = this.element.getSimpleName().toString();
//        }
//
//        terminalCode = String.format("return %s.%s;", classElement.getQualifiedName().toString(), this.element.getSimpleName().toString());
//
//        this.token = terminalToken;
//        setCode(terminalCode);
//    }
//    private void buildParamTerminal() {
//        String terminalToken = "";
//        String elementType = this.element.asType().toString();
//        Conversions conversions = ParserGenerator.getStringConversions();
//
//        Token tokenAnnotation = this.element.getAnnotation(Token.class);
//        if (tokenAnnotation != null) {
//            terminalToken = tokenAnnotation.value();
//        } else {
//            terminalToken = toUpperCaseNotation(this.element.getSimpleName().toString());
//        }
//
//        if (!conversions.containsConversion(elementType)) {
//            throw new GeneratorException(String.format("## Unsuported parameter '%s : %s' in element '%s'!", this.element.toString(), elementType, this.element.getEnclosingElement().getEnclosingElement().toString()));
//        }
//
//        Formatter declaration = new Formatter();
//        Formatter terminalCode = new Formatter();
//        doConversion(declaration, terminalCode, "");
//
//        this.token = terminalToken;
//        this.variable = "_token" + this.element.getSimpleName().toString();
//        setDecl(declaration.toString());
//        setCode(terminalCode.toString());
//    }
//    private String toUpperCaseNotation(String camelNotation) {
//        StringBuilder sb = new StringBuilder(camelNotation.length() + 10);
//        boolean change = true;
//        for (int i = 0; i < camelNotation.length(); i++) {
//            char c = camelNotation.charAt(i);
//            change = !change && Character.isUpperCase(c);
//            if (change) {
//                sb.append('_');
//            }
//            sb.append(Character.toUpperCase(c));
//            change = Character.isUpperCase(c);
//        }
//        return sb.toString();
//    }
//    private void doConversion(Formatter declaration, Formatter terminalCode, String extraCode) {
//        String variableName = this.element.getSimpleName().toString();
//        Conversions conversions = ParserGenerator.getStringConversions();
//        String elementType = this.element.asType().toString();
//        String conversion = conversions.getConversion(elementType);
//        String defaultValue = conversions.getDefaultValue(elementType);
//
//        declaration.format("  %s %s = %s;\n", elementType, variableName, defaultValue);
//        declaration.format("  Token _token%s = null;\n", variableName);
//
//        terminalCode.format("%s = ", variableName);
//        terminalCode.format(conversion, "_token" + variableName + ".image");
//        terminalCode.format(";");
//        terminalCode.format("%s", extraCode);
//    }
}
