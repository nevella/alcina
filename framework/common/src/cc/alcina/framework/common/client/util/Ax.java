package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class Ax {
    public static String blankTo(String string, String defaultValue) {
        return isBlank(string) ? defaultValue : string;
    }

    public static String blankTo(String string,
            Supplier<String> defaultValueSupplier) {
        return isBlank(string) ? defaultValueSupplier.get() : string;
    }

    public static <T> Optional<T> first(Collection<T> collection) {
        return collection.size() == 0 ? Optional.empty()
                : Optional.of(collection.iterator().next());
    }

    public static String format(String template, Object... args) {
        return CommonUtils.formatJ(template, args);
    }

    public static String friendly(Object o) {
        return CommonUtils.friendlyConstant(o);
    }

    public static boolean isBlank(String string) {
        return CommonUtils.isNullOrEmpty(string);
    }

    public static <T> T last(List<T> list) {
        return CommonUtils.last(list);
    }

    public static boolean notBlank(String string) {
        return !isBlank(string);
    }

    public static void out(String template, Object... args) {
        System.out.println(format(template, args));
    }
    public static void out(Object object){
        System.out.println(object);
    }
    public static void sysLogHigh(String template, Object... args) {
        System.out.println(CommonUtils.highlightForLog(template, args));
    }

    public static void newlineDump(Collection collection) {
        System.out.println(CommonUtils.joinWithNewlines(collection));
    }

    public static void err(String template, Object... args) {
        System.err.println(format(template, args));
    }

    public static String nullSafe(String string) {
        return string == null ? "" : string;
    }

    public static void runtimeException(String template, Object... args) {
        throw new RuntimeException(format(template, args));
    }
}
