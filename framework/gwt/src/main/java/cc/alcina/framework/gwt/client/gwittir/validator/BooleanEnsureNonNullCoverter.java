package cc.alcina.framework.gwt.client.gwittir.validator;

import java.util.Date;

import java.util.function.Function;

public class BooleanEnsureNonNullCoverter
		implements Function<Boolean, Boolean> {
	public static final BooleanEnsureNonNullCoverter INSTANCE = new BooleanEnsureNonNullCoverter();

	public Boolean apply(Boolean original) {
		return original == null ? Boolean.FALSE : original;
	}

	public String apply(Date original) {
		return original == null ? null : String.valueOf(original.getTime());
	}
}
