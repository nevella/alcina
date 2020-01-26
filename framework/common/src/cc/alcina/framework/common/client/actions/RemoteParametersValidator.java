package cc.alcina.framework.common.client.actions;

import com.totsp.gwittir.client.validator.ValidationException;

public interface RemoteParametersValidator<RP extends RemoteParameters> {
	public void validate(RP params) throws ValidationException;
}
