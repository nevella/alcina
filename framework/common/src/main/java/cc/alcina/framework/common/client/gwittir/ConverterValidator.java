package cc.alcina.framework.common.client.gwittir;

import java.util.function.Function;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.ValidationException;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.validator.Validator;

public class ConverterValidator implements Validator {
	private final Function converter;

	public ConverterValidator(Function converter) {
		this.converter = converter;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		return converter.apply(value);
	}
}
