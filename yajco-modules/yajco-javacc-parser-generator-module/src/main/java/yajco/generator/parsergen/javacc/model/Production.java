package yajco.generator.parsergen.javacc.model;

public class Production {

    public static final String NON_TERMINAL_SUFFIX = "Symbol";
    private String name;
    private String returnType;
    private final Expansion expansion;
    private Model model;
    private Integer shortestLength;

    public Production(String name, String returnType, Expansion expansion) {
        this.name = name;
        this.returnType = returnType;
        this.expansion = expansion;
        expansion.setParent(this);
    }

    public String getSymbolName() {
        return name + NON_TERMINAL_SUFFIX;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public Expansion getExpansion() {
        return expansion;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType + " " + getSymbolName() + "() :\n");
        sb.append("{\n");
        sb.append(expansion.generateDeclaration());
        sb.append("}\n");
        sb.append("{\n");
        sb.append(expansion.generateExpansion(1));
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return name + " ::= " + expansion.toString();
    }

    public Integer getShortestLength() {
        return shortestLength;
    }

    public void setShortestLength(Integer shortestLength) {
        this.shortestLength = shortestLength;
    }
}
