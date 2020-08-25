package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

public class FormValidation {
	private Binding binding;

	private Consumer<Void> onValid;

	void validate(Consumer<Void> onValid, Binding binding) {
		this.onValid = onValid;
		this.binding = binding;
		validateAndCommit(null, null);
	}

	public boolean validateAndCommit(final Widget sender,
			final AsyncCallback<Void> serverValidationCallback) {
		try {
			if (!WidgetUtils.docHasFocus()) {
				GwittirUtils.commitAllTextBoxes(binding);
			}
			LooseContext
					.pushWithTrue(ContentViewFactory.CONTEXT_VALIDATING_BEAN);
			if (!validateBean()) {
				return false;
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
					if (v instanceof ServerValidator && "fixme".isEmpty()) {
						ServerValidator sv = (ServerValidator) v;
						if (sv.isValidating()) {
							final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
									"Checking values", null);
							new Timer() {
								@Override
								public void run() {
									crd.hide();
									if (serverValidationCallback == null) {
										if (sender != null) {
											DomEvent.fireNativeEvent(
													WidgetUtils
															.createZeroClick(),
													sender);
										} else {
											// FIXME - directedlayout.2 -
											// probably throw a dev
											// exception - something should
											// happen here (which means we
											// need alcina devex support)
										}
									} else {
										validateAndCommit(sender,
												serverValidationCallback);
									}
								}
							}.schedule(500);
							return false;
						}
					}
				}
				// if (PermissionsManager.get().isMemberOfGroup(
				// PermissionsManager.getAdministratorGroupName())
				// && sender != null) {
				// if (ClientBase.getGeneralProperties()
				// .isAllowAdminInvalidObjectWrite()
				// && !alwaysDisallowOkIfInvalid) {
				// Registry.impl(ClientNotifications.class).confirm(
				// "Administrative option: save the changed items "
				// + "on this form (even though some are invalid)?",
				// new OkCallback() {
				// @Override
				// public void ok() {
				// commitChanges(true);
				// }
				// });
				// return false;
				// }
				// }
				if (sender != null) {
					Registry.impl(ClientNotifications.class).showWarning(
							"Please correct the problems in the form");
				} else {
				}
				return false;
			} // not valid
			if (serverValidationCallback != null) {
				for (Validator v : validators) {
					if (v instanceof ServerValidator) {
						serverValidationCallback.onSuccess(null);
						return true;
					}
				}
			}
		} finally {
			LooseContext.pop();
		}
		onValid.accept(null);
		return true;
	}

	public boolean validateBean() {
		// if (beanValidator == null) {
		// return true;
		// }
		// try {
		// beanValidator.validate(bean);
		// return true;
		// } catch (ValidationException e) {
		// Registry.impl(ClientNotifications.class)
		// .showWarning(e.getMessage());
		// return false;
		// }
		return true;
	}

	public boolean validateFields() {
		return binding.validate();
	}
}
