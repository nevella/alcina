package cc.alcina.framework.gwt.client.gwittir;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;

public class BeanValidationOnlyValidator implements Validator {
	private Validator wrappee;

	public BeanValidationOnlyValidator(Validator wrappee) {
		this.wrappee = wrappee;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (!LooseContext.is(ContentViewFactory.CONTEXT_VALIDATING_BEAN)) {
			return value;
		}
		return wrappee.validate(value);
	}
}
