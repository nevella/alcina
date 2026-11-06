package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.function.Function;

public class PassthroughWithNullRenderer implements Function<String, String> {
	private final String nullValue;

	public PassthroughWithNullRenderer(String nullValue) {
		this.nullValue = nullValue;
	}

	@Override
	public String apply(String o) {
		return o == null ? nullValue : o;
	}
}