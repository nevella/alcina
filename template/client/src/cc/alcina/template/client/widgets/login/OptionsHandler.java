package cc.alcina.template.client.widgets.login;

import java.util.ArrayList;
import java.util.Collection;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent.PermissibleActionListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

public class OptionsHandler implements LoginStateVisibleWithWidget,
		ClickHandler, PermissibleActionEvent.PermissibleActionListener {
	private Link hyperlink;

	private Collection beans;

	public OptionsHandler() {
		this.hyperlink = new Link("Options", true);
		hyperlink.addClickHandler(this);
	}

	public Widget getWidget() {
		return this.hyperlink;
	}

	public void onClick(ClickEvent clickEvent){
		beans = new ArrayList();
		beans.add(AlcinaTemplateObjects.current().getGeneralProperties());
		try {
			ArrayList beansCopy = new ArrayList();
			for (Object bean : beans) {
				if (bean instanceof Permissible) {
					if (!PermissionsManager.get().isPermissible(
							(Permissible) bean)) {
						continue;
					}
				}
				beansCopy.add(new CloneHelper().shallowishBeanClone(bean));
			}
			beans = beansCopy;
		} catch (Exception e) {
			throw new WrappedRuntimeException("Unable to clone: ", e,
					SuggestedAction.NOTIFY_WARNING);
		}
		ContentViewFactory cvf = new ContentViewFactory();
		cvf.setNoCaptionsOrButtons(true);
		TabPanel tp = new TabPanel();
		for (Object bean : beans) {
			tp.add(cvf.createBeanView(bean, true, this, false, true),
					ClientReflector.get().beanInfoForClass(bean.getClass())
							.getTypeDisplayName());
		}
		tp.selectTab(0);
		TransformManager.get().registerProvisionalObject(beans);
		new OkCancelDialogBox("Options", tp, this).show();
	}

	public void vetoableAction(PermissibleActionEvent evt) {
		if (evt.getAction().getActionName().equals(OkCancelDialogBox.OK_ACTION)) {
			TransformManager.get().promoteToDomainObject(beans);
		} else {
			TransformManager.get().deregisterProvisionalObjects(beans);
		}
	}

	public boolean visibleForLoginState(LoginState state) {
		return state == LoginState.LOGGED_IN;
	}

	public String getDebugId() {
		return AlcinaDebugIds.getButtonId(AlcinaDebugIds.TOP_BUTTON_OPTIONS);
	}
}
