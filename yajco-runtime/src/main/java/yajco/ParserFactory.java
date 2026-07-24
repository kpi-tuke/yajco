package yajco;

import yajco.parser.ParseException;
import yajco.parser.Parser;

import java.util.ServiceLoader;

public class ParserFactory {
    @SuppressWarnings("unchecked")
    public static <T> Parser<T, ? extends ParseException> getParser(Class<T> mainNodeType) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (var p : ServiceLoader.load(Parser.class, cl)) {
            if (p.mainNodeType().equals(mainNodeType)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No parser for " + mainNodeType.getName());
    }
}
