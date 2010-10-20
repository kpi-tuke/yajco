package yajco.printer;

import nl.flotsam.xeger.Xeger;

public class PrinterHelper {

    //TODO: experimental implementation, see limitations: http://code.google.com/p/xeger/
    public static String generateExpressionFromRegex(String regex) {
        Xeger xeger = new Xeger(regex);
        return xeger.generate();
    }

}
