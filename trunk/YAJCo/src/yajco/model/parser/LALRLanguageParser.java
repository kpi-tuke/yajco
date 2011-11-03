package yajco.model.parser;

import yajco.model.parser.beaver.LALRLanguageParserScanner;

public class LALRLanguageParser {
	private static yajco.model.parser.beaver.LALRLanguageParser parser;

	public yajco.model.Language parse(String input) throws LALRParseException {
		LALRLanguageParserScanner scanner = new LALRLanguageParserScanner(input);
		if (parser == null) {
			parser = new yajco.model.parser.beaver.LALRLanguageParser();
		}

		try {
			yajco.ReferenceResolver referenceResolver = yajco.ReferenceResolver.createInstance();
			yajco.model.Language root = ((yajco.model.parser.beaver.SymbolWrapper<yajco.model.Language>) parser.parse(scanner)).getWrappedObject();
			referenceResolver.resolveReferences();
			return root;
		} catch (java.io.IOException e) {
			throw new LALRParseException("Problem parsing source code ", e);
		} catch (yajco.model.parser.beaver.LALRLanguageParser.Exception e) {
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