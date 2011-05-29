package yajco.generator.parsergen.javacc.model;

import java.util.Set;

public abstract class RepeatingExpansion extends Expansion {
    private final Expansion expansion;

    public RepeatingExpansion(String decl, String code, String lookahead, Expansion expansion) {
        super(decl, code, lookahead);
        this.expansion = expansion;

        expansion.setParent(this);
    }

    public Expansion getExpansion() {
        return expansion;
    }

    @Override
    public String generateDeclaration() {
        return super.generateDeclaration() + expansion.generateDeclaration();
    }

    protected Set<String> first(int n) {
        Set<String> set = newSet();
        do {
            set.addAll(expansion.first(n));
            //TODO: POZOR aby to prestalo, predpokladam, ze nepride epsilon
            int length = expansion.getShortestLength();
            if(length == 0) {
                return set;
            }
            n -= length;
        } while (n > 0);
        return set;
    }
}
