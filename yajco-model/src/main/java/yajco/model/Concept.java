package yajco.model;

import java.util.ArrayList;
import java.util.List;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.Separator;
import yajco.annotation.Token;
import yajco.annotation.reference.Identifier;
import yajco.annotation.reference.References;
import yajco.model.pattern.ConceptPattern;
import yajco.model.pattern.PatternSupport;
import yajco.model.utilities.Utilities;

public class Concept extends PatternSupport<ConceptPattern> {

    @Identifier
    private String name;
    //TODO: mnozina superconceptov
    private Concept parent;
    private List<Property> abstractSyntax = new ArrayList<Property>();
    private List<Notation> concreteSyntax = new ArrayList<Notation>();

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    public Concept(
            @Before("concept") String name) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.name = name;
    }

        public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
    }

        public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

        public Concept(
            @Before("concept") String name,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

        public Concept(
            @Before("concept") String name,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.name = name;
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax){
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(new ArrayList<ConceptPattern>(), null);
        this.name = name;
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"AS", ":"}) @Separator(",") @yajco.annotation.Range(minOccurs = 1) Property[] abstractSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
    }

    public Concept(
            @Before("concept") String name,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"CS", ":"}) @Separator("|") @yajco.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    @Exclude
    public Concept(String name, Object sourceElement) {
        super(sourceElement);
        this.name = name;
        this.abstractSyntax = new ArrayList<Property>();
        this.concreteSyntax = new ArrayList<Notation>();
    }
    
    //needed for XML binding
    @Exclude
    private Concept(){
        super(null);
    }

    public String getName() {
        return name;
    }

    public Concept getParent() {
        return parent;
    }

    public void setParent(Concept parent) {
        this.parent = parent;
    }

    public List<Property> getAbstractSyntax() {
        return abstractSyntax;
    }

    public Property getProperty(String name) {
        for (Property property : abstractSyntax) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    public void addProperty(Property property) {
        abstractSyntax.add(property);
    }

    public List<Notation> getConcreteSyntax() {
        return concreteSyntax;
    }

    public void addNotation(Notation notation) {
        concreteSyntax.add(notation);
    }

    public String getConceptName() {
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex < 0) {
            return name;
        } else {
            return name.substring(lastDotIndex + 1, name.length());
        }
    }
    
    public String getNameWithoutDots() {
        return name.replace('.', '_');
    }

    public String getSubPackage() {
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex < 0) {
            return "";
        } else {
            return name.substring(0, lastDotIndex);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Concept)) {
            return false;
        }
        Concept concept = (Concept) obj;
        return name.equals(concept.getName());
    }

    @Override
    public int hashCode() {
        if (name != null) {
            return name.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public Property getSuperProperty(String name) {
        Property property = getProperty(name);
        if (property == null && parent != null) {
            property = parent.getSuperProperty(name);
        }
        return property;
    }
}
