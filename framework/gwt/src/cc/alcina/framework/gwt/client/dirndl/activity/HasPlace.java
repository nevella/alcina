package cc.alcina.framework.gwt.client.dirndl.activity;

import cc.alcina.framework.gwt.client.place.BasePlace;

public interface HasPlace<P extends BasePlace> {
	P getPlace();

	void setPlace(P place);
}
