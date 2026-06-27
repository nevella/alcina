package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@TypedProperties
public abstract class ServerConsoleContents<RP extends ServerConsolePlace>
		extends Model.All implements Registration.AllSubtypes, IfNotEqual,
		ReflectedEvents.PlaceChanged.Handler {
	@Directed.Exclude
	public RP place;

	/*
	 * All place updates should be handled by the ServerConsoleContents subtype,
	 * _if_ the place type is unchanged
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass();
	}

	static <RP extends ServerConsolePlace> ServerConsoleContents<RP>
			forPlace(RP place) {
		ServerConsoleContents<RP> result = Registry
				.impl(ServerConsoleContents.class, place.getClass());
		result.place = place;
		return result;
	}

	@Override
	public void onPlaceChanged(ReflectedEvents.PlaceChanged event) {
		RP place = event.getModel();
		if (Reflections.at(this)
				.firstGenericBound() == ((Class) place.getClass())) {
			Reflections.at(this).property("place").set(this, place);
		}
	}
}
