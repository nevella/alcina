package cc.alcina.framework.common.client.gwittir;

import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

public class ConverterValidator implements Validator {
	private final Converter converter;

	public ConverterValidator(Converter converter) {
		this.converter = converter;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		return converter.convert(value);
	}
}
