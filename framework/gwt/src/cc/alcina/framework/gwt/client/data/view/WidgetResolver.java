package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.user.client.ui.IsWidget;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = WidgetResolver.class)
public class WidgetResolver {
	public IsWidget resolve(Class modelClass, Class context) {
		return (IsWidget) Registry.impl(WidgetResolver.class, modelClass);
	}
}
