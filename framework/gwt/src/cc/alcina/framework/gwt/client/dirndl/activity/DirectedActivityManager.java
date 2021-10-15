package cc.alcina.framework.gwt.client.dirndl.activity;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.web.bindery.event.shared.EventBus;

public class DirectedActivityManager extends ActivityManager {
	public DirectedActivityManager(ActivityMapper mapper, EventBus eventBus) {
		super(mapper, eventBus);
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
