package cc.alcina.framework.common.client;

import javax.annotation.Nullable;

import cc.alcina.framework.common.client.util.Ax;

public final class Preconditions {
	public static void checkState(boolean expression) {
		if (!expression) {
			throw new IllegalStateException();
		}
	}

	public static void checkState(boolean expression, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalStateException(
					Ax.format(errorMessageTemplate, errorMessageArgs));
		}
	}

	public static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

	public static void checkArgument(boolean expression, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(
					Ax.format(errorMessageTemplate, errorMessageArgs));
		}
	}

	public static <T> T checkNotNull(T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	public static <T> T checkNotNull(T reference, @Nullable
	String errorMessageTemplate, @Nullable
	Object... errorMessageArgs) {
		if (reference == null) {
			throw new NullPointerException(
					Ax.format(errorMessageTemplate, errorMessageArgs));
		}
		return reference;
	}
}