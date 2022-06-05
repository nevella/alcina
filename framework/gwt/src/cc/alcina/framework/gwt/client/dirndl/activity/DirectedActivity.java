package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.Optional;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.action.PlaceAction;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.view.AppController;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BindablePlace;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

@Reflected
@Registration(DirectedActivity.class)
public class DirectedActivity<P extends BasePlace> extends Model
		implements Activity, HasPlace<P> {
	public static final Topic<DirectedActivity> topicActivityStarted = Topic
			.create();

	public static Activity forPlace(Place place) {
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
					directedActivity = Registry
							.query(DirectedEntityActivity.class)
							.addKeys(place.getClass()).impl();
				} else {
					directedActivity = Registry
							.query(DirectedBindableSearchActivity.class)
							.addKeys(place.getClass()).impl();
				}
			} else if (place instanceof BindablePlace) {
				directedActivity = Registry
						.query(DirectedBindableSearchActivity.class)
						.addKeys(place.getClass()).impl();
			} else if (place instanceof CategoryNamePlace) {
				CategoryNamePlace categoryPlace = (CategoryNamePlace) place;
				PermissibleAction action = categoryPlace.ensureAction();
				if (action instanceof PlaceAction) {
					AppController.get().goToPlaceReplaceCurrent(
							((PlaceAction) action).getTargetPlace());
					return ActivityManager.NULL_ACTIVITY;
				}
				if (categoryPlace.nodeName == null) {
					directedActivity = Registry
							.query(DirectedCategoriesActivity.class)
							.addKeys(place.getClass()).impl();
				} else {
					directedActivity = Registry
							.query(DirectedCategoryActivity.class)
							.addKeys(place.getClass()).impl();
				}
			} else {
				directedActivity = Registry.query(DirectedActivity.class)
						.addKeys(place.getClass()).impl();
			}
		}
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
		// could publish for cleanup, but don't see any use case
	}

	@Override
	public void setPlace(P place) {
		this.place = place;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// ask the framework to render this activity
		topicActivityStarted.publish(this);
	}

	public <DA extends DirectedActivity> DA withPlace(P place) {
		setPlace(place);
		return (DA) this;
	}

	public static interface Provider<P extends Place> {
		DirectedActivity getActivity(P place);
	}
}
