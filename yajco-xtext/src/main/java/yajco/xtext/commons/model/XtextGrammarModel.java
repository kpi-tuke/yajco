package yajco.xtext.commons.model;

import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.SkipDef;
import yajco.model.TokenDef;
import yajco.model.pattern.ConceptPattern;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.xtext.commons.regex.XtextRegexCompiler;
import yajco.xtext.commons.settings.XtextProjectSettings;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class XtextGrammarModel {
    private final Language language;
    private final XtextProjectSettings settings;
    private String grammarDeclaration;
    private String generateDeclaration;

    private List<Rule> rules;
    private Map<String, String> declaratorMap = new HashMap<>();
    private HashMap<Integer, List<Rule>> operatorPriorityMap = new HashMap<>();
    private static String PARENTHESES = "Parentheses";
    private static final String TERM = "_TERMINAL";
    private String wsTerminalBody;
    private Map<String, String> terminalsMap = new LinkedHashMap<>();
    private XtextRegexCompiler regexCompiler = new XtextRegexCompiler();


    public XtextGrammarModel(Language language) {
        rules = new ArrayList<>();
        this.language = language;
        this.settings = XtextProjectSettings.getInstance();
    }

    public void createModel() {
        createGrammarDefinition();
        createRules(language.getConcepts());
        createWsTerminalBody(language.getSkips());
        createTerminalsMap(language.getTokens());
    }
    public String getWsTerminalBody() {
        return wsTerminalBody;
    }

    public Map<String, String> getTerminalsMap() {
        return terminalsMap;
    }


    private void createGrammarDefinition() {
        grammarDeclaration = "grammar " + settings.getLanugageFullName()
                + " with org.eclipse.xtext.common.Terminals\n\n";
        generateDeclaration = "generate " + settings.getGrammarName() +
                "Grammar \"http://www.example.org/" + settings.getGrammarName().toLowerCase() + "/" +
                settings.getMainNode() + "Grammar\"\n\n";
    }

    private void createRules(List<Concept> concepts) {
        rules = new ArrayList<>();
        Rule rule;
        for (int i = concepts.size() - 1; i >= 0; i--) {
            Concept currentConcept = concepts.get(i);
            if (getSimpleName(currentConcept.getName()).equals(settings.getMainNode())) {
                rule = initializeMainRule(currentConcept);
            } else if (currentConcept.getConcreteSyntax() == null || currentConcept.getConcreteSyntax().isEmpty()) {
                rule = initializeDeclaratorRule(currentConcept);
            } else {
                rule = initializeReturnerOrOperatorRule(currentConcept);
            }
            rules.add(rule);

            ConceptPattern parentheses = currentConcept.getPattern(Parentheses.class);
            if (parentheses != null) {
                rules.add(initializeParenthesesRule((Parentheses) parentheses, rule));
            }

        }
        rules = editRules(rules);
        rules = finishEditingRules(rules);
    }

    private Rule initializeMainRule(Concept concept) {
        Rule rule = new Rule();
        rule.setType(RuleType.MAIN);
        rule.setName(concept.getName());
        rule.setAs(concept.getAbstractSyntax());
        rule.setCs(concept.getConcreteSyntax());
        return rule;
    }

    private Rule initializeDeclaratorRule(Concept concept) {
        Rule rule = new Rule();
        rule.setType(RuleType.DECLARATOR);
        rule.setName(concept.getName());
        rule.setParent(concept.getParent() != null && !concept.getParent().getName().isEmpty() ?
                concept.getParent().getName() : rule.getName());

        return rule;
    }

    private Rule initializeReturnerOrOperatorRule(Concept concept) {
        Rule rule = new Rule();
        rule.setType(RuleType.RETURNER);
        rule.setName(concept.getName());
        rule.setAs(concept.getAbstractSyntax());
        rule.setCs(concept.getConcreteSyntax());
        if (concept.getParent() != null && !concept.getParent().getName().isEmpty()) {
            rule.setParent(concept.getParent().getName());
        }
        ConceptPattern pattern = concept.getPattern(Operator.class);
        if (pattern != null) {
            rule.setType(RuleType.OPERATOR);
            rule.setAssociativity(((Operator) pattern).getAssociativity());
            rule.setPriority(((Operator) pattern).getPriority());
        }
        return rule;
    }


    private Rule initializeParenthesesRule(Parentheses parentheses, Rule rule) {
        Rule parenthesesRule = new Rule();
        parenthesesRule.setName(rule.getName() + PARENTHESES);
        String parent = rule.getParent() != null && !rule.getParent().isEmpty() ?
                rule.getParent() : rule.getName();
        parenthesesRule.setParent(parent);
        parenthesesRule.setParentheses(parentheses);
        parenthesesRule.setType(RuleType.PARENTHESES);
        return parenthesesRule;
    }

    private List<Rule> editRules(List<Rule> rules) {
        List<Rule> newRules = new ArrayList<>();
        int declaratorAndMainCounter = 0;

        for (Rule rule : rules) {
            if (rule.getType() == RuleType.RETURNER) {
                newRules.add(newRules.size(), rule);
            } else if (rule.getType() == RuleType.MAIN) {
                newRules.add(0, rule);
                declaratorAndMainCounter++;
                if (rule.getCs() == null || rule.getCs().isEmpty()) {
                    addRuleToDeclaratorMap(rule);
                }
            } else if (rule.getType() == RuleType.DECLARATOR) {
                addRuleToDeclaratorMap(rule);
                newRules.add(rule);
            } else if (rule.getType() == RuleType.OPERATOR) {
                addRuleToPriorityMap(rule);
            } else if (rule.getType() == RuleType.PARENTHESES) {
                newRules.add(rule);
            }
        }
        newRules.addAll(declaratorAndMainCounter, mergeOperatorRules());

        changeReturnTypes(newRules);
        List<Rule> declarationsRules = createRulesForDeclarators(newRules);
        newRules.addAll(declarationsRules);

        newRules.addAll(newRules.size() - 1, createNewRulesFromAllTheReturners(
                rules.stream().filter(rule -> rule.getType() == RuleType.RETURNER ||
                        rule.getType() == RuleType.PARENTHESES).collect(Collectors.toList())));

        newRules.sort(Comparator.comparingInt(o -> o.getType().getValue()));

        return newRules;
    }

    private List<Rule> finishEditingRules(List<Rule> rules) {
        for (int i = 0; i < rules.size(); i++) {
            Rule currentRule = rules.get(i);
            if (currentRule.getType() == RuleType.MAIN) {
                if (currentRule.getCs() != null) {
                    String parentName = currentRule.getParent() != null ?
                            currentRule.getParent() : currentRule.getName();
                    Rule rule = rules.stream()
                            .filter(nextOperator -> nextOperator.getType() == RuleType.OPERATOR &&
                                    nextOperator.getParent().equals(parentName)).findFirst().orElse(null);
                    if (rule != null) {
                        currentRule.setNext(rule);
                    } else {
                        rule = rules.stream()
                                .filter(nextOperator -> nextOperator.getType() == RuleType.RETURNER_AGGREGATOR &&
                                        nextOperator.getParent().equals(parentName)).findFirst().orElse(null);
                        if (rule != null) {
                            currentRule.setNext(rule);
                        }
                    }

                }
            } else if (currentRule.getType() == RuleType.OPERATOR) {
                Rule nextRule = rules.get(i + 1);
                if (nextRule.type == RuleType.OPERATOR) {
                    currentRule.setNext(nextRule);
                } else {
                    rules.stream()
                            .filter(nxt -> nxt.type == RuleType.RETURNER_AGGREGATOR &&
                                    nextRule.parent.equals(nxt.parent)).findFirst().ifPresent(currentRule::setNext);
                }
            } else if (currentRule.getType() == RuleType.PARENTHESES) {
                currentRule.setRightParenthesis(getRightParenthesis(currentRule));
                currentRule.setLeftParenthesis(getLeftParenthesis(currentRule));
            }
        }
        return rules;
    }

    private void addRuleToDeclaratorMap(Rule rule) {
        if (rule.getParent() != null && !rule.getParent().isEmpty()) {
            this.declaratorMap.put(rule.getName(), rule.getParent());
        } else {
            this.declaratorMap.put(rule.getName(), rule.getName());
        }
    }

    private List<Rule> createRulesForDeclarators(List<Rule> rules) {
        List<Rule> declarators = rules.stream()
                .filter(nxt -> nxt.getType() == RuleType.DECLARATOR)
                .collect(Collectors.toList());

        List<Rule> newRules = new ArrayList<>();
        for (Rule dec : declarators) {
            SuperRule superRule;
            Rule nextUsageRule = rules.stream()
                    .filter(nxt -> nxt.getType() == RuleType.OPERATOR && dec.getName().equals(nxt.getParent()))
                    .findFirst().orElse(null);

            if (nextUsageRule != null) {
                superRule = new SuperRule(dec, asList(nextUsageRule));
            } else {
                List<Rule> rules1 = rules.stream()
                        .filter(nxt -> dec.getName().equals(nxt.getParent()) && nxt.getType() != RuleType.PROCESSED)
                        .collect(Collectors.toList());
                superRule = new SuperRule(dec, rules1);
            }

            newRules.add(superRule);
        }
        return newRules;
    }

    private void changeReturnTypes(List<Rule> rules) {
        boolean changed = false;
        for (Rule rule : rules) {
            if (rule.getType() == RuleType.RETURNER || rule.getType() == RuleType.OPERATOR || rule.getType() == RuleType.DECLARATOR){
                String newParent = this.declaratorMap.get(rule.getParent());
                if (newParent != null && !newParent.isEmpty() && !newParent.equals(rule.getParent())) {
                    rule.setParent(newParent);
                    changed = true;
                }
            }
        }
        if (changed){
            changeReturnTypes(rules);
        }
    }

    private List<Rule> mergeOperatorRules() {
        Set<Integer> keys = this.operatorPriorityMap.keySet();
        List<Rule> rules;
        List<Rule> newRules = new ArrayList<>();
        for (Integer key : keys) {
            rules = this.operatorPriorityMap.get(key);
            newRules.add(new SuperRule(rules, RuleType.OPERATOR));
        }
        return newRules;
    }

    private List<Rule> createNewRulesFromAllTheReturners(List<Rule> returners) {
        Set<String> parents = new HashSet<>(this.declaratorMap.values());
        List<Rule> newRules = new ArrayList<>();
        for (String parent : parents) {
            if (parent != null) {
                List<Rule> rules = returners.stream().filter(ret -> parent.equals(ret.getParent())).collect(Collectors.toList());
                if (!rules.isEmpty()) {
                    SuperRule superRule = new SuperRule(rules, RuleType.RETURNER_AGGREGATOR);
                    newRules.add(superRule);
                }
            }
        }
        return newRules;
    }

    private void addRuleToPriorityMap(Rule rule) {
        List<Rule> rulesByPriority = this.operatorPriorityMap.get(rule.getPriority());
        if (rulesByPriority == null) {
            List<Rule> rules = new ArrayList<>();
            rules.add(rule);
            this.operatorPriorityMap.put(rule.getPriority(), rules);
        } else {
            rulesByPriority.add(rule);
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public String getGrammarDeclaration() {
        return grammarDeclaration;
    }

    public String getGenerateDeclaration() {
        return generateDeclaration;
    }

    private String findTokenFromString(String str) {
        return this.language.getTokens().stream()
                .filter(token ->
                        str.equals(regexCompiler.compileRegex(token.getRegexp())
                                .substring(regexCompiler.compileRegex(token.getRegexp()).indexOf('\'') + 1,
                                        regexCompiler.compileRegex(token.getRegexp()).lastIndexOf('\''))))
                .map(TokenDef::getName).findFirst().orElse(str);
    }

    private String getLeftParenthesis(Rule currentRule) {
        String leftName = this.language.getTokens().stream().filter(token -> token.getName().equals(
                currentRule.parentheses.getLeft().toUpperCase()))
                .map(TokenDef::getName).findFirst().orElse("");
        if (!leftName.isEmpty()) {
            return leftName + TERM;
        } else {
            leftName = findTokenFromString(currentRule.parentheses.getLeft());
            if (leftName.equals(currentRule.parentheses.getLeft())) {
                return "\"" + leftName + "\"";
            } else {
                return leftName + TERM;
            }
        }
    }

    private String getRightParenthesis(Rule currentRule) {
        String rightName = this.language.getTokens().stream().filter(token -> token.getName().equals(
                currentRule.parentheses.getRight().toUpperCase()))
                .map(TokenDef::getName).findFirst().orElse("");
        if (!rightName.isEmpty()) {
            return rightName + TERM;
        } else {
            rightName = findTokenFromString(currentRule.parentheses.getRight());
            if (rightName.equals(currentRule.parentheses.getRight())) {
                return "\"" + rightName + "\"";
            } else {
                return rightName + TERM;
            }
        }
    }

    private void createTerminalsMap(List<TokenDef> tokens) {
        for (TokenDef tokenDef : tokens) {
            terminalsMap.put(tokenDef.getName().toUpperCase(), this.regexCompiler.compileRegex(tokenDef.getRegexp()));
        }
    }

    private void createWsTerminalBody(List<SkipDef> skips) {
        if (skips != null && !skips.isEmpty()) {
            this.wsTerminalBody = String.join(" | ",
                    skips.stream().map(
                            skip -> this.regexCompiler.compileRegex(skip.getRegexp())
                    ).collect(toList()));
        }

    }

    private String getSimpleName(String name){
        return name.lastIndexOf(".") != -1 ? name.substring(name.lastIndexOf(".") + 1) : name;
    }


}
