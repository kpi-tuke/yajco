package yajco;

import yajco.parser.ParseException;
import yajco.parser.Parser;
import yajco.parser.ParserFor;

import java.util.ServiceLoader;

public class ParserFactory {
    @SuppressWarnings("unchecked")
    public static <T> Parser<T, ? extends ParseException> getParser(Class<T> mainNodeType) {
        Parser<T, ? extends ParseException> match = ServiceLoader.load(Parser.class).stream()
            .filter(p -> isParserFor(p.type(), mainNodeType))
            .reduce((p1, p2) -> {
                throw new IllegalStateException(
                    "Cannot create the parser because multiple parser providers found for " + mainNodeType.getName()
                    + ": " + p1.type().getName() + " and " + p2.type().getName());
            })
            .map(ServiceLoader.Provider::get)
            .orElseThrow(() -> new IllegalArgumentException("No parser for " + mainNodeType.getName()));
        return match;
    }

    private static boolean isParserFor(Class<?> parser, Class<?> mainNodeType) {
        ParserFor annotation = parser.getAnnotation(ParserFor.class);
        if (annotation == null) {
            return false;
        }
        return annotation.value().equals(mainNodeType);
    }
}
