package yajco.model;

import java.util.ArrayList;
import java.util.List;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.Utilities;

public class Language extends YajcoModelElement {

    private String name;
    private List<Concept> concepts;
    private List<TokenDef> tokens;
    private List<SkipDef> skips;

    public Language(
            @Before("tokens") @Range(minOccurs = 0) List<TokenDef> tokens,
            @Before("skips") @Range(minOccurs = 0) List<SkipDef> skips,
            @Range(minOccurs = 1) Concept[] concepts) {
        super(null);
        this.tokens = tokens;
        this.skips = skips;
        this.concepts = Utilities.asList(concepts);
    }

    public Language(
            @Before("language") String name,
            @Before("tokens") @Range(minOccurs = 0) List<TokenDef> tokens,
            @Before("skips") @Range(minOccurs = 0) List<SkipDef> skips,
            @Range(minOccurs = 1) Concept[] concepts) {
        super(null);
        this.name = name;
        this.tokens = tokens;
        this.skips = skips;
        this.concepts = Utilities.asList(concepts);
    }

    @Exclude
    public Language(Object sourceElement) {
        super(sourceElement);
        concepts = new ArrayList<Concept>();
    }

    public String getName() {
        return name;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public Concept getConcept(String name) {
        for (Concept concept : concepts) {
            if (concept.getName().equals(name)) {
                return concept;
            }
        }
        return null;
    }

    public void addConcept(Concept concept) {
        concepts.add(concept);
    }

    public List<SkipDef> getSkips() {
        return skips;
    }

    public void setSkips(List<SkipDef> skips) {
        this.skips = skips;
    }

    public List<TokenDef> getTokens() {
        return tokens;
    }

    public void setTokens(List<TokenDef> tokens) {
        this.tokens = tokens;
    }

    public TokenDef getToken(String name) {
        for (TokenDef token : tokens) {
            if (token.getName().equals(name)) {
                return token;
            }
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }
}
