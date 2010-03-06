package yajco.model;

import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import tuke.pargen.annotation.reference.References;
import yajco.Utilities;
import yajco.model.pattern.NotationPartPattern;

public class PropertyReferencePart extends BindingNotationPart {
    private Property property;

    public PropertyReferencePart(
            @References(value = Property.class, path = "ancestor::yajco.model.Concept//yajco.model.Property") String name,
            @Optional @Before("{") @After("}") NotationPartPattern[] patterns) {
        super(Utilities.asList(patterns));
    }

    @Exclude
    public PropertyReferencePart(Property property) {
        this.property = property;
    }

    public Property getProperty() {
        return property;
    }
}
