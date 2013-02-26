package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Token;
import yajco.model.pattern.NotationPartPattern;

public class Range extends NotationPartPattern {

	public static final int INFINITY = -1;
	private int minOccurs = 0;
	private int maxOccurs = INFINITY;

	@Before({"Range", "("})
	@After({"..", "*", ")"})
	public Range(@Token("INT_VALUE") int minOccurs) {
		super(null);
		this.minOccurs = minOccurs;
	}

	@Before({"Range", "("})
	@After(")")
	public Range(@Token("INT_VALUE") int minOccurs, @Before("..") @Token("INT_VALUE") int maxOccurs) {
		super(null);
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}

	//TODO: Ak bolo toto odkomentovane parser generator negeneroval dobre vystup
//    @Before({"Range", "(", "*", ")"})
//    public Range() {
//    }
	@Exclude
	public Range() {
		super(null);
	}

//DOMINIK TEST - neviem preco tu dodal tieto konstruktory vlastne
//	@Exclude
//	public Range(int minOccurs, Object sourceElement) {
//		super(sourceElement);
//		this.minOccurs = minOccurs;
//	}

	@Exclude
	public Range(int minOccurs, int maxOccurs, Object sourceElement) {
		super(sourceElement);
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}

	public int getMinOccurs() {
		return minOccurs;
	}

	public int getMaxOccurs() {
		return maxOccurs;
	}
}
