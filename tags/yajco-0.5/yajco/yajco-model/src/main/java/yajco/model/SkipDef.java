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

    public String getRegexp() {
        return regexp;
    }
}
