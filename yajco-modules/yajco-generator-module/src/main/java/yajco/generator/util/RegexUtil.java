package yajco.generator.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexUtil {

    private static Pattern cyclicTestPattern = Pattern.compile("[^\\[].*[^\\\\][+*?].*[^\\]]?");

    public static boolean isCyclic(String regex) {
        return cyclicTestPattern.matcher(regex).find();
    }
    
    public static boolean isCyclic(Pattern regex) {
        return isCyclic(regex.pattern());
    }
    
    public static Map<String,String> filterMap(Map<String,String> map, boolean cyclic) {
        Map<String,String> filteredMap = new HashMap<String, String>(map);
        for (Map.Entry<String, String> entry : map.entrySet()) {
//            System.out.println(">>Regex: "+entry.getValue() + " isCyclic: "+isCyclic(entry.getValue()));
            if (isCyclic(entry.getValue()) != cyclic) {
                filteredMap.remove(entry.getKey());
            }
        }
        return filteredMap;
    }
}
