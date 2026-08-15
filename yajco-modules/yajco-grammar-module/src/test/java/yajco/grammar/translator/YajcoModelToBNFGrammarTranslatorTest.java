package yajco.grammar.translator;

import org.junit.Test;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.semlang.AddSharedElementsToCollectionAction;
import yajco.grammar.semlang.Action;
import yajco.grammar.semlang.ActionType;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.Notation;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenDef;
import yajco.model.pattern.impl.BooleanValue;
import yajco.model.pattern.impl.Shared;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.ReferenceType;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void shouldRejectSharedConceptWithMultipleNotations() {
        Property value = new Property("value", new PrimitiveType(PrimitiveTypeConst.STRING), null);
        Property suffix = new Property("suffix", new PrimitiveType(PrimitiveTypeConst.STRING), null);
        PropertyReferencePart sharedSuffix = new PropertyReferencePart(suffix, null);
        sharedSuffix.addPattern(new Shared(","));

        Notation sharedNotation = new Notation(new yajco.model.NotationPart[] {
                new PropertyReferencePart(value, null), sharedSuffix
        });
        Notation otherNotation = new Notation(new yajco.model.NotationPart[] {
                new PropertyReferencePart(value, null)
        });
        Concept item = new Concept("Item", new Property[] {value, suffix}, new Notation[] {
                sharedNotation, otherNotation
        });

        Property items = new Property("items", new ListType(new ReferenceType(item, null)), null);
        Concept root = new Concept("Root", new Property[] {items}, new Notation[] {
                new Notation(new yajco.model.NotationPart[] {new PropertyReferencePart(items, null)})
        });
        Language language = new Language("test", new ArrayList<>(), new ArrayList<>(), Arrays.asList(root, item));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> YajcoModelToBNFGrammarTranslator.getInstance().translate(language));

        assertEquals("@Shared in concept 'Item' requires exactly one notation; found 2.", exception.getMessage());
    }

    @Test
    public void shouldGenerateSharedCollectionAction() {
        Property value = new Property("value", new PrimitiveType(PrimitiveTypeConst.STRING), null);
        Property suffix = new Property("suffix", new PrimitiveType(PrimitiveTypeConst.STRING), null);
        PropertyReferencePart sharedSuffix = new PropertyReferencePart(suffix, null);
        sharedSuffix.addPattern(new Shared(","));
        Concept item = new Concept("Item", new Property[] {value, suffix}, new Notation[] {
                new Notation(new yajco.model.NotationPart[] {
                        new PropertyReferencePart(value, null), sharedSuffix
                })
        });

        Property items = new Property("items", new ListType(new ReferenceType(item, null)), null);
        Concept root = new Concept("Root", new Property[] {items}, new Notation[] {
                new Notation(new yajco.model.NotationPart[] {new PropertyReferencePart(items, null)})
        });
        Language language = new Language(
                "test",
                Arrays.asList(new TokenDef("VALUE", "[a-z]+"), new TokenDef("SUFFIX", "[a-z]+")),
                new ArrayList<>(),
                Arrays.asList(root, item));

        Grammar grammar = YajcoModelToBNFGrammarTranslator.getInstance().translate(language);

        NonterminalSymbol sharedGroup = grammar.getNonterminal("ItemWithSharedPartSuffix");
        assertNotNull(sharedGroup);
        Action generatedAction = grammar
                .getProduction(sharedGroup)
                .getRhs()
                .get(0)
                .getActions()
                .get(2);
        assertEquals(ActionType.ADD_SHARED_ELEMENTS_TO_COLLECTION, generatedAction.getActionType());
        AddSharedElementsToCollectionAction action = (AddSharedElementsToCollectionAction) generatedAction;
        assertEquals("list", action.getCollection().getVarName());
        assertEquals(2, action.getConstructorParameters().size());
        assertEquals("value", action.getConstructorParameters().get(0).getSymbol().getVarName());
        assertEquals("suffix", action.getConstructorParameters().get(1).getSymbol().getVarName());
    }
}
