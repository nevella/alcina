/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.gwittir.validator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

/**
 *
 * @author Nick Reddel
 */
public class ServerValidator extends Model
		implements ParameterisedValidator, Serializable {
	public static boolean performingBeanValidation = false;

	public static final Topic<ServerValidator> topicBeforeServerValidationSend = Topic
			.create();

	public static final Topic<ServerValidator> topicServerValidationResult = Topic
			.create();

	public static boolean listIsValid(List<ServerValidator> svs) {
		for (ServerValidator sv : svs) {
			if (sv.message != null) {
				return false;
			}
		}
		return true;
	}

	private String message;

	private transient boolean validating;

	private transient boolean validated;

	protected transient Object lastValidated = null;

	private transient Object validateAfterServerReturns = null;

	private transient boolean ignoreValidation = false;

	private ServerValidationResult serverValidationResult;

	public String getMessage() {
		return message;
	}

	public ServerValidationResult getServerValidationResult() {
		return this.serverValidationResult;
	}

	@AlcinaTransient
	public String getValidatingMessage() {
		return " validating";
	}

	protected void handleServerValidationException(ServerValidator sv) {
		setMessage(sv.getMessage());
	}

	@AlcinaTransient
	public boolean isValidated() {
		return this.validated;
	}

	@AlcinaTransient
	public boolean isValidating() {
		return this.validating;
	}

	protected ValidationException provideTypedException(String sMessage) {
		return new ValidationException(sMessage, getClass());
	}

	public void reset() {
		message = null;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public void setParameters(NamedParameter[] params) {
	}

	public void setServerValidationResult(
			ServerValidationResult serverValidationResult) {
		this.serverValidationResult = serverValidationResult;
	}

	@Override
	public Object validate(final Object value) throws ValidationException {
		if (ignoreValidation) {
			return value;
		}
		final String valueForPropChange = getClass().getName() + "-validating";
		if (valueForPropChange.equals(value)) {
			throw new ValidationException("Validating", null);
		}
		if (GWT.isClient()) {
			if (validating) {
				validateAfterServerReturns = value;
				final ProcessingServerValidationException psve = new ProcessingServerValidationException(
						getValidatingMessage(), null);
				throw (psve);
			}
		}
		if ((value == null && lastValidated == null)
				|| (value != null && value.equals(lastValidated))) {
			if (getMessage() != null) {
				throw provideTypedException(getMessage());
			} else {
				return value;
			}
		}
		if (GWT.isClient() && !validating && !performingBeanValidation) {
			validateAfterServerReturns = null;
			validating = true;
			validated = false;
			setMessage(null);
			final ProcessingServerValidationException psve = new ProcessingServerValidationException(
					getValidatingMessage(), null);
			AsyncCallback<List<ServerValidator>> callback = new AsyncCallback<List<ServerValidator>>() {
				void cleanUp() {
					validating = false;
					validated = true;
					lastValidated = value;
					psve.setFeedback(null);
					if (validateAfterServerReturns != null) {
						AbstractBoundWidget bw = (AbstractBoundWidget) psve.sourceWidget;
						if (bw != null) {
							bw.setValue(validateAfterServerReturns);
						}
					}
					psve.setSourceWidget(null);
				}

				@Override
				public void onFailure(Throwable caught) {
					setMessage("Validator error");
					resolveFeedback(null);
					cleanUp();
					throw new WrappedRuntimeException("Validator error",
							caught);
				}

				@Override
				public void onSuccess(List<ServerValidator> result) {
					setMessage(null);
					ServerValidator lastValidatorWithException = null;
					for (ServerValidator sv : result) {
						if (sv.getMessage() != null) {
							lastValidatorWithException = sv;
							handleServerValidationException(sv);
						}
						topicServerValidationResult.publish(sv);
					}
					resolveFeedback(lastValidatorWithException);
					cleanUp();
				}

				void resolveFeedback(
						ServerValidator lastValidatorWithException) {
					if (psve.feedback == null) {
						return;
					}
					psve.feedback.resolve(psve.sourceWidget);
					if (getMessage() != null) {
						ValidationException validationException = null;
						if (lastValidatorWithException == null) {
							validationException = new ValidationException(
									getMessage(),
									ServerValidator.this.getClass());
						} else {
							validationException = lastValidatorWithException
									.provideTypedException(getMessage());
						}
						psve.feedback.handleException(psve.getSourceWidget(),
								validationException);
					} else {
						AbstractBoundWidget bw = (AbstractBoundWidget) psve.sourceWidget;
						// jiggery-pokery to tell the binding the value's ok
						// ignore validation because otherwise we'll overwrite
						// the last "real" validation request
						ignoreValidation = true;
						try {
							bw.setValue(valueForPropChange);
							bw.setValue(value);
						} finally {
							ignoreValidation = false;
						}
					}
				}
			};
			topicBeforeServerValidationSend.publish(this);
			validateWithCallback(callback);
			throw psve;
		}
		return value;
	}

	protected void validateWithCallback(
			AsyncCallback<List<ServerValidator>> callback) {
		Client.commonRemoteService()
				.validateOnServer(Arrays.stream(new ServerValidator[] { this })
						.collect(Collectors.toList()), callback);
	}

	public static class ProcessingServerValidationException
			extends ValidationException {
		private Object sourceWidget;

		private RelativePopupValidationFeedback feedback;

		public ProcessingServerValidationException(String message,
				Class validatorClass) {
			super(message, validatorClass);
		}

		public RelativePopupValidationFeedback getFeedback() {
			return this.feedback;
		}

		public Object getSourceWidget() {
			return sourceWidget;
		}

		public void setFeedback(RelativePopupValidationFeedback feedback) {
			this.feedback = feedback;
		}

		public void setSourceWidget(Object sourceWidget) {
			this.sourceWidget = sourceWidget;
		}
	}
}