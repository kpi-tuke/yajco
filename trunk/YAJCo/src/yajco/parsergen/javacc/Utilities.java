package yajco.parsergen.javacc;

public final class Utilities {
    private Utilities() {
    }

    public static String test() {
        return "hallo";
    }

    public static String encodeStringIntoTokenName(String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            } else {
                sb.append("_" + ((int) c));
            }
        }

        return sb.toString().toUpperCase();
    }

    public static String encodeStringIntoRegex(String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (c == '[') {
                sb.append("\\[");
            } else if (c == '^') {
                sb.append("\\^");
            } else {
                sb.append("[" + c + "]");
            }
        }
        return sb.toString();
    }

    public static String encodeStringToJavaLiteral(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }
}
