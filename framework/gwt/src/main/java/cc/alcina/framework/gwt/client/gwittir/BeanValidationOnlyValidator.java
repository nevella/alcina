package cc.alcina.framework.gwt.client.gwittir;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.ValidationException;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.Validator;

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
