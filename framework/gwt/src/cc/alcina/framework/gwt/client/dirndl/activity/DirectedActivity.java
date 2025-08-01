package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.Optional;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.PlaceUpdateable;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.MultipleImplementationsException;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.action.PlaceAction;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.AppController;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

/**
 * <h2>TransformSourceModified.Handler implementation</h2>
 * <p>
 * Implementations may require additional (remote) data before rendering - the
 * dirndl way to implement that is via emission of a TransformSourceModified
 * event (which both DirectedEntityActivity and DirectedBindableSearchActivity
 * do), and interception/re-render (via property change) by the container or the
 * activity subclass itself
 *
 * <p>
 * Normally application routing works by having a 'main area', the type of which
 * is a DirectedActivity. Changes to that activity can either be rendered
 * directed (if the activity is itself the UI Model) or be transformed into the
 * appropriate UI model via &#064;Directed.Transform
 *
 * @param <P>
 * 
 * @see PlaceUpdateable
 */
@Registration(DirectedActivity.class)
@TypedProperties
public class DirectedActivity<P extends BasePlace> extends Model
		implements Activity, HasPlace<P> {
	@Registration.Singleton
	@Registration.EnvironmentSingleton
	public static class Topics {
		public static Topics get() {
			return Registry.impl(Topics.class);
		}

		public final Topic<DirectedActivity> topicActivityStarted = Topic.RetainMultiple
				.create().withRetainPublished(true);

		public final Topic<DirectedActivity> topicActivityStopped = Topic.RetainMultiple
				.create().withRetainPublished(true);

		public final Topic<Class<? extends BasePlace>> topicChannelStopped = Topic
				.create().withRetainPublished(true);
	}

	public Class<? extends Place> channel;

	public static Activity forPlace(Place place,
			Class<? extends Place> channel) {
		if (!(place instanceof BasePlace)) {
			return null;
		}
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
				try {
					directedActivity = Registry.impl(DirectedActivity.class,
							place.getClass());
				} catch (MultipleImplementationsException e) {
					// no activity
					return null;
				}
			}
		}
		directedActivity.channel = channel;
		directedActivity.setPlace((BasePlace) place);
		return directedActivity;
	}

	protected P place;

	public DirectedActivity() {
	}

	@Override
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
		Topics.get().topicActivityStopped.publish(this);
	}

	@Override
	public void setPlace(P place) {
		set("place", this.place, place, () -> this.place = place);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// ask the framework to render this activity
		Topics.get().topicActivityStarted.publish(this);
	}

	public <DA extends DirectedActivity> DA withPlace(P place) {
		setPlace(place);
		return (DA) this;
	}

	public static transient PackageProperties._DirectedActivity properties = PackageProperties.directedActivity;

	/*
	 * Non-auto-registered
	 */
	// @Registration.NonGenericSubtypes(Provider.class)
	@EnvironmentRegistration
	public static interface Provider<P extends Place> {
		DirectedActivity getActivity(P place);
	}
}
