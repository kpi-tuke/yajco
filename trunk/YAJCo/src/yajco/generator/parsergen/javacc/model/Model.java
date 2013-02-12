package yajco.generator.parsergen.javacc.model;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import yajco.annotation.config.Option;
import yajco.generator.parsergen.javacc.Utilities;
import yajco.model.SkipDef;

//TODO: naozaj treba <EOF> v generateParserDefinition?, nebolo by mozne robit kompoziciu jazykov - v tom istom subore viacero jazykov?
public class Model {
    private final String packageName;

    private final String className;

    private final List<SkipDef> skips;

    private final Map<String, String> tokens;

    private final List<yajco.annotation.config.Option> options;

    private final Production mainProduction;

    private final Production[] productions;

    public Model(String packageName, String className, SkipDef[] skips, Map<String, String> tokens, yajco.annotation.config.Option[] options, Production mainProduction, Production[] productions) {
        this.packageName = packageName;
        this.className = className;
        this.skips = Arrays.asList(skips);
        this.tokens = tokens;
        this.options = Arrays.asList(options);
        this.mainProduction = mainProduction;
        this.productions = productions;

        for (Production production : productions) {
            production.setModel(this);
        }
    }

    public Production getProduction(String name) {
        for (Production production : productions) {
            if (production.getName().equals(name)) {
                return production;
            }
        }
        return null;
    }

    public void generate(Writer writer) throws IOException {
        writer.write(generateContent());
    }

    private String generateContent() {
        StringBuilder sb = new StringBuilder();

        //Generate code from annotation @Parser
        sb.append(generateParserDefinition());

        //Generate production code
        for (Production production : productions) {
            sb.append(production.generate()).append("\n");
        }

        return sb.toString();
    }

    private String generateParserDefinition() {
        Formatter code = new Formatter();

            code.format("options {\n");
            code.format("  USER_TOKEN_MANAGER = true;\n");
        if (options.size() > 0) {
            for (Option option : options) {
                code.format("  %s = %s;\n", option.name(), option.value());
            }
        }
            code.format("}\n\n");

        //PARSER definition
        code.format("PARSER_BEGIN(%s)\n", className);
        if (packageName != null) {
            code.format("package %s;\n\n", packageName);
        }
        code.format("public class %s {}\n\n", className);
        code.format("PARSER_END(%s)\n\n", className);

        //Toto je tu kvoli chybe v javacc ak je pouzity vlastny tokenmanager ale je tam konflikt
        //snazi sa to vypisat meno tokenu a spadne to - null pointer exception
        //ukazuje sa preto vhodne geenerovat vsetky lex. jedntotky aj ked pise, ze ich ignoruje
//        code.format("TOKEN :\n{\n");
//        boolean separator = false;
//        for (Map.Entry<String, String> token : tokens.entrySet()) {
//            printSeparator(code, separator, "| ", "  ");
//            separator = true;
//            code.format("<%s : \"%<s\">\n", Utilities.encodeStringIntoTokenName(token.getKey()));
//        }
//        code.format("}\n\n");

//        //SKIP definition
//        if (skips.size() > 0) {
//            code.format("SKIP :\n{\n");
//            boolean separator = false;
//            for (Skip skip : skips) {
//                printSeparator(code, separator, "| ", "  ");
//                separator = true;
//                //code.format("%s\n", skip.value());
//                code.format("<%s>\n", skip.value());
//            }
//            code.format("}\n\n");
//        }
//
//        //TOKEN definition
//        if (tokens.size() > 0) {
//            code.format("TOKEN :\n{\n");
//            boolean separator = false;
//            for (Map.Entry<String, String> token : tokens.entrySet()) {
//                if (token.getKey().equals(token.getValue())) {
//                    printSeparator(code, separator, "| ", "  ");
//                    separator = true;
//                    code.format("<%s : \"%s\">\n",
//                            "T_" + (isJavaIdentifier(token.getKey()) ? token.getKey() : Math.abs(token.getKey().hashCode())),
//                            token.getValue());
//                }
//            }
//
//            for (Map.Entry<String, String> token : tokens.entrySet()) {
//                if (!token.getKey().equals(token.getValue())) {
//                    printSeparator(code, separator, "| ", "  ");
//                    separator = true;
//                    code.format("<%s : %s>\n", token.getKey(), token.getValue());
//                }
//            }
//            code.format("}\n\n");
//        }

        //Main method
        code.format("%s parse() :\n", mainProduction.getReturnType());
        code.format("{\n  %s _value;\n}\n", mainProduction.getReturnType());
        code.format("{\n  _value = %s() <EOF>\n", mainProduction.getSymbolName());
        code.format("  {return _value;}\n");
        code.format("}\n\n");

        return code.toString();
    }

    private void printSeparator(Formatter code, boolean separate, String trueCase, String falseCase) {
        if (separate) {
            code.format(trueCase);
        } else {
            code.format(falseCase);
        }
    }

    public String getClassName() {
        return className;
    }

    public Production getMainProduction() {
        return mainProduction;
    }

    public List<yajco.annotation.config.Option> getOptions() {
        return options;
    }

    public String getPackageName() {
        return packageName;
    }

    public Production[] getProductions() {
        return productions;
    }

    public List<SkipDef> getSkips() {
        return skips;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        //Sort productions according to the lhs name
        List<Production> orderedProductions = Arrays.asList(productions);
        Collections.sort(orderedProductions, new Comparator<Production>() {
            public int compare(Production p1, Production p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });

        for (Production production : orderedProductions) {
            sb.append(production.toString() + "\n");
        }

        return sb.toString();
    }
}
