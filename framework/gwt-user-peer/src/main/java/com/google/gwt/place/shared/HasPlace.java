package com.google.gwt.place.shared;

public interface HasPlace<P extends StructuredPlace> {
	P getPlace();

	void setPlace(P place);
}
