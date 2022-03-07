package cc.alcina.framework.gwt.client.gwittir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Action;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;

public class RenderedClass {
	public static List<String> allInterestingProperties(Object bean) {
		return Reflections.at(bean.getClass()).properties().stream()
				.filter(Property::isWriteable).map(p -> p.getName())
				.collect(Collectors.toList());
	}

	public static List<Class<? extends PermissibleAction>>
			getActions(Class<?> clazz, Object object) {
		List<Class<? extends PermissibleAction>> result = new ArrayList<Class<? extends PermissibleAction>>();
		ObjectActions actions = Reflections.at(clazz)
				.annotation(ObjectActions.class);
		for (Action action : actions.value()) {
			Class<? extends PermissibleAction> actionClass = action
					.actionClass();
			boolean noPermissionsCheck = actionClass == CreateAction.class
					|| actionClass == EditAction.class
					|| actionClass == ViewAction.class
					|| actionClass == DeleteAction.class;
			if (noPermissionsCheck || PermissionsManager.get().isPermitted(
					object, new AnnotatedPermissible(action.permission()))) {
				result.add(actionClass);
			}
		}
		return result;
	}

	public static String getTypeDisplayName(Class<?> beanClass) {
		Display display = Reflections.at(beanClass).annotation(Display.class);
		String tn = display == null ? "" : display.name();
		if (CommonUtils.isNullOrEmpty(tn)) {
			tn = CommonUtils.capitaliseFirst(beanClass.getSimpleName());
		}
		return TextProvider.get().getUiObjectText(beanClass,
				TextProvider.DISPLAY_NAME, tn);
	}
}
