package cc.alcina.framework.gwt.client.dirndl.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;

@RegistryLocation(registryPoint = DirectedActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
/*
 * This class (and subclasses) act as the main model nodes for app views
 */
public class DirectedActivity<P extends BasePlace> extends Model
		implements Activity {
	protected P place;

	public void setPlace(P place) {
		this.place = place;
	}

	public DirectedActivity() {
	}

	public P getPlace() {
		return this.place;
	}

	private static Topic<DirectedActivity> topicActivityStarted = Topic.local();

	public static Topic<DirectedActivity> topicActivityStarted() {
		return topicActivityStarted;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// ask the framework to render this activity
		topicActivityStarted().publish(this);
	}

	@Override
	public void onStop() {
		// could publish for cleanup, but don't see any use case
	}

	@Override
	public String mayStop() {
		return null;
	}

	@Override
	public void onCancel() {
	}

	public static Activity forPlace(Place place) {
		DirectedActivity directedActivity = null;
		if (place instanceof EntityPlace) {
			if (((EntityPlace) place).id != 0) {
				directedActivity = Registry.impl(
						DirectedSingleEntityActivity.class, place.getClass());
			} else {
				directedActivity = Registry.impl(
						DirectedMultipleEntityActivity.class, place.getClass());
			}
		} else {
			directedActivity = Registry.impl(DirectedActivity.class,
					place.getClass());
		}
		directedActivity.setPlace((BasePlace) place);
		return directedActivity;
	}
}
