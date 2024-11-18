package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Delegating;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.place.BasePlace;

/**
 * <p>
 * The basic renderer of a fully place-driven Dirndl app. {@link #mainActivity}
 * corresponds to the main place, {@link #fragmentActivities} to the place's
 * fragments
 * 
 * <p>
 * Since this model is delegating (to mainActivity), we're forced to render the
 * fragmentActivities as overlays - *however* that's actually correct. If a
 * top-level activity could render fragments, they should be modelled as
 * properties somewhere inside the model's descendant objects top-level (and
 * hooked up to the main Topic with a filter). So rendering the fragments is
 * logically correct - and in the first use case (a help UI), it's the correct
 * modality
 */
@Delegating
@TypedProperties
public class RootArea extends Model.Fields {
	public static PackageProperties._RootArea properties = PackageProperties.rootArea;

	@Directed
	public DirectedActivity mainActivity;

	Map<Class<? extends BasePlace>, ChannelOverlay> channelOverlays = new LinkedHashMap<>();

	@Delegating
	@TypedProperties
	@Bean(PropertySource.FIELDS)
	static class ChannelOverlay extends Model.Fields {
		public static PackageProperties._RootArea properties = PackageProperties.rootArea;

		Class<? extends BasePlace> channel;

		@Directed
		DirectedActivity activity;

		Overlay overlay;

		ChannelOverlay(Class<? extends BasePlace> channel,
				DirectedActivity activity) {
			this.channel = channel;
			this.overlay = Overlay.builder().withContents(this)
					.positionViewportCentered().build();
			overlay.open();
		}

		public void setActivity(DirectedActivity activity) {
			set("activity", this.activity, activity,
					() -> this.activity = activity);
		}
	}

	/*
	 * Note that a method handle block returns a new lambda each time - so we
	 * make a field ref, which can be correctly removed
	 */
	TopicListener<DirectedActivity> onActivityStartedRef = this::onActivityStarted;

	TopicListener<Class<? extends BasePlace>> onChannelStoppedRef = this::onChannelStopped;

	public RootArea() {
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		DirectedActivity.Topics.get().topicActivityStarted
				.addWithPublishedCheck(onActivityStartedRef);
		DirectedActivity.Topics.get().topicChannelStopped
				.addWithPublishedCheck(onChannelStoppedRef);
		super.onBeforeRender(event);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			Preconditions.checkNotNull(onActivityStartedRef);
		} else {
			DirectedActivity.Topics.get().topicActivityStarted
					.remove(onActivityStartedRef);
			DirectedActivity.Topics.get().topicChannelStopped
					.remove(onChannelStoppedRef);
		}
	}

	protected void onActivityStarted(DirectedActivity activity) {
		if (activity.channel == null) {
			properties.mainActivity.set(this, activity);
		} else {
			updateFragmentOverlays(activity);
		}
	}

	protected void onChannelStopped(Class<? extends BasePlace> channel) {
		channelOverlays.remove(channel).overlay.close();
	}
	//

	void updateFragmentOverlays(DirectedActivity activity) {
		channelOverlays
				.computeIfAbsent(activity.channel,
						channel -> new ChannelOverlay(channel, activity))
				.setActivity(activity);
	}
}