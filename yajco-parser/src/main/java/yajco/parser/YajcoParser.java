package yajco.parser;

import yajco.parser.beaver.YajcoParserScanner;

public class YajcoParser {
	private static yajco.parser.beaver.YajcoParser parser;

	public yajco.model.Language parse(String input) throws LALRParseException {
		YajcoParserScanner scanner = new YajcoParserScanner(input);
		if (parser == null) {
			parser = new yajco.parser.beaver.YajcoParser();
		}

		try {
			yajco.ReferenceResolver referenceResolver = yajco.ReferenceResolver.createInstance();
			yajco.model.Language root = ((yajco.parser.beaver.SymbolWrapper<yajco.model.Language>) parser.parse(scanner)).getWrappedObject();
			referenceResolver.resolveReferences();
			return root;
		} catch (java.io.IOException e) {
			throw new LALRParseException("Problem parsing source code ", e);
		} catch (yajco.parser.beaver.YajcoParser.Exception e) {
			throw new LALRParseException("Problem parsing source code ", e);
		}
	}

	public yajco.model.Language parse(java.io.Reader reader) throws LALRParseException {
		try {
			return parse(readAsString(reader));
		} catch(java.io.IOException e) {
			throw new LALRParseException("Problem reading input file", e);
		}
	}

	private String readAsString(java.io.Reader r) throws java.io.IOException {
		StringBuilder sb = new StringBuilder();
		java.io.BufferedReader br = new java.io.BufferedReader(r);
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		return sb.toString();
	}
}