package cc.alcina.framework.gwt.client.place;

import com.google.gwt.place.shared.Place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;

@RegistryLocation(registryPoint = GenericBasePlace.class)
@ClientInstantiable
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

	protected abstract SD createSearchDefinition();

	public boolean provideIsDefaultDefs() {
		GenericBasePlace o = Reflections.classLookup().newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(def, o.def);
	}
}
