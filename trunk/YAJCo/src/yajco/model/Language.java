package yajco.model;

import java.util.ArrayList;
import java.util.List;
import tuke.pargen.annotation.Before;
import tuke.pargen.annotation.Exclude;
import tuke.pargen.annotation.Range;
import tuke.pargen.annotation.Token;
import yajco.Utilities;

public class Language {

	private String name;
	private List<Concept> concepts;
	private List<TokenDef> tokens;
	private List<String> skips;

	public Language(
			@Before("tokens") @Range(minOccurs = 0) List<TokenDef> tokens,
			@Before("skips") @Range(minOccurs = 0) @Token("STRING_VALUE") List<String> skips,
			@Range(minOccurs = 1) Concept[] concepts) {
		this.tokens = tokens;
		this.skips = skips;
		this.concepts = Utilities.asList(concepts);
	}

	public Language(
			@Before("language") String name,
			@Before("tokens") @Range(minOccurs = 0) List<TokenDef> tokens,
			@Before("skips") @Range(minOccurs = 0) @Token("STRING_VALUE") List<String> skips,
			@Range(minOccurs = 1) Concept[] concepts) {
		this.name = name;
		this.tokens = tokens;
		this.skips = skips;
		this.concepts = Utilities.asList(concepts);
	}

	@Exclude
	public Language() {
		concepts = new ArrayList<Concept>();
	}

	public String getName() {
		return name;
	}

	public List<Concept> getConcepts() {
		return concepts;
	}

	public Concept getConcept(String name) {
		for (Concept concept : concepts) {
			if (concept.getName().equals(name)) {
				return concept;
			}
		}
		return null;
	}

	public void addConcept(Concept concept) {
		concepts.add(concept);
	}

	public List<String> getSkips() {
		return skips;
	}

	public void setSkips(List<String> skips) {
		this.skips = skips;
	}

	public List<TokenDef> getTokens() {
		return tokens;
	}

	public void setTokens(List<TokenDef> tokens) {
		this.tokens = tokens;
	}

	public TokenDef getToken(String name) {
		for (TokenDef token : tokens) {
			if (token.getName().equals(name)) {
				return token;
			}
		}
		return null;
	}
}
