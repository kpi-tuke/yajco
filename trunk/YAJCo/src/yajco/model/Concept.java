package yajco.model;

import java.util.ArrayList;
import yajco.model.pattern.PatternSupport;
import java.util.List;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import tuke.pargen.annotation.Separator;
import tuke.pargen.annotation.Token;
import tuke.pargen.annotation.reference.Identifier;
import tuke.pargen.annotation.reference.References;
import yajco.Utilities;
import yajco.model.pattern.ConceptPattern;

public class Concept extends PatternSupport<ConceptPattern> {
    @Identifier
    private final String name;

    //TODO: mnozina superconceptov
    private Concept parent;

    private List<Property> abstractSyntax;

    private List<Notation> concreteSyntax;

    public Concept(
            @Before("concept") String name,
            @Optional @Before(":") @References(Concept.class) @Token("NAME") String parent,
            @Optional @Before("{") @After("}") ConceptPattern[] patterns,
            @Optional @Before({"AS", ":"}) @Separator(",") @tuke.pargen.annotation.Range(minOccurs = 1) Property[] abstractSyntax,
            @Optional @Before({"CS", ":"}) @Separator("|") @tuke.pargen.annotation.Range(minOccurs = 1) Notation[] concreteSyntax) {
        super(Utilities.asList(patterns));
        this.name = name;
        this.abstractSyntax = Utilities.asList(abstractSyntax);
        this.concreteSyntax = Utilities.asList(concreteSyntax);
    }

    @Exclude
    public Concept(String name) {
        super();
        this.name = name;
        this.abstractSyntax = new ArrayList<Property>();
        this.concreteSyntax = new ArrayList<Notation>();
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
		return name.hashCode();
	}

    public Property getSuperProperty(String name) {
        Property property = getProperty(name);
        if (property == null && parent != null) {
            property = parent.getSuperProperty(name);
        }
        return property;
    }
}
