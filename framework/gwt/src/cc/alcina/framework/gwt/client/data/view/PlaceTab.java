package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.place.shared.Place;

import cc.alcina.framework.gwt.client.widget.BaseTab;

public abstract class PlaceTab extends BaseTab {
	public abstract Place getPlace();

	public Class<? extends Place> getPlaceBaseClass() {
		return getPlace().getClass();
	}
}
