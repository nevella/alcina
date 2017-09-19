package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;

@RegistryLocation(registryPoint = GenericBasePlace.class)
public abstract class GenericBasePlace<SD extends SearchDefinition>
		extends BasePlace implements PlaceWithSearchDefinition<SD> {
	public GenericBasePlace() {
		def = createSearchDefinition();
	}

	public long fromId;

	public long id;

	public SD def;

	@Override
	public SD getSearchDefinition() {
		return def;
	}

	public String stringId() {
		return String.valueOf(id);
	}

	public <T extends GenericBasePlace> T putId(String stringId) {
		id = Long.parseLong(stringId);
		return (T) this;
	}

	protected abstract SD createSearchDefinition();

	public boolean provideIsDefaultDefs() {
		GenericBasePlace o = Reflections.classLookup().newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(def, o.def);
	}
}
