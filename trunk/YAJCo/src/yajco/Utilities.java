package yajco;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utilities {
    public static <T> List<T> asList(T[] a) {
        if (a == null) {
            return new ArrayList<T>();
        }
        return new ArrayList<T>(Arrays.asList(a));
    }
}
