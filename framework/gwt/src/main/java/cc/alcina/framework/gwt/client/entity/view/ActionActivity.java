package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.entity.place.ActionPlace;
import cc.alcina.framework.gwt.client.ide.provider.ActionPlaceHandler;
import cc.alcina.framework.gwt.client.place.TypedActivity;

public class ActionActivity extends TypedActivity<ActionPlace> {
	public ActionActivity(ActionPlace place) {
		super(place);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		ActionPlaceHandler handler = Registry.impl(ActionPlaceHandler.class,
				place.getClass());
		if (handler != null) {
			if (handler instanceof ActionWidgetSupplier) {
				panel.setWidget(
						((ActionWidgetSupplier) handler).getWidget(place));
			} else {
				handler.performAction();
			}
		}
	}
}
