package yajco.model.pattern;

import java.util.ArrayList;
import java.util.List;

public class PatternSupport<T extends Pattern> {
    private List<T> patterns;

    public PatternSupport() {
        this.patterns = new ArrayList<T>();
    }

    public PatternSupport(List<T> patterns) {
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
