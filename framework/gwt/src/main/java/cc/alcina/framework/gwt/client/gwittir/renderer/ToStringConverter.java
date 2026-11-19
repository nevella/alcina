package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.function.Function;

public class ToStringConverter implements Function<Object, String> {
	@Override
	public String apply(Object o) {
		return o == null ? "null" : o.toString();
	}
}
