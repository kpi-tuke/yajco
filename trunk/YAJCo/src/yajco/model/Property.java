package yajco.model;

import yajco.model.type.Type;
import yajco.model.pattern.PatternSupport;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import tuke.pargen.annotation.reference.Identifier;
import yajco.Utilities;
import yajco.model.pattern.PropertyPattern;

//Dodat tu este Range
public class Property extends PatternSupport<PropertyPattern> {
    @Identifier(unique = "../yajco.model.Concept")
    private final String name;

    private final Type type;

    public Property(
            String name,
            @Before(":") Type type,
            @Optional @Before("{") @After("}") PropertyPattern[] patterns) {
        super(Utilities.asList(patterns));
        this.name = name;
        this.type = type;
    }

    @Exclude
    public Property(
            String name,
            @Before(":") Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
