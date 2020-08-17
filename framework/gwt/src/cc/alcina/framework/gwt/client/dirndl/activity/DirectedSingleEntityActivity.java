package cc.alcina.framework.gwt.client.dirndl.activity;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;

@RegistryLocation(registryPoint = DirectedSingleEntityActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class DirectedSingleEntityActivity<EP extends EntityPlace, E extends Entity>
		extends DirectedActivity<EP> {
	private E entity;

	public E getEntity() {
		return this.entity;
	}

	public void setEntity(E entity) {
		E old_entity = this.entity;
		this.entity = entity;
		propertyChangeSupport().firePropertyChange("entity", old_entity,
				entity);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		Domain.async(provideDomainClass(), place.id, provideIsCreate(), e -> {
			if (provideIsCreate()) {
				onCreate(e);
			}
			setEntity((E) e);
			fireUpdated();
		});
		super.start(panel, eventBus);
	}

	private void onCreate(Entity e) {
	}

	private boolean provideIsCreate() {
		return place.getAction() == EntityAction.CREATE;
	}

	private Class<? extends Entity> provideDomainClass() {
		return getPlace().provideEntityClass();
	}
}
