package yajco.model.grammar;

import java.util.ArrayList;
import java.util.List;
import yajco.model.YajcoModelElement;
import yajco.model.pattern.Pattern;

public class GrammarModelElement {

    private YajcoModelElement sourceElement;
    private List<Pattern> patterns;

    public GrammarModelElement(YajcoModelElement sourceElement) {
        this.sourceElement = sourceElement;
        this.patterns = new ArrayList<Pattern>();
    }

    public GrammarModelElement(List<Pattern> patterns, YajcoModelElement sourceElement) {
        this.sourceElement = sourceElement;
        this.patterns = patterns;
    }

    public void addPattern(Pattern pattern) {
        patterns.add(pattern);
    }

    public Pattern getPattern(Class<? extends Pattern> clazz) {
        for (Pattern pattern : patterns) {
            if (pattern.getClass().equals(clazz)) {
                return pattern;
            }
        }

        return null;
    }

    public boolean hasPattern(Class<? extends Pattern> clazz) {
        return getPattern(clazz) != null;
    }

    public YajcoModelElement getSourceElement() {
        return sourceElement;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }
}
