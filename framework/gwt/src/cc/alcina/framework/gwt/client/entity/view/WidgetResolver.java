package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.user.client.ui.IsWidget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration(WidgetResolver.class)
public class WidgetResolver {
	public IsWidget resolve(Class modelClass, Class context) {
		return (IsWidget) Registry.query(WidgetResolver.class)
				.addKeys(modelClass).impl();
	}
}
