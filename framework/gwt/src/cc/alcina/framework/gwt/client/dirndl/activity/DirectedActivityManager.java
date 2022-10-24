package cc.alcina.framework.gwt.client.dirndl.activity;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

public class DirectedActivityManager extends ActivityManager {
	public DirectedActivityManager(ActivityMapper mapper, EventBus eventBus) {
		super(mapper, eventBus);
		// dirndl activity routing doesn't follow this model - rather there's
		// one extra layer - FIXME - dirndl doc
		setDisplay(new SimplePanel() {
			@Override
			public void setWidget(Widget w) {
				if (w != null) {
					throw new UnsupportedOperationException();
				}
			}
		});
	}

	@Override
	protected void onStart() {
		// since not using the activity->widget mapping, which controls
		// startingNext
		if (currentActivity instanceof DirectedActivity) {
			startingNext = false;
		}
	}
}
