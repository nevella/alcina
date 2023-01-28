package cc.alcina.framework.entity;

import java.util.Optional;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.projection.GraphProjection;

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

	public static Key key(String keyPart) {
		return new Key(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), keyPart);
	}

	/*
	 * A utility mostly intended for 'context beats configuration'
	 */
	public static class Key {
		private Class clazz;

		private String keyPart;

		private boolean contextOverride = true;

		Key(Class clazz, String keyPart) {
			this.clazz = clazz;
			this.keyPart = keyPart;
		}

		public boolean definedAndIs() {
			return has() && is();
		}

		public String get() {
			if (contextOverride) {
				String key = Ax.format("%s.%s", clazz.getSimpleName(), keyPart);
				if (LooseContext.has(key)) {
					return LooseContext.getString(key);
				}
			}
			return Configuration.get(clazz, keyPart);
		}

		public boolean has() {
			if (contextOverride) {
				String key = Ax.format("%s.%s", clazz.getSimpleName(), keyPart);
				if (LooseContext.has(key)) {
					return true;
				}
			}
			return ResourceUtilities.isDefined(Ax.format("%s.%s",
					GraphProjection.classSimpleName(clazz), keyPart));
		}

		public int intValue() {
			String value = get();
			return Integer.valueOf(value);
		}

		public boolean is() {
			String value = get();
			return Boolean.valueOf(value);
		}

		public long longValue() {
			String value = get();
			return Long.valueOf(value);
		}

		public Optional<Key> optional() {
			return has() ? Optional.of(this) : Optional.empty();
		}

		public void set(String value) {
			String key = Ax.format("%s.%s", clazz.getSimpleName(), keyPart);
			LooseContext.set(key, value);
		}

		public Key withContextOverride(boolean contextOverride) {
			this.contextOverride = contextOverride;
			return this;
		}
	}
}
