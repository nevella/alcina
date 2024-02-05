package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

class FormValidation {
	static final String DEFAULT_BEAN_EXCEPTION_MESSAGE = "Please correct the problems in the form";

	Binding binding;

	Topic<State> topicState = Topic.create();

	String beanValidationExceptionMessage;

	Bindable bindable;

	void validate(TopicListener<FormValidation.State> stateListener,
			Binding binding, Bindable bindable) {
		this.bindable = bindable;
		topicState.add(stateListener);
		this.binding = binding;
		validateAndCommit();
	}

	void validateAndCommit() {
		topicState.publish(State.VALIDATING);
		try {
			if (!WidgetUtils.docHasFocus()) {
				GwittirUtils.commitAllTextBoxes(binding);
			}
			LooseContext
					.pushWithTrue(ContentViewFactory.CONTEXT_VALIDATING_BEAN);
			Validator beanValidator = BeanFields.query().forBean(bindable)
					.getValidator();
			if (beanValidator != null) {
				try {
					beanValidator.validate(bindable);
				} catch (ValidationException e) {
					beanValidationExceptionMessage = e.getMessage();
					topicState.publish(State.INVALID);
					return;
				}
			}
			ServerValidator.performingBeanValidation = true;
			boolean bindingValid = false;
			try {
				bindingValid = binding.validate();
			} finally {
				ServerValidator.performingBeanValidation = false;
			}
			List<Validator> validators = GwittirUtils.getAllValidators(binding,
					null);
			if (!bindingValid) {
				for (Validator v : validators) {
					if (v instanceof ServerValidator) {
						if (!"fixme".isEmpty()) {
							throw new UnsupportedOperationException();
						}
						topicState.publish(State.ASYNC_VALIDATING);
						ServerValidator sv = (ServerValidator) v;
						if (sv.isValidating()) {
							// final CancellableRemoteDialog crd = new
							// NonCancellableRemoteDialog(
							// "Checking values", null);
							// new Timer() {
							// @Override
							// public void run() {
							// crd.hide();
							// if (serverValidationCallback == null) {
							// if (sender != null) {
							// DomEvent.fireNativeEvent(
							// WidgetUtils
							// .createZeroClick(),
							// sender);
							// } else {
							// // FIXME - dirndl 1x2 -
							// // probably throw a dev
							// // exception - something should
							// // happen here (which means we
							// // need alcina devex support)
							// }
							// } else {
							// validateAndCommit(sender,
							// serverValidationCallback);
							// }
							// }
							// }.schedule(500);
							// return false;
						}
					}
				}
				beanValidationExceptionMessage = DEFAULT_BEAN_EXCEPTION_MESSAGE;
				topicState.publish(State.INVALID);
			} else {
				// if (serverValidationCallback != null) {
				// for (Validator v : validators) {
				// if (v instanceof ServerValidator) {
				// serverValidationCallback.onSuccess(null);
				// return true;
				// }
				// }
				// }
				topicState.publish(State.VALID);
			}
		} finally {
			LooseContext.pop();
		}
	}

	enum State {
		VALIDATING, ASYNC_VALIDATING, VALID, INVALID;

		boolean isComplete() {
			switch (this) {
			case VALIDATING:
			case ASYNC_VALIDATING:
				return false;
			default:
				return true;
			}
		}

		boolean isValid() {
			return this == VALID;
		}
	}
}
