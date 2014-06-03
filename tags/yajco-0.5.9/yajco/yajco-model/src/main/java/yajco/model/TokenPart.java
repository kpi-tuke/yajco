package yajco.model;

import yajco.annotation.Exclude;

public class TokenPart extends YajcoModelElement implements NotationPart {

    private String token;

    public TokenPart(String stringValue) {
        super(null);
        this.token = stringValue;
    }

    @Exclude
    public TokenPart(String token, Object sourceElement) {
        super(sourceElement);
        this.token = token;
    }
    
    //needed for XML binding
    @Exclude
    private TokenPart() {
        super(null);
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "Token: "+token;
    }
    
    
}
