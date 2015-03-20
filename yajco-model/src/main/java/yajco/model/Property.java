package yajco.model;

import java.util.ArrayList;
import yajco.model.type.Type;
import yajco.model.pattern.PatternSupport;
import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Optional;
import yajco.annotation.Separator;
import yajco.annotation.reference.Identifier;
import yajco.model.utilities.Utilities;
import yajco.model.pattern.PropertyPattern;

//Dodat tu este Range
public class Property extends PatternSupport<PropertyPattern> {

    @Identifier(unique = "../yajco.model.Property")
    private String name;
    private Type type;

    public Property(
            String name,
            @Before(":") Type type,
            @Optional @Before("{") @After("}") PropertyPattern[] patterns) {
        super(Utilities.asList(patterns), null);
        this.name = name;
        this.type = type;
    }

    public Property(
            String name,
            @Before(":") Type type) {
        super(new ArrayList<PropertyPattern>(), null);
        this.name = name;
        this.type = type;
    }

    @Exclude
    public Property(String name, @Before(":") Type type, Object sourceElement) {
        super(sourceElement);
        this.name = name;
        this.type = type;
    }
    
    //needed for XML binding
    @Exclude
    private Property() {
        super(null);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
