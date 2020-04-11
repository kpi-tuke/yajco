package yajco.model.pattern;

import yajco.model.YajcoModelElement;

import java.util.ArrayList;
import java.util.List;

public class PatternSupport<T extends Pattern> extends YajcoModelElement {
    private final List<T> patterns;

    public PatternSupport(Object sourceElement) {
        super(sourceElement);
        this.patterns = new ArrayList<>();
    }

    public PatternSupport(List<T> patterns, Object sourceElement) {
        super(sourceElement);
        this.patterns = patterns;
    }

    public List<T> getPatterns() {
        return patterns;
    }

    public void addPattern(T pattern) {
        patterns.add(pattern);
    }

    /**
     * Get a pattern of <b>exactly</b> the given type from the patterns contained in this object.
     * For example {@link yajco.model.Concept Concept} has {@link ConceptPattern ConceptPatterns} (type T)
     * and you want to get an {@link yajco.model.pattern.impl.Operator Operator pattern} (type R - a subtype of
     * ConceptPattern). The returned pattern is already cast to the requested type R.
     *
     * @param clazz class of the pattern you are searching
     * @param <R>   Type of the pattern you are looking for.
     *              It can be any subtype of {@link T} held in this object.
     * @return searched pattern if present, otherwise null
     */
    public <R extends T> R getPattern(Class<R> clazz) {
        for (T pattern : patterns) {
            if (pattern.getClass().equals(clazz)) {
                //noinspection unchecked
                return (R) pattern;
            }
        }

        return null;
    }
}
