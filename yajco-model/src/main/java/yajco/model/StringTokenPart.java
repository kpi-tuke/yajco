package yajco.model;

import yajco.annotation.Exclude;
import yajco.annotation.Range;
import yajco.annotation.Token;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class StringTokenPart extends YajcoModelElement implements NotationPart {
    private TokenPart tokenPart;

    public StringTokenPart(TokenPart tokenPart) {
        super(null);
        this.tokenPart = tokenPart;
    }

    @Exclude
    public StringTokenPart(Object sourceElement) {
        super(sourceElement);
        this.tokenPart = null;
    }

    //needed for XML binding
    @Exclude
    private StringTokenPart() {
        super(null);
    }

    public TokenPart getTokenPart() {
        return tokenPart;
    }

    @Override
    public String toString() {
        return this.tokenPart.toString();
    }
}
