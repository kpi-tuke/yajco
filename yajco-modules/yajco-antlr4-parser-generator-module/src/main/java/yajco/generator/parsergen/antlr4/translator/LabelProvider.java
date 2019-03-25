package yajco.generator.parsergen.antlr4.translator;

import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to provide unique labels to elements in generated grammars
 * for distinction in semantic actions.
 */
class LabelProvider {
    private Map<String, Integer> counters = new HashMap<>();

    public String createLabel(String ruleName) {
        if (counters.containsKey(ruleName)) {
            counters.put(ruleName, counters.get(ruleName) + 1);
        } else {
            counters.put(ruleName, 1);
        }
        return ruleName + "_" + counters.get(ruleName);
    }
}
