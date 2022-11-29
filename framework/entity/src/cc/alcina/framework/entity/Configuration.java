package cc.alcina.framework.entity;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

/*
 * Replacement for system configuration portion of ResourceUtilities
 *
 * TODO - clazz name -> property path segment[s] should change from
 * Class.simpleClassName to SeUtilities.getNestedSimpleName
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

	public static boolean has(Class clazz, String keyPart) {
		String key = clazz.getSimpleName() + "." + keyPart;
		return ResourceUtilities.isDefined(key);
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

	
	public static Key key(Class clazz, String keyPart) {
		return new Key(clazz, keyPart);
	}
	public static Key key( String keyPart) {
		return new Key(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), keyPart);
	}

	public static class Key {
		private Class clazz;
		private String keyPart;
		
		private boolean contextOverride;

		Key(Class clazz, String keyPart) {
			this.clazz = clazz;
			this.keyPart = keyPart;
		}
		public Key contextOverride(){
			contextOverride=true;
			return this;
		}
		
		public String get(){
			if(contextOverride){
				String key = Ax.format("%s.%s", clazz.getSimpleName(),keyPart);
				if(LooseContext.has(key)){
					return LooseContext.getString(key);
				}
			}
			return Configuration.get(clazz,keyPart);
		}
		public boolean is(){
			String value = get();
			return Boolean.valueOf(value);
		}
	}
}
