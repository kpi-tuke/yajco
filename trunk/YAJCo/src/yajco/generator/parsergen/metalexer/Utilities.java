/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package yajco.generator.parsergen.metalexer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import yajco.grammar.TerminalSymbol;

/**
 *
 * @author DeeL
 */
public final class Utilities {
    
    private Utilities() {
    }
    
    public static boolean isCyclicRegex(String regex) {
        Pattern pattern = Pattern.compile("([^\\[]+[?+*][^\\]]*)"); // TODO: prehodnotit regularny vyraz na hladanie cyklickosti regularneho vyrazu
        Matcher matcher = pattern.matcher(regex);
        return matcher.find();
        //return false;
    }
    
}
