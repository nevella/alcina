package com.google.gwt.place.shared;

import java.util.List;

public interface StructuredPlace extends Place {
	List<StructuredPlace> getFragments();

	void updateFrom(StructuredPlace outgoingPlace);
}
