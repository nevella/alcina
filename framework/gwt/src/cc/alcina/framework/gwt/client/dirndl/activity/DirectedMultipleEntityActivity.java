package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.List;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;

@RegistryLocation(registryPoint = DirectedMultipleEntityActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class DirectedMultipleEntityActivity<EP extends EntityPlace, E extends Entity>
		extends DirectedActivity<EP> {
	private List<E> entities;

	private EntitySearchDefinition esd;

	public List<E> getEntities() {
		return this.entities;
	}

	public void setEntities(List<E> entities) {
		List<E> old_entities = this.entities;
		this.entities = entities;
		propertyChangeSupport().firePropertyChange("entities", old_entities,
				entities);
	}

	public EntitySearchDefinition getEsd() {
		return this.esd;
	}

	public void setEsd(EntitySearchDefinition esd) {
		EntitySearchDefinition old_esd = this.esd;
		this.esd = esd;
		propertyChangeSupport().firePropertyChange("esd", old_esd, esd);
	}
}
