package yajco.parser;

public class Parser {
  private static yajco.parser.javacc.Parser _parser;

  public yajco.model.Language parse(String input) throws ParseException {
    yajco.parser.javacc.ParserTokenManager tm = new yajco.parser.javacc.ParserTokenManager(input);
    if (_parser == null) {
      _parser = new yajco.parser.javacc.Parser(tm);
    } else {
      _parser.ReInit(tm);
    }

    try {
      yajco.ReferenceResolver referenceResolver = yajco.ReferenceResolver.createInstance();
      yajco.model.Language root = yajco.parser.javacc.Parser.parse();
      referenceResolver.resolveReferences();
      return root;
    } catch (yajco.parser.javacc.ParseException e) {
      throw new ParseException("Problem parsing source code ", e);
    }
  }

  public yajco.model.Language parse(java.io.Reader reader) throws ParseException {
    try {
      return parse(readAsString(reader));
    } catch(java.io.IOException e) {
      throw new ParseException("Problem reading input file", e);
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
