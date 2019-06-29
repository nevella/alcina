package cc.alcina.extras.dev.console.remote.client.common.widget.main;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.lux.IClientFactory;

public class RootComponent extends Composite implements AcceptsOneWidget {
	private SimplePanel sp;

	public RootComponent() {
		this.sp = new SimplePanel();
		initWidget(sp);
		ActivityMapper activityMapper = Registry.impl(ActivityMapper.class);
		IClientFactory clientFactory = Registry.impl(IClientFactory.class);
		ActivityManager activityManager = new ActivityManager(activityMapper,
				clientFactory.getEventBus());
		activityManager.setDisplay(this);
	}

	@Override
	public void setWidget(IsWidget w) {
		sp.setWidget(w);
	}
}
