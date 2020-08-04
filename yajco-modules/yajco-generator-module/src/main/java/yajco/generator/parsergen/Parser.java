package yajco.generator.parsergen;

import java.io.Reader;

public interface Parser<T, E extends Exception> {
	T parse(String input) throws E;
	T parse(Reader reader) throws E;
}
