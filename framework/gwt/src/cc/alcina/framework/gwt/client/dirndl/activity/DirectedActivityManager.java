package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.NoSuchElementException;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.web.bindery.event.shared.EventBus;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;

public class DirectedActivityManager extends ActivityManager {
	public DirectedActivityManager(ActivityMapper mapper, EventBus eventBus) {
		super(mapper, eventBus);
		/*
		 * The place -> activity will be buffered by {@link
		 * Topic#retainPublished} - so it is legal (and makes sense) to attach
		 * placechange handlers early
		 */
		updateHandlers(true);
	}

	@Override
	protected Activity getNextActivity(PlaceChangeEvent event) {
		return mapper.getActivity(event.getNewPlace(), channel);
	}

	@Override
	protected void onStart() {
		// since not using the activity->widget mapping, which controls
		// startingNext
		if (currentActivity instanceof DirectedActivity) {
			startingNext = false;
		}
	}

	@Override
	protected ActivityManager createFragmentManager() {
		return new DirectedActivityManager(mapper, eventBus);
	}

	@Override
	protected void onChannelStopped(Class<? extends BasePlace> channel) {
		DirectedActivity.Topics.get().topicChannelStopped.publish(channel);
	}

	public static class DefaultMapper implements ActivityMapper {
		@Override
		public Activity getActivity(Place place) {
			return getActivity(place, null);
		}

		@Override
		public Activity getActivity(Place place,
				Class<? extends Place> channel) {
			Activity activity = DirectedActivity.forPlace(place, channel);
			if (activity != null) {
				return activity;
			}
			throw new NoSuchElementException(
					Ax.format("No activity for place: %s", place));
		}
	}
}
