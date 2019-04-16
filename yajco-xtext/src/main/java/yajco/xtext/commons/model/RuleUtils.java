package yajco.xtext.commons.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RuleUtils {

    public static String makeCamelCaseName(String name) {
        if (name != null) {
            List<String> strings = Arrays.asList(name.split("\\."));
            return String.join("", strings.stream().map(RuleUtils::toCapital).collect(Collectors.toList()));
        }
        return null;
    }

    public static String toCapital(String name) {
        if (name != null && !name.isEmpty()) {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        } else
            return "";
    }
}
