package yajco.model;

import yajco.annotation.Exclude;
import yajco.annotation.Token;

public class SkipDef extends YajcoModelElement {

    private String regexp;

    public SkipDef(@Token("STRING_VALUE") String regexp) {
        super(null);
        this.regexp = regexp;
    }

    @Exclude
    public SkipDef(String regexp, Object sourceElement) {
        super(sourceElement);
        this.regexp = regexp;
    }
    
    //needed for XML binding
    @Exclude
    private SkipDef() {
        super(null);
    }

    public String getRegexp() {
        return regexp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SkipDef)) {
            return false;
        }
        SkipDef that = (SkipDef) obj;
        if (this.regexp == null && that.regexp == null) {
            return true;
        }
        return this.regexp.equals(that.regexp);
    }
    
    
}
