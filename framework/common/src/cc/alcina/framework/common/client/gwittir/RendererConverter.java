package cc.alcina.framework.common.client.gwittir;

import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.ui.Renderer;

public class RendererConverter<T, C> implements Converter<T, C> {
	private final Renderer<T, C> renderer;

	public RendererConverter(Renderer<T, C> renderer) {
		this.renderer = renderer;
	}

	public C convert(T original) {
		return renderer.render(original);
	}
}
