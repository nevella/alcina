package cc.alcina.framework.gwt.client.dirndl.activity;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@Reflected
@Registration(DirectedEntityActivity.class)
public class DirectedEntityActivity<EP extends EntityPlace, E extends Entity>
		extends DirectedActivity<EP> {
	private E entity;

	private boolean entityNotFound;

	public E getEntity() {
		return this.entity;
	}

	public boolean isEntityNotFound() {
		return this.entityNotFound;
	}

	private void onCreate(Entity e, Runnable postCreate) {
		if (place.fromId != 0 && place.fromClass != null) {
			EntityPlace fromPlace = (EntityPlace) RegistryHistoryMapper.get()
					.getPlaceOrThrow(place.fromClass);
			Class implementationClass = fromPlace.provideEntityClass();
			Optional<Property> ownerReflector = Entity.Ownership
					.getOwnerReflectors(e.entityClass())
					.filter(r -> r.annotation(Association.class)
							.implementationClass() == implementationClass)
					.filter(Objects::nonNull).findFirst();
			Domain.async(implementationClass, place.fromId, false, parent -> {
				if (ownerReflector.isPresent()) {
					ownerReflector.get().set(e, parent);
				}
				postCreate.run();
			});
			return;
		}
		postCreate.run();
	}

	private Class<? extends Entity> provideDomainClass() {
		return getPlace().provideEntityClass();
	}

	private boolean provideIsCreate() {
		return place.getAction() == EntityAction.CREATE;
	}

	public void setEntity(E entity) {
		E old_entity = this.entity;
		this.entity = entity;
		propertyChangeSupport().firePropertyChange("entity", old_entity,
				entity);
	}

	public void setEntityNotFound(boolean entityNotFound) {
		boolean old_entityNotFound = this.entityNotFound;
		this.entityNotFound = entityNotFound;
		propertyChangeSupport().firePropertyChange("entityNotFound",
				old_entityNotFound, entityNotFound);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		Domain.async(provideDomainClass(), place.id, provideIsCreate(), e -> {
			Runnable postCreate = () -> {
				setEntity((E) e);
				if (e == null) {
					setEntityNotFound(true);
				}
				if (provideIsBound()) {
					// entity rendering is normally tree-shaped
					provideNode().dispatch(
							ModelEvents.TransformSourceModified.class, e);
				}
			};
			if (provideIsCreate()) {
				onCreate(e, postCreate);
			} else {
				postCreate.run();
			}
		});
		super.start(panel, eventBus);
	}
}
