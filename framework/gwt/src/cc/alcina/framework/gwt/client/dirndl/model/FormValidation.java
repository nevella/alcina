package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.Validation;
import cc.alcina.framework.common.client.gwittir.validator.ValidationState;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.model.FormEvents.ValidationResult;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.Feedback;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormElement;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class FormValidation {
	static final String DEFAULT_BEAN_EXCEPTION_MESSAGE = "Please correct the problems in the form";

	Topic<ValidationResult> topicValidationResult = Topic.create();

	FormModel formModel;

	List<Validation> validations;

	FormValidation(FormModel formModel) {
		this.formModel = formModel;
	}

	class RemovableListener implements TopicListener<ValidationResult> {
		TopicListener<ValidationResult> listener;

		ModelEvent originatingModelEvent;

		RemovableListener(TopicListener<ValidationResult> listener,
				ModelEvent originatingModelEvent) {
			this.listener = listener;
			this.originatingModelEvent = originatingModelEvent;
		}

		@Override
		public void topicPublished(ValidationResult result) {
			if (result.isValidationComplete()) {
				topicValidationResult.remove(this);
			}
			result = result.copy();
			result.originatingEvent = originatingModelEvent;
			listener.topicPublished(result);
		}
	}

	void validate(TopicListener<ValidationResult> validationResultListener,
			ModelEvent originatingEvent) {
		topicValidationResult.add(new RemovableListener(
				validationResultListener, originatingEvent));
		ensureObservers();
		commitInputsAndValidate();
	}

	void ensureObservers() {
		if (validations == null) {
			validations = new ArrayList<>();
			formModel.elements.stream().map(FormElement::getValidation)
					.filter(Objects::nonNull).forEach(validations::add);
			Validator beanValidator = BeanFields.query()
					.forBean(formModel.getState().getModel()).getValidator();
			if (beanValidator != null) {
				validations.add(new BeanValidation());
			}
			validations.forEach(
					v -> v.topicStateChange.add(this::checkValidationComplete));
		}
	}

	void checkValidationComplete() {
		if (validations.stream().allMatch(Validation::isComplete)
				|| validations.stream().anyMatch(Validation::isInvalid)) {
			if (validations.stream().allMatch(Validation::isValid)) {
				topicValidationResult
						.publish(new ValidationResult(ValidationState.VALID));
			} else {
				String beanValidationExceptionMessage = validations.stream()
						.map(Validation::getBeanValidationExceptionMessage)
						.filter(Objects::nonNull).findFirst()
						.orElse(DEFAULT_BEAN_EXCEPTION_MESSAGE);
				topicValidationResult.publish(ValidationResult
						.invalid(beanValidationExceptionMessage));
			}
		}
	}

	class BeanValidation extends Validation {
		Validator validator;

		Feedback feedback;

		BeanValidation() {
			this.feedback = new FormModel.Feedback();
			this.validator = BeanFields.query()
					.forBean(formModel.getState().getModel()).getValidator();
		}

		@Override
		public Validator getValidator() {
			return validator;
		}

		@Override
		public Object getValidationInput() {
			return formModel.getState().getModel();
		}

		@Override
		public Binding getBinding() {
			return null;
		}

		@Override
		public String getMessage() {
			return feedback.getMessage();
		}
	}

	void commitInputsAndValidate() {
		topicValidationResult
				.publish(new ValidationResult(ValidationState.VALIDATING));
		try {
			if (!WidgetUtils.docHasFocus()) {
				GwittirUtils
						.commitAllTextBoxes(formModel.getState().formBinding);
			}
			/*
			 * This is...OK, having a ValidationContext would be nicer, but
			 * would require backwards compatibility
			 */
			LooseContext
					.pushWithTrue(ContentViewFactory.CONTEXT_VALIDATING_BEAN);
			validations.forEach(Validation::validate);
		} finally {
			LooseContext.pop();
		}
		checkValidationComplete();
	}
	// public interface Validation2 {
	// String getBeanValidationExceptionMessage();
	// Validator getValidator();
	// void setValidationState(ValidationState state);
	// default void validate(ValidationTask task) {
	// Validator validator = getValidator();
	// if (validator instanceof AsyncValidator) {
	// /*
	// * required for dirndl async validation
	// */
	// Object validationInput = getValidationInput();
	// Preconditions
	// .checkState(validator instanceof AsyncValidatorBase);
	// setValidationState(ValidationState.VALIDATING);
	// AsyncValidatorBase asyncValidator = (AsyncValidatorBase) validator;
	// AsyncCallback<ServerValidationResult> callback = Async
	// .<ServerValidationResult> callbackBuilder()
	// .success(result -> onServerValidationCallbackSuccess(
	// task, result))
	// .failure(throwable -> onServerValidationFailure(task,
	// throwable))
	// .build();
	// asyncValidator.validateAsync(validationInput, callback);
	// } else {
	// try {
	// validateSync();
	// task.onValidationSuccess();
	// } catch (Exception e) {
	// task.onValidationFailure(e);
	// }
	// }
	// }
	// void validateSync();
	// default void onServerValidationCallbackSuccess(ValidationTask task,
	// ServerValidationResult result) {
	// AsyncValidatorBase asyncValidator = (AsyncValidatorBase) getValidator();
	// asyncValidator.setState(ValidationState.VALID);
	// validateSync();
	// asyncValidator.setState(ValidationState.NOT_VALIDATED);
	// task.onValidationSuccess();
	// setValidationState(ValidationState.VALID);
	// }
	// default void onServerValidationFailure(ValidationTask task,
	// Throwable throwable) {
	// setValidationState(ValidationState.INVALID);
	// }
	// Object getValidationInput();
	// public interface Has {
	// Validation getValidation();
	// }
	// }
}
