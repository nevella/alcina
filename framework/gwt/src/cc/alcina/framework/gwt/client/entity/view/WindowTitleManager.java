package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.place.BasePlace;

@RegistryLocation(registryPoint = WindowTitleManager.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class WindowTitleManager {
	private EventBus eventBus;

	private String appName;

	private String defaultPlaceName;

	public WindowTitleManager() {
	}

	public void init(EventBus eventBus, String defaultPlaceName,
			String appName) {
		this.eventBus = eventBus;
		this.defaultPlaceName = defaultPlaceName;
		this.appName = appName;
		setup();
		DetailView.topicDetailModelObjectSet().add(new TopicListener<Entity>() {
			@Override
			public void topicPublished(String key, Entity message) {
				updateTitle(ClientFactory.currentPlace());
			}
		});
	}

	protected String getTitlePartFromPlace(Place place,
			String defaultPlaceName) {
		String category = place.getClass().getSimpleName()
				.replaceFirst("(.*)Place", "$1");
		if (place instanceof BasePlace) {
			return ((BasePlace) place).toTitleString();
		} else {
			return category;
		}
	}

	protected void updateTitle(Place place) {
		Window.setTitle(Ax.format("%s - %s",
				getTitlePartFromPlace(place, defaultPlaceName), appName));
	}

	void setup() {
		final HandlerRegistration placeReg = eventBus.addHandler(
				PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
					@Override
					public void onPlaceChange(PlaceChangeEvent event) {
						Place newPlace = event.getNewPlace();
						updateTitle(newPlace);
					}
				});
	}
}
