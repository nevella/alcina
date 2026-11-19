package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.function.Function;

public class ToLowerCaseTrimmedConverter implements Function<Object, String> {
	@Override
	public String apply(Object original) {
		return original == null ? null
				: original.toString().toLowerCase().trim();
	}
}
