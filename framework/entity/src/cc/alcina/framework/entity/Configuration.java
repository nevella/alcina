package cc.alcina.framework.entity;

/*
 * Replacement for system configuration portion of ResourceUtilities
 * 
 * TODO - clazz name -> property path segment[s] should change from Class.simpleClassName to SeUtilities.getNestedSimpleName
 * 
 * ... with a regression test
 */
public class Configuration {
	public static String get(Class clazz, String key) {
		return ResourceUtilities.getBundledString(clazz, key);
	}

	public static String get(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return value;
	}

	public static int getInt(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return Integer.parseInt(value);
	}

	public static boolean is(Class clazz, String key) {
		String value = get(clazz, key);
		return Boolean.valueOf(value);
	}

	public static boolean is(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return Boolean.valueOf(value);
	}
}
