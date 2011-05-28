package cc.alcina.framework.gwt.client.gwittir.renderer;

import com.totsp.gwittir.client.ui.Renderer;

public class PassthroughWithNullRenderer implements
		Renderer<String, String> {
	private final String nullValue;

	public PassthroughWithNullRenderer(String nullValue) {
		this.nullValue = nullValue;
	}

	@Override
	public String render(String o) {
		return o == null ? nullValue : o;
	}
}