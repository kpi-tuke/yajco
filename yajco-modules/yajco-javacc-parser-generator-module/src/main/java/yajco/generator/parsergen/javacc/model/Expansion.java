package yajco.generator.parsergen.javacc.model;

import java.util.HashSet;
import java.util.Set;

public abstract class Expansion {

    private String decl;
    private String code;
    private String lookahead;
    private Object parent;

    protected Expansion() {
        this(null, null, null);
    }

    //TODO: Zrusit lookahead z konstruktora
    public Expansion(String decl, String code, String lookahead) {
        this.decl = decl;
        this.code = code;
        this.lookahead = lookahead;
    }

    public String getDecl() {
        return decl;
    }

    public void setDecl(String decl) {
        this.decl = decl;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLookahead() {
        return lookahead;
    }

    public void setLookahead(String lookahead) {
        this.lookahead = lookahead;
    }

    public Object getParent() {
        return parent;
    }

    public void setParent(Object parent) {
        this.parent = parent;
    }

    public abstract ExpansionType getType();

    public String generateDeclaration() {
        if (decl != null) {
            return decl;
        }
        return "";
    }

    public String generateLookahead() {
        if (lookahead != null) {
            return "LOOKAHEAD(" + lookahead + ")";
        }
        return "";
    }

    public String generateExpansion(int level) {
            return generateExpansion(level, true);
        }
        
        public abstract String generateExpansion(int level, boolean withCode);

    protected String generateCode() {
        if (code != null) {
            return "  {" + code + "}";
        }
        return "";
    }

    protected String spaces(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    protected Production getProduction() {
        Expansion expansion = this;
        while (!(expansion.getParent() instanceof Production)) {
            expansion = (Expansion) expansion.getParent();
        }
        return (Production) expansion.getParent();
    }

    protected Model getModel() {
        return getProduction().getModel();
    }

    protected Set<String> newSet() {
        return new HashSet<String>();
    }

    protected Set<String> newSet(String s) {
        Set<String> set = new HashSet<String>();
        set.add(s);
        return set;
    }

    protected abstract Set<String> first(int n);

    protected abstract int getShortestLength();
}
