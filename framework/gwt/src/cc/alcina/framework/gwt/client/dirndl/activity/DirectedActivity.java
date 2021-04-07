package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.Optional;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.gwt.client.dirndl.action.PlaceAction;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.AppController;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@RegistryLocation(registryPoint = DirectedActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
/*
 * This class (and subclasses) act as the main model nodes for app views
 */
public class DirectedActivity<P extends BasePlace> extends Model
		implements Activity {
	private static Topic<DirectedActivity> topicActivityStarted = Topic.local();

	public static Activity forPlace(Place place) {
		DirectedActivity directedActivity = null;
		Optional<Provider> provider = Registry
				.optional(DirectedActivity.Provider.class, place.getClass());
		if (provider.isPresent()) {
			directedActivity = provider.get().getActivity(place);
		} else {
			if (place instanceof EntityPlace) {
				EntityPlace entityPlace = (EntityPlace) place;
				if (entityPlace.id != 0
						|| entityPlace.action == EntityAction.CREATE) {
					directedActivity = Registry.impl(
							DirectedEntityActivity.class, place.getClass());
				} else {
					directedActivity = Registry.impl(
							DirectedBindableSearchActivity.class,
							place.getClass());
				}
			} else if (place instanceof BindablePlace) {
				directedActivity = Registry.impl(
						DirectedBindableSearchActivity.class, place.getClass());
			} else if (place instanceof CategoryNamePlace) {
				CategoryNamePlace categoryPlace = (CategoryNamePlace) place;
				PermissibleAction action = categoryPlace.ensureAction();
				if (action instanceof PlaceAction) {
					AppController.get().goToPlaceReplaceCurrent(
							((PlaceAction) action).getTargetPlace());
					return ActivityManager.NULL_ACTIVITY;
				}
				if (categoryPlace.nodeName == null) {
					directedActivity = Registry.impl(
							DirectedCategoriesActivity.class, place.getClass());
				} else {
					directedActivity = Registry.impl(
							DirectedCategoryActivity.class, place.getClass());
				}
			} else {
				directedActivity = Registry.impl(DirectedActivity.class,
						place.getClass());
			}
		}
		directedActivity.setPlace((BasePlace) place);
		return directedActivity;
	}

	public static Topic<DirectedActivity> topicActivityStarted() {
		return topicActivityStarted;
	}

	protected P place;

	public DirectedActivity() {
	}

	public P getPlace() {
		return this.place;
	}

	@Override
	public String mayStop() {
		return null;
	}

	@Override
	public void onCancel() {
	}

	@Override
	public void onStop() {
		// could publish for cleanup, but don't see any use case
	}

	public void setPlace(P place) {
		this.place = place;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// ask the framework to render this activity
		topicActivityStarted().publish(this);
	}

	public static interface Provider<P extends Place> {
		DirectedActivity getActivity(P place);
	}
}
