package cc.alcina.framework.gwt.client.cell;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public abstract class TypedActivityMapper<P extends Place> implements
		ActivityMapper {
	private Class<P> clazz;

	public TypedActivityMapper(Class<P> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Activity getActivity(Place place) {
		if (place.getClass() != clazz) {
			return null;
		}
		return new AbstractActivity() {
			@Override
			public void start(AcceptsOneWidget panel, EventBus eventBus) {
				TypedActivityMapper.this.start(panel, (P) place);
			}
			@Override
			public void onCancel() {
				TypedActivityMapper.this.stop();
			}
			@Override
			public void onStop() {
				TypedActivityMapper.this.stop();
			}
			
		};
	}

	protected void stop() {
	}

	protected abstract void start(AcceptsOneWidget panel, P place);
}
