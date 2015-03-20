package yajco.parser.beaver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import beaver.Symbol;
import beaver.Scanner;
import yajco.parser.beaver.YajcoParser.Terminals;

public class YajcoParserScanner extends Scanner {

	private int line = 1;
	private int column = 1;
	private String input;
        private int position = 0;
	private static final Map<Short, Pattern> tokens = new LinkedHashMap<Short, Pattern>();
	private static final List<Pattern> skips = new ArrayList<Pattern>();

	static {
		tokens.put(Terminals.SYMBOLCONCEPT, Pattern.compile("concept"));
		tokens.put(Terminals.SYMBOLIDENTIFIER, Pattern.compile("Identifier"));
		tokens.put(Terminals.SYMBOLSEPARATOR, Pattern.compile("Separator"));
		tokens.put(Terminals.SYMBOLPROPERTY, Pattern.compile("property"));
		tokens.put(Terminals.SYMBOL_58, Pattern.compile("[:]"));
		tokens.put(Terminals.SYMBOL_46__46, Pattern.compile("[.][.]"));
		tokens.put(Terminals.SYMBOLOPERATOR, Pattern.compile("Operator"));
		tokens.put(Terminals.SYMBOLPRIORITY, Pattern.compile("priority"));
		tokens.put(Terminals.SYMBOLCS, Pattern.compile("CS"));
		tokens.put(Terminals.SYMBOLNONE, Pattern.compile("NONE"));
		tokens.put(Terminals.SYMBOL_124, Pattern.compile("[|]"));
		tokens.put(Terminals.SYMBOL_125, Pattern.compile("[}]"));
		tokens.put(Terminals.SYMBOLMETHOD, Pattern.compile("method"));
		tokens.put(Terminals.SYMBOLLEFT, Pattern.compile("LEFT"));
		tokens.put(Terminals.SYMBOLLANGUAGE, Pattern.compile("language"));
		tokens.put(Terminals.SYMBOL_123, Pattern.compile("[{]"));
		tokens.put(Terminals.SYMBOLPARENTHESES, Pattern.compile("Parentheses"));
		tokens.put(Terminals.SYMBOLINDENT, Pattern.compile("Indent"));
		tokens.put(Terminals.SYMBOLTOKENS, Pattern.compile("tokens"));
		tokens.put(Terminals.SYMBOL_41, Pattern.compile("[)]"));
		tokens.put(Terminals.SYMBOLAS, Pattern.compile("AS"));
		tokens.put(Terminals.SYMBOL_42, Pattern.compile("[*]"));
		tokens.put(Terminals.SYMBOLSKIPS, Pattern.compile("skips"));
		tokens.put(Terminals.SYMBOLASSOCIATIVITY, Pattern.compile("associativity"));
		tokens.put(Terminals.SYMBOL_61, Pattern.compile("[=]"));
		tokens.put(Terminals.SYMBOL_44, Pattern.compile("[,]"));
		tokens.put(Terminals.SYMBOLOF, Pattern.compile("of"));
		tokens.put(Terminals.SYMBOLTOKEN, Pattern.compile("Token"));
		tokens.put(Terminals.SYMBOL_40, Pattern.compile("[(]"));
		tokens.put(Terminals.SYMBOLRIGHT, Pattern.compile("RIGHT"));
		tokens.put(Terminals.SYMBOLREFERENCES, Pattern.compile("References"));
		tokens.put(Terminals.SYMBOLAUTO, Pattern.compile("AUTO"));
		tokens.put(Terminals.SYMBOLLIST, Pattern.compile("list"));
		tokens.put(Terminals.SYMBOLENUM, Pattern.compile("Enum"));
		tokens.put(Terminals.SYMBOLSET, Pattern.compile("set"));
		tokens.put(Terminals.SYMBOLSETTINGS, Pattern.compile("settings"));
		tokens.put(Terminals.SYMBOLARRAY, Pattern.compile("array"));
		tokens.put(Terminals.SYMBOLRANGE, Pattern.compile("Range"));
		tokens.put(Terminals.SYMBOLNEWLINE, Pattern.compile("NewLine"));
		tokens.put(Terminals.SYMBOLFACTORY, Pattern.compile("Factory"));

		tokens.put(Terminals.INTEGER, Pattern.compile("int"));
		tokens.put(Terminals.REAL, Pattern.compile("real"));
		tokens.put(Terminals.BOOLEAN, Pattern.compile("boolean"));
		tokens.put(Terminals.STRING, Pattern.compile("string"));
		tokens.put(Terminals.NAME, Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)|\\[([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\]"));
		tokens.put(Terminals.INT_VALUE, Pattern.compile("[0-9]+"));
		tokens.put(Terminals.STRING_VALUE, Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\""));

		skips.add(Pattern.compile(" "));
		skips.add(Pattern.compile("\\t"));
		skips.add(Pattern.compile("\\n"));
		skips.add(Pattern.compile("\\r"));
		skips.add(Pattern.compile("//.*"));
	}

	public YajcoParserScanner(String input) {
		this.input = input;
                position = 0;
	}

	@Override
	public Symbol nextToken() throws IOException, Scanner.Exception {
		skipWhiteSpaces();

		if (input.length() == position) {
			return new Symbol(Terminals.EOF, line, column);
		}

		return findToken();
	}

	private void skipWhiteSpaces() {
		boolean matched;
		do {
			matched = false;
			Matcher matcher = null;
			for (Pattern skip : skips) {
				if (matcher == null) {
					matcher = skip.matcher(input);
				} else {
					matcher.usePattern(skip);
				}
                                matcher.useTransparentBounds(true);
                                matcher.region(position, input.length());
				if (matcher.lookingAt()) {
					//Consume the white space from the input
					consumeInput(matcher.group().length());
					matched = true;
					matcher = null;
					break;
				}
			}
		} while (matched);
	}

	private Symbol findToken() throws IOException, Scanner.Exception {
		Matcher matcher = null;
		int longest = 0;
		Symbol token = null;
		for (Map.Entry<Short, Pattern> entry : tokens.entrySet()) {
			if (matcher == null) {
				matcher = entry.getValue().matcher(input);
			} else {
				matcher.usePattern(entry.getValue());
			}
                        matcher.useTransparentBounds(true);
                        matcher.region(position, input.length());
			if (matcher.lookingAt()) {
				String group = matcher.group();
				if (longest < group.length()) {
					longest = group.length();
					for (int i = 1; i <= matcher.groupCount(); i++) {
						if (matcher.group(i) != null) {
							group = matcher.group(i);
							break;
						}
					}

					//Create token
					token = new Symbol(entry.getKey(), line, column, longest, group);
				}
			}
		}

		//Return the longest matching token, consume it from input
		if (token != null) {
			consumeInput(longest);
			return token;
        }

		Scanner.Exception exception = new Scanner.Exception(line, column, "Unrecognized character detected: '" + input.charAt(position) + "'!");
		// Beaver sa pokusa o error recovering, a preto je nutne, aby sme preskocili dany nespravny znak, pretoze ak
		// sa tak neucini, tak vznikne nekonecny cyklus a vypisy na konzolu s danou chybou
		consumeInput(1);
		throw exception;
	}

	private void consumeInput(int length) {
		for (int i = 0; i < length; i++) {
			char c = input.charAt(position+i);
			column++;
			if (c == '\n') {
				column = 1;
				line++;
			}
		}
                position = position + length;
	}
}