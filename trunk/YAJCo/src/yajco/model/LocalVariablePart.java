package yajco.model;

import yajco.model.type.Type;
import tuke.pargen.annotation.After;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Optional;
import yajco.Utilities;
import yajco.model.pattern.NotationPartPattern;

public class LocalVariablePart extends BindingNotationPart {
    private final String name;

    private final Type type;

    public LocalVariablePart(
            String name,
            @Before(":") Type type,
            @Optional @Before("{") @After("}") NotationPartPattern[] patterns) {
        super(Utilities.asList(patterns));
        this.name = name;
        this.type = type;
    }

    @Exclude
    public LocalVariablePart(String name, Type type) {
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
