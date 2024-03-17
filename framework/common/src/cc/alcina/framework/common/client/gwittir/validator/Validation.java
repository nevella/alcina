package cc.alcina.framework.common.client.gwittir.validator;

import java.util.Objects;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;

/**
 * This class manages the validation process (property or bean)
 * 
 */
public abstract class Validation {
	public Topic<Validation> topicStateChange = Topic.create();

	ValidationResult result = new ValidationResult(ValidationState.VALIDATING);

	public ValidationResult getResult() {
		return result;
	}

	public void setResult(ValidationResult result) {
		ValidationResult old_result = this.result;
		this.result = result;
		if (!Objects.equals(this.result, old_result)) {
			topicStateChange.publish(this);
		}
	}

	public void validate() {
		if (getBinding() != null) {
			getBinding().setRight();
		} else {
			try {
				setResult(new ValidationResult(ValidationState.VALIDATING));
				getValidator().validate(getValidationInput());
				setResult(new ValidationResult(ValidationState.VALID));
			} catch (ValidationException e) {
				setResult(ValidationResult.invalid(e.getMessage()));
			}
		}
	}

	public boolean isComplete() {
		return result.isValidationComplete();
	}

	public abstract String getMessage();

	public String getBeanValidationExceptionMessage() {
		return isBean() ? getMessage() : null;
	}

	public boolean isBean() {
		return getBinding() == null;
	}

	public abstract Binding getBinding();

	public boolean isInvalid() {
		/*
		 * not the same - intermediate (validating etc) are neither valid nor
		 * invalid
		 */
		// !isValid();
		return result.state.isInvalid();
	}

	public boolean isValid() {
		return result.state.isValid();
	}

	public Validator getValidator() {
		throw new UnsupportedOperationException();
	}

	public Object getValidationInput() {
		throw new UnsupportedOperationException();
	}

	public abstract static class PropertyObserver {
	}

	public abstract static class BeanObserver {
	}

	public interface Has {
		Validation getValidation();
	}
}
