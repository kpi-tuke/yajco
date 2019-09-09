package yajco.xtext.commons.model;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class SuperRule extends Rule {
    private List<Rule> rules;

    public SuperRule(List<Rule> rules, RuleType type) {
        this.type = type;
        this.rules = rules;
        this.parent = rules.get(0).parent;

        setSuperRules(this.rules);

        if (type == RuleType.OPERATOR) {
            this.name = createOperatorName();
            this.associativity = rules.get(0).associativity;
            this.priority = rules.get(0).priority;
        } else if (type == RuleType.RETURNER_AGGREGATOR) {
            if (rules.size() == 1) {
                Rule rule = rules.get(0);
                this.name = rule.name;
                this.cs = rule.cs;
                this.as = rule.as;
                rule.type = RuleType.PROCESSED;
            } else {
                this.name = createReturnerName();
            }
        }
    }

    public SuperRule(Rule declarator, List<Rule> usages) {
        this.rules = usages;
        this.name = declarator.name;
        this.type = declarator.type;
        this.parent = declarator.parent;
        declarator.type = RuleType.PROCESSED;
        setSuperRules(this.rules);
    }


    private String createOperatorName() {
        if (this.rules.size() > 1) {
            String name = String.join("Or", this.rules.stream()
                    .map(rule -> rule.originalName.lastIndexOf(".") != -1 ?
                            rule.originalName.substring(rule.originalName.lastIndexOf(".") + 1) :
                            rule.originalName).collect(toList()));
            return name.length() >= 21 ? name.substring(0, 20) : name;
        }else {
            return this.rules.get(0).name;
        }
    }

    private String createReturnerName() {
        String name = String.join("", this.rules.stream().map(
                rule -> rule.originalName.lastIndexOf(".") != -1 ?
                        rule.originalName.substring(rule.originalName.lastIndexOf(".") + 1) :
                        rule.originalName).collect(toList()));
        return name.length() >= 21 ? name.substring(0, 20) + "Primary" : name + "Primary";
    }


    public List<String> getAllDeclaratorRulesNames(){
        return rules.stream().filter(rul -> !this.name.equals(rul.name))
                .map(rl -> rl.name).collect(Collectors.toList());
    }

    public Rule getLastRuleItem(){
        return this.rules.get(this.rules.size() - 1);
    }

    private void setSuperRules(List<Rule> rules){
        rules.forEach(rule -> rule.setSuperRule(this));
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

}
