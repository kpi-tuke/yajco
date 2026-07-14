package yajco.grammar.translator;

import org.junit.Test;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.bnf.Grammar;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.Notation;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.pattern.impl.BooleanValue;
import yajco.model.type.PrimitiveType;
import yajco.model.type.PrimitiveTypeConst;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class YajcoModelToBNFGrammarTranslatorTest {

    @Test
    public void shouldGenerateEmptyAlternativeForBooleanFlagPattern() {
        Property flag = new Property("isFinal", new PrimitiveType(PrimitiveTypeConst.BOOLEAN), null);
        PropertyReferencePart flagPart = new PropertyReferencePart(flag, null);
        flagPart.addPattern(new BooleanValue(new String[] {"initial"}, new String[0], null));

        Notation notation = new Notation(new yajco.model.NotationPart[] {flagPart});
        Concept concept = new Concept("Member", new Property[] {flag}, new Notation[] {notation});
        Language language = new Language("test", new ArrayList<>(), new ArrayList<>(), Arrays.asList(concept));

        Grammar grammar = YajcoModelToBNFGrammarTranslator.getInstance().translate(language);

        NonterminalSymbol booleanFlag = new NonterminalSymbol("BooleanValue_1", new PrimitiveType(PrimitiveTypeConst.BOOLEAN));
        assertNotNull(grammar.getProduction(booleanFlag));
        assertEquals(2, grammar.getProduction(booleanFlag).getRhs().size());
        assertEquals("", grammar.getProduction(booleanFlag).getRhs().get(1).toString());
    }
}
