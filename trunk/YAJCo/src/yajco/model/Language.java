package yajco.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yajco.Utilities;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.impl.Token;

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

    public List<TokenDef> getUsedTokens() {
        Set<TokenDef> usedTokens = new HashSet<TokenDef>();
        Map<String, TokenDef> mapTokens = new HashMap<String, TokenDef>();
        for (TokenDef tokenDef : tokens) {
            mapTokens.put(tokenDef.getName().toUpperCase(), tokenDef);
        }
        
        for (Concept concept : concepts) {
            for (Notation notation : concept.getConcreteSyntax()) {
                for (NotationPart notationPart : notation.getParts()) {
                    String possibleToken;
                    possibleToken = getTokenString(notationPart);
                    if (mapTokens.containsKey(possibleToken.toUpperCase())) {
                        usedTokens.add(mapTokens.get(possibleToken.toUpperCase()));
                    } else if (possibleToken.toUpperCase().endsWith("S")) {
                        if(mapTokens.containsKey(possibleToken.substring(0, possibleToken.length()-1).toUpperCase())) {
                            usedTokens.add(mapTokens.get(possibleToken.toUpperCase()));
                        }
                    }
                }
            }
        }
        return new ArrayList<TokenDef>(usedTokens);
    }

    private String getTokenString(NotationPart notationPart) {
        String possibleToken;
        if (notationPart instanceof BindingNotationPart) {
            BindingNotationPart bnp = (BindingNotationPart) notationPart;
            Token tokenPattern = (Token) bnp.getPattern(Token.class);
            if (tokenPattern != null) {
                possibleToken = tokenPattern.getName();
            } else {
                if (bnp instanceof LocalVariablePart) {
                    LocalVariablePart lvp = (LocalVariablePart) bnp;
                    possibleToken = lvp.getName();
                } else {
                    PropertyReferencePart prp = (PropertyReferencePart) bnp;
                    possibleToken = prp.getProperty().getName();
                }
            }
            
        } else if (notationPart instanceof TokenPart) {
            TokenPart tp = (TokenPart) notationPart;
            possibleToken  = tp.getToken();
        } else {
            possibleToken = "";
            //not know type
        }
        return possibleToken;
    }
}
