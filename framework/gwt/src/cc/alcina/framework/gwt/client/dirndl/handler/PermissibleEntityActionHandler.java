package cc.alcina.framework.gwt.client.dirndl.handler;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleEntityAction;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.entity.view.ClientFactory;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@RegistryLocation(registryPoint = PermissibleActionHandler.class, targetClass = PermissibleEntityAction.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class PermissibleEntityActionHandler
		implements PermissibleActionHandler<PermissibleEntityAction> {
	@Override
	public void handleAction(Widget sourceWidget,
			PermissibleEntityAction action, Object target) {
		Place currentPlace = ClientFactory.currentPlace();
		if (currentPlace instanceof CategoryNamePlace) {
			CategoryNamePlace categoryNamePlace = ((CategoryNamePlace) currentPlace)
					.copy();
			categoryNamePlace.nodeName = null;
			ClientFactory.goTo(categoryNamePlace);
		}
	}
}
