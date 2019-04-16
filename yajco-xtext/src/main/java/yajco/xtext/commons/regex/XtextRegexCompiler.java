package yajco.xtext.commons.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class XtextRegexCompiler {

    private StringBuilder resultStringBuilder;
    private List<Tuple<Integer, Integer>> tupleList = new ArrayList<>();
    private Stack<Integer> indexStack = new Stack<>();
    private String regex;
    private boolean isInsideString;

    public String compileRegex(String regex) {
        this.tupleList = new ArrayList<>();
        resultStringBuilder = new StringBuilder();
        indexStack = new Stack<>();
        isInsideString = false;

        this.regex = regex;
        mapParentheses(regex);
        processRegex(0, regex.length());
        return resultStringBuilder.toString();
    }

    private void mapParentheses(String regex) {
        int index = 0;
        boolean block = false;
        for (int i = 0; i < regex.length(); i++) {
            char current = regex.charAt(i);
            if (current == '[') {
                block = true;
            } else if (block && current == ']') {
                block = false;
            }
            if (!block) {
                if (current == '(') {
                    if (i == 0) {
                        tupleList.add(new Tuple<>(i, null));
                        indexStack.push(index++);
                    } else if (regex.charAt(i - 1) != '\\') {
                        tupleList.add(new Tuple<>(i, null));
                        indexStack.push(index++);
                    }
                } else if (current == ')') {
                    if (i == 0) {
                        tupleList.get(indexStack.pop()).setY(i);
                    } else if (regex.charAt(i - 1) != '\\') {
                        tupleList.get(indexStack.pop()).setY(i);
                    }
                }
            }

        }
    }

    private void processRegex(int stringStart, int stringEnd) {
        Pattern regexPattern = compile("(?<parentheses>\\()|(?<brackets>\\[.*?\\][+*?]?)|(?<pipe>\\|)|(?<right>\\\\?\\))|(?<special>([+*?]|(\\.\\*)|(\\.\\*\\?)))|(?<others>(\\\\?.+?)(?<othersSpecial>([+*?]|(\\.\\*)|(\\.\\*\\?))?))");
        Matcher regexMatcher = regexPattern.matcher(regex);
        int start = stringStart;
        int end = 0;
        while (regexMatcher.find(start) && end <= stringEnd) {
            String parentheses = regexMatcher.group("parentheses"),
                    brackets = regexMatcher.group("brackets"),
                    others = regexMatcher.group("others"),
                    othersSpecial = regexMatcher.group("othersSpecial"),
                    pipe = regexMatcher.group("pipe"),
                    right = regexMatcher.group("right"),
                    special = regexMatcher.group("special");

            if (right != null) {
                end = processRightGroup(regexMatcher, right, end);
            }
            if (parentheses != null) {
                end = processParenthesesGroup(regexMatcher, end);
            }
            if (brackets != null) {
                end = processBracketsGroup(regexMatcher, brackets,end);
            }
            if (others != null) {
                end = processOthersGroup(regexMatcher, others, othersSpecial, end);
            }
            if (pipe != null) {
                end = processPipeGroup(regexMatcher,end);
            }
            if (special != null) {
                end = processSpecialGroup(regexMatcher, special, end);
            }
            start = end;
        }
    }

    private int processRightGroup(Matcher matcher, String right, int end) {
        if (right.charAt(0) != '\\') {
            if (isInsideString)
                resultStringBuilder.append("'");
            resultStringBuilder.append(")");
            isInsideString = false;
        } else {
            if (!isInsideString)
                resultStringBuilder.append("')");
            else
                resultStringBuilder.append(")");

            resultStringBuilder.append("'");
            isInsideString = false;
        }
        return matcher.end("right") > end ? matcher.end("right") : end;
    }

    private int processParenthesesGroup(Matcher matcher, int end) {
        int newEnd = tupleList.stream().filter(tuple -> tuple.getX() == matcher.start("parentheses"))
                .findFirst().map(tuple -> tuple.getY() + 1).orElse(end);
        int returnEnd = newEnd > end ? newEnd : end;
        if (isInsideString)
            resultStringBuilder.append("'");
        isInsideString = false;
        resultStringBuilder.append("(");
        processRegex(matcher.start("parentheses") + 1, returnEnd - 1);
        return returnEnd;
    }

    private int processBracketsGroup(Matcher matcher, String brackets, int end) {
        int returnEnd = matcher.end("brackets") > end ? matcher.end("brackets") : end;
        if (isInsideString)
            resultStringBuilder.append("'");
        isInsideString = false;
        processSquareBrackets(brackets);
        return returnEnd;
    }

    private int processOthersGroup(Matcher matcher, String others, String othersSpecial, int end) {
        if (!isInsideString) {
            resultStringBuilder.append("'");
        }
        isInsideString = true;
        int returnEnd = matcher.end("others") > end ? matcher.end("others") : end;
        if (!othersSpecial.isEmpty()) {
            resultStringBuilder.append(resultStringBuilder.toString().charAt(resultStringBuilder.length() - 1) == '\'' ? "" : "' '");
            processOthers(others.substring(0, others.length() - othersSpecial.length()), false);
            resultStringBuilder.append("'").append(othersSpecial).append(" ");
            isInsideString = false;
        } else {
            processOthers(others, false);
        }

        if (returnEnd == regex.length() && othersSpecial.isEmpty()) {
            resultStringBuilder.append("'");
            isInsideString = false;
        }
        return returnEnd;
    }

    private int processPipeGroup(Matcher matcher, int end){
        if (isInsideString)
            resultStringBuilder.append("'");
        isInsideString = false;
        resultStringBuilder.append("|");
        return matcher.end("pipe") > end ? matcher.end("pipe") : end;
    }

    private int processSpecialGroup(Matcher matcher, String special, int end){
        if (isInsideString)
            resultStringBuilder.append("'");
        isInsideString = false;
        resultStringBuilder.append(special).append(" ");
        return matcher.end("special") > end ? matcher.end("special") : end;
    }

    private void processSquareBrackets(String regex) {
        Pattern patternRanges = compile("(?<first>\\[)(?<ranges>.*?)(?<last>\\])(?<others>[+*?]?)");
        Matcher matcherRanges = patternRanges.matcher(regex);

        String finalString = regex;
        int start = 0;
        while (matcherRanges.find(start)) {
            if (start != 0) {
                resultStringBuilder.append(" | ");
            }

            finalString = finalString.replace(matcherRanges.group("first"), "");
            finalString = finalString.replace(matcherRanges.group("ranges"), "");
            finalString = finalString.replace(matcherRanges.group("last"), "");

            resultStringBuilder.append("(");
            if (matcherRanges.group("ranges").charAt(0) == '^') {
                resultStringBuilder.append("!");
                processRanges(matcherRanges.group("ranges").substring(1));
            } else {
                processRanges(matcherRanges.group("ranges"));
            }
            resultStringBuilder.append(")");

            if (matcherRanges.group("others") != null) {
                resultStringBuilder.append(matcherRanges.group("others")).append(" ");
                finalString = finalString.replace(matcherRanges.group("others"), "");
                start = matcherRanges.end("others");
            } else {
                start = matcherRanges.end("last");
            }

        }
        processOthers(finalString, true);
    }

    private void processRanges(String regex) {
        Pattern patternRange = compile("(?<range>\\p{Alnum}-\\p{Alnum})");
        Matcher matcherRange = patternRange.matcher(regex);
        String result = regex;
        boolean matchAny = false;
        int start = 0;
        while (matcherRange.find(start)) {
            matchAny = true;
            if (start != 0) {
                resultStringBuilder.append(" | ");
            }
            start = matcherRange.end("range");
            processRange(matcherRange.group("range"));
            result = result.replace(matcherRange.group("range"), "");
        }
        if (!result.isEmpty()) {
            if (matchAny)
                resultStringBuilder.append(" | ");
            processOthers(result, true);
        }
    }

    private void processRange(String regex) {
        Pattern patternChar = compile("(?<first>\\p{Alnum})(?<delimiter>-)(?<last>\\p{Alnum})");
        Matcher matcherChar = patternChar.matcher(regex);

        int nestedStart = 0;
        while (matcherChar.find(nestedStart)) {
            resultStringBuilder.append(" \'").append(matcherChar.group("first")).append("\' ");
            resultStringBuilder.append("..");
            resultStringBuilder.append(" \'").append(matcherChar.group("last")).append("\' ");
            nestedStart = matcherChar.end("last");
        }
    }

    private void processOthers(String regex, boolean separate) {
        String edited = escapeSpecialRegexChars(regex);
        Pattern pattern = compile("\\\\?.");
        Matcher matcher = pattern.matcher(edited);

        int start = 0;

        while (matcher.find(start)) {
            if (start != 0 && separate) {
                resultStringBuilder.append(" | ");
            }
            start = matcher.end();

            StringBuilder append = new StringBuilder();
            if (matcher.group().matches("\\\\\\p{Alpha}")) {
                if (matcher.group().equals("\\s")) {
                    append.append("\\t" + "\'");
                    append.append(" | ");
                    append.append("\'" + " " + "\'");
                    append.append(" | ");
                    append.append("\'" + "\\n" + "\'");
                    append.append(" | ");
                    append.append("\'" + "\\r");
                } else {
                    append.append(separate ? "\'" + matcher.group() + "\'" : matcher.group());
                }
            } else if (matcher.group().matches("\\\\\\p{Punct}")) {
                append.append(separate ? "\'" + matcher.group().replace("\\", "") + "\'" : matcher.group().replace("\\", ""));
            } else if (matcher.group().matches("[\\w ]")) {
                append.append(separate ? "\'" + matcher.group() + "\'" : matcher.group());
            } else {
                append.append(matcher.group());
            }
            resultStringBuilder.append(append.toString());
        }
    }

    private String escapeSpecialRegexChars(String regex) {
        Pattern pattern = compile("[\\p{Punct}&&[^\\\\]]");
        Matcher matcher = pattern.matcher(regex);
        while (matcher.find()) {
            regex = regex.replace(matcher.group(), "\\" + matcher.group());
        }
        return regex;
    }
}
