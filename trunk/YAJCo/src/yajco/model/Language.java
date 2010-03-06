package yajco.model;

import java.util.ArrayList;
import java.util.List;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Range;
import yajco.Utilities;

public class Language {
    private String name;

    private List<Concept> concepts;

    public Language(@Range(minOccurs = 1) Concept[] concepts) {
        this.concepts = Utilities.asList(concepts);
    }

    public Language(@Before("language") String name, @Range(minOccurs = 1) Concept[] concepts) {
        this.name = name;
        this.concepts = Utilities.asList(concepts);
    }

    @Exclude
    public Language() {
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
}
