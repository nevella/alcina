package cc.alcina.framework.servlet.servlet.handler;

import cc.alcina.framework.common.client.gwittir.validator.AsyncValidator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.ReflectiveRemoteServiceHandler;
import cc.alcina.framework.common.client.remote.ReflectiveValidationRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.ValidationRemoteService;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;
import cc.alcina.framework.servlet.servlet.AsyncValidatorHandler;

@Registration({ ReflectiveRemoteServiceHandler.class,
		ReflectiveValidationRemoteServiceAsync.class })
public class ReflectiveValidationRemoteServiceAsyncHandler
		implements ReflectiveRemoteServiceHandler, ValidationRemoteService {
	@Override
	public ValidationResult validateAsync(
			Class<? extends AsyncValidator> validatorClass, String value) {
		try {
			/*
			 * use the result as a passthrough token
			 */
			return Registry.impl(AsyncValidatorHandler.class, validatorClass)
					.validate(value);
		} catch (Exception e) {
			return ValidationResult.invalid(e);
		}
	}
}
