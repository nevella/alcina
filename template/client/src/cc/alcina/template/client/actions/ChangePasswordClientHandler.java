package cc.alcina.template.client.actions;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CancelAction;
import cc.alcina.framework.common.client.actions.instances.ChangePasswordClientAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.gwittir.validator.CallbackValidator;
import cc.alcina.framework.common.client.gwittir.validator.CallbackValidator.ValidationCallback;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;
import cc.alcina.template.cs.actions.ChangePasswordServerAction;
import cc.alcina.template.cs.csobjects.ChangePasswordModel;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.Validator;

/**
 * 
 * @author nreddel@barnet.com.au
 * 
 */
@RegistryLocation(j2seOnly = false, registryPoint = PermissibleActionHandler.class, targetClass = ChangePasswordClientAction.class)
@ClientInstantiable
public class ChangePasswordClientHandler implements PermissibleActionHandler {
	private GlassDialogBox gdb;

	public void handleAction(PermissibleAction action, Object target) {
		IUser user = (IUser) target;
		if (user.getId() == 0) {
			ClientLayerLocator.get().notifications().showWarning(
					"This user has not been saved to the database");
			return;
		}
		gdb = new GlassDialogBox();
		createChangePasswordForm(user);
		gdb.setText("Change password");
		gdb.add(changePasswordHolder);
		gdb.center();
		gdb.show();
	}

	private FlowPanel changePasswordHolder;

	private ChangePasswordModel pcm;

	private PaneWrapperWithObjects changePasswordView;

	private void createChangePasswordForm(IUser target) {
		changePasswordHolder = new FlowPanel();
		changePasswordHolder.add(new Label("Please enter the new password"));
		ContentViewFactory cvf = new ContentViewFactory();
		pcm = new ChangePasswordModel();
		PermissibleActionListener vl = new PermissibleActionListener() {
			public void vetoableAction(PermissibleActionEvent evt) {
				if (evt.getAction() instanceof ViewAction) {
					ChangePasswordServerAction cp = new ChangePasswordServerAction();
					cp.setParameters(pcm);
					AsyncCallback<ActionLogItem> callback = new AsyncCallback<ActionLogItem>() {
						public void onFailure(Throwable caught) {
							ClientLayerLocator.get().notifications().showError(caught);
							gdb.hide();
						}

						public void onSuccess(ActionLogItem item) {
							ClientLayerLocator.get().notifications().showMessage(
									"Password changed");
							gdb.hide();
						}
					};
					changePasswordView.setVisible(false);
					ClientLayerLocator.get().commonRemoteServiceAsync()
							.performActionAndWait(cp, callback);
				}
				if (evt.getAction() instanceof CancelAction) {
					gdb.hide();
				}
			}
		};
		cvf.setNoCaption(true);
		cvf.setNoButtons(false);
		cvf.setCancelButton(true);
		pcm.setUserId(target.getId());
		changePasswordView = cvf.createBeanView(pcm, true, vl, false, true);
		List<Binding> bindings = changePasswordView.getBoundWidget()
				.getBinding().getChildren();
		for (Binding b : bindings) {
			if (b.getRight().property.getName().equals("newPassword2")) {
				ValidationCallback passwordCallback = new ValidationCallback() {
					public String validate(Object value) {
						if (pcm.getNewPassword() == null
								|| pcm.getNewPassword().equals(value)) {
							return null;
						}
						return "The passwords are not the same";
					}
				};
				Validator password2Validator = new CallbackValidator(
						passwordCallback);
				b.getLeft().validator = password2Validator;
			}
			RelativePopupValidationFeedback feedback = new RelativePopupValidationFeedback(
					RelativePopupValidationFeedback.BOTTOM);
			feedback.setCss("withBkg");
			b.getLeft().feedback = feedback;
		}
		changePasswordHolder.add(changePasswordView);
	}
}
