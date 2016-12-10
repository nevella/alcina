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
public abstract class GenericBasePlace<V extends HasIdAndLocalId, SD extends SearchDefinition>
		extends BasePlace {
	public String id;

	public SD def;

	public GenericBasePlace() {
		def = createSearchDefinition();
	}

	protected abstract SD createSearchDefinition();

	public void setId(long objectId) {
		id = String.valueOf(objectId);
	}

	public boolean provideIsDefaultDefs() {
		GenericBasePlace o = Reflections.classLookup().newInstance(getClass());
		return HasEquivalenceHelper.argwiseEquivalent(def, o.def);
	}

	public abstract Place toTokenizablePlace() ;
}
