package yajco.model.pattern;

import java.util.ArrayList;
import java.util.List;
import yajco.model.YajcoModelElement;

public class PatternSupport<T extends Pattern> extends YajcoModelElement {
    private List<T> patterns;

    public PatternSupport(Object sourceElement) {
        super(sourceElement);
        this.patterns = new ArrayList<T>();
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

    //public T getPattern(Class<? extends Pattern> clazz) {
    //public <R extends T> T getPattern(Class<R> clazz) {
    public T getPattern(Class<? extends T> clazz) {
        for (T pattern : patterns) {
            if (pattern.getClass().equals(clazz)) {
                return pattern;
            }
        }

        return null;
    }
}
