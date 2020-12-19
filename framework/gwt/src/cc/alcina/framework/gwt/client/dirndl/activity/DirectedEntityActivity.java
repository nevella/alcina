package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;

@RegistryLocation(registryPoint = DirectedEntityActivity.class, implementationType = ImplementationType.INSTANCE)
@ClientInstantiable
public class DirectedEntityActivity<EP extends EntityPlace, E extends Entity>
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
			Runnable postCreate = () -> {
				setEntity((E) e);
				fireUpdated();
			};
			if (provideIsCreate()) {
				onCreate(e, postCreate);
			} else {
				postCreate.run();
			}
		});
		super.start(panel, eventBus);
	}

	private void onCreate(Entity e, Runnable postCreate) {
		if (place.fromId != 0) {
			Optional<PropertyReflector> parentReflector = 	ClientReflector.get()
			.beanInfoForClass(e.entityClass()).getParentReflector();
			
			if (parentReflector.isPresent()) {
				Class implementationClass = parentReflector.get().getAnnotation(Association.class)
						.implementationClass();
				Domain.async(
						implementationClass,
						place.fromId, false, parent -> {
							parentReflector.get().setPropertyValue(e, parent);
								postCreate.run();
						});
				return;
			}
		}
		postCreate.run();
	}

	private boolean provideIsCreate() {
		return place.getAction() == EntityAction.CREATE;
	}

	private Class<? extends Entity> provideDomainClass() {
		return getPlace().provideEntityClass();
	}
}
