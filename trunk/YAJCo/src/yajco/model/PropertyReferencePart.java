package yajco.model;

import java.util.ArrayList;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.reference.References;
import yajco.Utilities;
import yajco.model.pattern.NotationPartPattern;

public class PropertyReferencePart extends BindingNotationPart {

    private Property property;

    public PropertyReferencePart(
            @References(value = Property.class, path = "ancestor::yajco.model.Concept//yajco.model.Property") String name,
            @Optional @Before("{") @After("}") NotationPartPattern[] patterns) {
        super(Utilities.asList(patterns), null);
    }

    public PropertyReferencePart(
            @References(value = Property.class, path = "ancestor::yajco.model.Concept//yajco.model.Property") String name) {
        super(new ArrayList<NotationPartPattern>(), null);
    }

    @Exclude
    public PropertyReferencePart(Property property, Object sourceElement) {
        super(sourceElement);
        this.property = property;
    }

    public Property getProperty() {
        return property;
    }
}
