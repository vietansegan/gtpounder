package util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 *
 * @author vietan
 */
public class MiscUtils {

    protected static final NumberFormat formatter = new DecimalFormat("###.###");

    public static String listToString(List<Double> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder str = new StringBuilder();
        str.append("[").append(formatDouble(list.get(0)));
        for (int i = 1; i < list.size(); i++) {
            str.append(", ").append(formatDouble(list.get(i)));
        }
        str.append("]");
        return str.toString();
    }

    public static String arrayToString(double[] array) {
        StringBuilder str = new StringBuilder();
        str.append("[").append(formatDouble(array[0]));
        for (int i = 1; i < array.length; i++) {
            str.append(", ").append(formatDouble(array[i]));
        }
        str.append("]");
        return str.toString();
    }

    public static String arrayToString(int[] array) {
        StringBuilder str = new StringBuilder();
        str.append("[").append(formatDouble(array[0]));
        for (int i = 1; i < array.length; i++) {
            str.append(", ").append(formatDouble(array[i]));
        }
        str.append("]");
        return str.toString();
    }

    public static String formatDouble(double value) {
        return formatter.format(value);
    }
}
