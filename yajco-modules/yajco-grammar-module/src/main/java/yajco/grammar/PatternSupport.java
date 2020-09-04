package yajco.grammar;

import java.util.ArrayList;
import java.util.List;
import yajco.model.pattern.Pattern;

public abstract class PatternSupport {

    private List<Pattern> patterns;

    public PatternSupport(List<Pattern> patterns) {
        this.patterns = patterns != null ? patterns : new ArrayList<Pattern>();
    }

    public void addPattern(Pattern pattern) {
        if (pattern != null) {
            patterns.add(pattern);
        }
    }

    public <T extends Pattern> T getPattern(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }

        for (Pattern pattern : patterns) {
            if (pattern.getClass().equals(clazz)) {
                //noinspection unchecked
                return (T) pattern;
            }
        }

        return null;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }
}
