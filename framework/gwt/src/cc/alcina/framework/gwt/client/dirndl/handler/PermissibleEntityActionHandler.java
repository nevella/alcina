package cc.alcina.framework.gwt.client.dirndl.handler;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionHandler;
import cc.alcina.framework.common.client.actions.PermissibleEntityAction;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;


@ClientInstantiable
@Registration({ PermissibleActionHandler.class, PermissibleEntityAction.class })
public class PermissibleEntityActionHandler
		implements PermissibleActionHandler<PermissibleEntityAction> {
	@Override
	public void handleAction(Widget sourceWidget,
			PermissibleEntityAction action, Object target) {
		Place currentPlace = Client.currentPlace();
		if (currentPlace instanceof CategoryNamePlace) {
			CategoryNamePlace categoryNamePlace = ((CategoryNamePlace) currentPlace)
					.copy();
			categoryNamePlace.nodeName = null;
			Client.goTo(categoryNamePlace);
		}
	}
}
