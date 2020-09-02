package yajco.generator.parsergen;

import java.io.Reader;

/**
 * Is a service as per the SPI contract.
 *
 * @param <T> type of the main (root) node in the parsed AST (sentence)
 * @param <E> specific parser exception
 * @see java.util.ServiceLoader
 */
public interface Parser<T, E extends Exception> {
	T parse(String input) throws E;

	T parse(Reader reader) throws E;
}
