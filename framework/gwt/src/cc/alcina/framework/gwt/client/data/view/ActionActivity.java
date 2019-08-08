package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.gwt.client.data.place.ActionPlace;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionRegistry;
import cc.alcina.framework.gwt.client.place.TypedActivity;

public class ActionActivity extends TypedActivity<ActionPlace> {
	public ActionActivity(ActionPlace place) {
		super(place);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		LooseActionHandler handler = LooseActionRegistry.get()
				.getHandler((place).actionName);
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
