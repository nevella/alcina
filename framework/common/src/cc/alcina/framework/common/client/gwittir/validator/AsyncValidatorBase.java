package cc.alcina.framework.common.client.gwittir.validator;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.remote.ReflectiveValidationRemoteServiceAsync;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;
import cc.alcina.framework.gwt.client.util.Async;

/*
 * Async validators request validation on the server (for a given
 * string/Validator type), and reuse validation results for completed
 * validations of identical values
 */
@Bean(PropertySource.FIELDS)
public abstract class AsyncValidatorBase implements AsyncValidator {
	ValidationResult validationResult = new ValidationResult(
			ValidationState.NOT_VALIDATED);

	Object validating;

	Object validated;

	private Runnable postAsyncValidation;

	private AsyncCallback callback;

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		Preconditions.checkArgument(value == null || value instanceof String);
		/*
		 * this should only be called post-validation
		 */
		switch (validationResult.state) {
		case VALID:
			return value;
		case INVALID:
			throw new ValidationException(validationResult.exceptionMessage);
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean validateAsync(Object value, Runnable postAsyncValidation) {
		this.postAsyncValidation = postAsyncValidation;
		if (this.validationResult.state == ValidationState.ASYNC_VALIDATING) {
			if (Objects.equals(validating, value)) {
				// don't cancel the existing validation
				return true;
			}
		}
		if (this.validationResult.isValidationComplete()
				&& Objects.equals(validated, value)) {
			return false;
		}
		validationResult = new ValidationResult(
				ValidationState.ASYNC_VALIDATING);
		this.validating = value;
		this.callback = Async.<ValidationResult> callbackBuilder()
				.success(this::onServerValidationCallbackSuccess)
				.failure(this::onServerValidationFailure)
				.withCancelInflight(this.callback).build();
		ReflectiveValidationRemoteServiceAsync.get().validateAsync(getClass(),
				(String) value, callback);
		return true;
	}

	void onServerValidationComplete() {
		validated = validating;
		validating = null;
		postAsyncValidation.run();
	}

	void onServerValidationFailure(Throwable throwable) {
		throwable.printStackTrace();
		validationResult = ValidationResult.invalid(throwable);
		onServerValidationComplete();
	}

	void onServerValidationCallbackSuccess(ValidationResult validationResult) {
		this.validationResult = validationResult;
		onServerValidationComplete();
	}
}
