package cc.alcina.framework.common.client.gwittir;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

public class RendererValidator implements Validator {
	private final Renderer renderer;

	public RendererValidator(Renderer renderer) {
		this.renderer = renderer;
	}


	public Object validate(Object value) throws ValidationException {
		return renderer.render(value);
	}
}
