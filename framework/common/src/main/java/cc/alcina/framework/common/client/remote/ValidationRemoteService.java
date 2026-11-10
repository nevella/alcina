package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RemoteService;

import cc.alcina.framework.common.client.gwittir.validator.AsyncValidator;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;

public interface ValidationRemoteService extends RemoteService {
	@WebMethod
	public ValidationResult validateAsync(
			Class<? extends AsyncValidator> validatorClass, String value);
}
