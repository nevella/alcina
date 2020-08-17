package cc.alcina.framework.gwt.client.module.support.login.pub;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.gwt.client.place.TypedActivity;

public class SimpleLoginActivity extends TypedActivity<LoginPlace> {
	public SimpleLoginActivity(LoginPlace place) {
		super(place);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		throw new UnsupportedOperationException();
	}
}
