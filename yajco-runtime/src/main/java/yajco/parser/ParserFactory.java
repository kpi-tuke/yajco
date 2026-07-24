package yajco.parser;

import java.util.ServiceLoader;

public class ParserFactory {
    @SuppressWarnings("unchecked")
    public static <T> Parser<T, ? extends ParseException> getParser(Class<T> mainNodeType) {
        return ServiceLoader.load(Parser.class).stream()
            .filter(p -> isParserFor(p.type(), mainNodeType))
            .reduce((p1, p2) -> {
                throw new IllegalStateException(
                    "Cannot create the parser because multiple parser providers found for " + mainNodeType.getName()
                    + ": " + p1.type().getName() + " and " + p2.type().getName());
            })
            .map(ServiceLoader.Provider::get)
            .orElseThrow(() -> new IllegalArgumentException("No parser for " + mainNodeType.getName()));
    }

    private static boolean isParserFor(Class<?> parser, Class<?> mainNodeType) {
        ParserFor annotation = parser.getAnnotation(ParserFor.class);
        if (annotation == null) {
            return false;
        }
        return annotation.value().equals(mainNodeType);
    }
}
