package yajco.model;

import java.util.ArrayList;
import yajco.model.type.Type;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.model.utilities.Utilities;
import yajco.model.pattern.NotationPartPattern;

public class LocalVariablePart extends BindingNotationPart {

    private final String name;
    private final Type type;

    public LocalVariablePart(
            String name,
            @Before(":") Type type,
            @Optional @Before("{") @After("}") NotationPartPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.type = type;
    }

    public LocalVariablePart(
            String name,
            @Before(":") Type type) {
        super(new ArrayList<NotationPartPattern>(), null);
        this.name = name;
        this.type = type;
    }

    @Exclude
    public LocalVariablePart(String name, Type type, Object sourceElement) {
        super(sourceElement);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "LocalVariable name: "+name + " | type: "+type.toString();
    }
    
    
}
