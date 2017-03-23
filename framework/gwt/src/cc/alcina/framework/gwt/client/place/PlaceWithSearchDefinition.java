package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;

public interface PlaceWithSearchDefinition<SD extends SearchDefinition> {
	public SD getSearchDefinition();

	default boolean provideIsDefaultDefs() {
		PlaceWithSearchDefinition<SD> defaultPlace = Reflections.classLookup()
				.newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(getSearchDefinition(),
				defaultPlace.getSearchDefinition());
	}
}
