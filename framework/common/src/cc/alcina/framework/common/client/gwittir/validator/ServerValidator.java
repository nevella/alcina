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

import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.validator.ValidationException;

/**
 *
 * @author Nick Reddel
 */

 public class ServerValidator implements ParameterisedValidator, Serializable {
	private String message;

	private transient boolean validating;

	private transient boolean validated;

	private transient Object lastValidated = null;

	private transient Object validateAfterServerReturns = null;

	private transient boolean ignoreValidation = false;
	
	public static boolean performingBeanValidation = false;

	public boolean isValidating() {
		return this.validating;
	}

	public boolean isValidated() {
		return this.validated;
	}

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
				return value;
			}
		}
		if ((value == null && lastValidated == null)
				|| (value != null && value.equals(lastValidated))) {
			if (getMessage() != null) {
				throw new ValidationException(getMessage(), getClass());
			} else {
				return value;
			}
		}
		if (GWT.isClient() && !validating&&!performingBeanValidation) {
			validateAfterServerReturns = null;
			validating = true;
			validated = false;
			setMessage(null);
			final ProcessingServerValidationException psve = new ProcessingServerValidationException(
					getValidatingMessage(), null);
			AsyncCallback<List<ServerValidator>> callback = new AsyncCallback<List<ServerValidator>>() {
				public void onFailure(Throwable caught) {
					ClientLayerLocator.get().clientBase().showError(
							"Validator error", caught);
					setMessage("Validator error");
					resolveFeedback();
					cleanUp();
				}

				public void onSuccess(List<ServerValidator> result) {
					setMessage(null);
					for (ServerValidator sv : result) {
						if (sv.getMessage() != null) {
							setMessage(sv.getMessage());
						}
					}
					resolveFeedback();
					cleanUp();
				}
				@SuppressWarnings("unchecked")
				void resolveFeedback() {
					psve.feedback.resolve(psve.sourceWidget);
					if (getMessage() != null) {
						psve.feedback.handleException(psve.getSourceWidget(),
								new ValidationException(getMessage(),
										ServerValidator.this.getClass()));
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
				@SuppressWarnings("unchecked")
				void cleanUp() {
					validating = false;
					validated = true;
					lastValidated = value;
					psve.setFeedback(null);
					if (validateAfterServerReturns != null) {
						AbstractBoundWidget bw = (AbstractBoundWidget) psve.sourceWidget;
						bw.setValue(validateAfterServerReturns);
					}
					psve.setSourceWidget(null);
				}
			};
			ClientLayerLocator.get().commonRemoteServiceAsync().validateOnServer(
					Arrays.asList(new ServerValidator[] { this }), callback);
			throw psve;
		}
		return value;
	}

	public String getValidatingMessage() {
		return " validating";
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void reset() {
		message = null;
	}

	public static boolean listIsValid(List<ServerValidator> svs) {
		for (ServerValidator sv : svs) {
			if (sv.message != null) {
				return false;
			}
		}
		return true;
	}

	public void setParameters(NamedParameter[] params) {
	}

	public static class ProcessingServerValidationException extends
			ValidationException {
		public ProcessingServerValidationException(String message,
				Class validatorClass) {
			super(message, validatorClass);
		}

		private Object sourceWidget;

		private RelativePopupValidationFeedback feedback;

		public RelativePopupValidationFeedback getFeedback() {
			return this.feedback;
		}

		public void setFeedback(RelativePopupValidationFeedback feedback) {
			this.feedback = feedback;
		}

		public void setSourceWidget(Object sourceWidget) {
			this.sourceWidget = sourceWidget;
		}

		public Object getSourceWidget() {
			return sourceWidget;
		}
	}
}