package lh.demo.util;

/**
 * @author lh
 */
public class StringUtils {
    public static String  getLastDelimiterValue(String string,char delimiter){
        return  string.substring(string.lastIndexOf(delimiter) + 1);
    }

    public static String uppercaseFirstLetter(String name){
        return  name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
