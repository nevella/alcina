package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

public interface EntityDataObject {
	public interface ContextProjector {
		public abstract Object project(Object source);

		public abstract <E> E registerProjected(E source, E dataObject);
	}

	public abstract static class EntitySingleDataObjectDecorator<E extends VersionableEntity, EDO extends E> {
		public E apply(E source, ContextProjector projector) {
			EDO dataObject = createDataObject();
			E originalProjection = projector.registerProjected(source,
					dataObject);
			decorate(source, dataObject, projector);
			projector.registerProjected(source, originalProjection);
			return dataObject;
		}

		protected abstract void decorate(E source, EDO dataObject,
				ContextProjector projector);

		public abstract EDO createDataObject();
	}

	public interface EntityMultipleDataObjectDecorator<E extends VersionableEntity, B extends Bindable>
			extends Function<E, B> {
		Class<? extends Bindable> getProjectedClass();
	}

	public static class OneToManySummary<E extends VersionableEntity>
			implements Serializable {
		private int size;

		public int getSize() {
			return this.size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public E getLastModified() {
			return this.lastModified;
		}

		public void setLastModified(E lastModified) {
			this.lastModified = lastModified;
		}

		private E lastModified;

		private String entityClassName;

		public String getEntityClassName() {
			return this.entityClassName;
		}

		public OneToManySummary<E> withCollection(Collection<E> collection,
				ContextProjector projector, Class<E> entityClass) {
			size = collection.size();
			lastModified = collection.stream().sorted(
					VersionableEntity.LAST_MODIFIED_COMPARATOR_DESCENDING)
					.findFirst().map(e -> (E) projector.project(e))
					.orElse(null);
			entityClassName = entityClass.getName();
			return this;
		}
	}

	public static class OneToManyMultipleSummary implements Serializable {
		private int size;

		public int getSize() {
			return this.size;
		}

		private transient String collectionAccessorMethodName;
		private transient Class<? extends Entity> entityClass;

		public Class<? extends Entity> getEntityClass() {
			return this.entityClass;
		}


		public OneToManyMultipleSummary(String collectionAccessorMethodName,
				Class<? extends Entity> entityClass) {
			this.collectionAccessorMethodName = collectionAccessorMethodName;
			this.entityClass = entityClass;
		}


		public String getCollectionAccessorMethodName() {
			return this.collectionAccessorMethodName;
		}


		public void setSize(int size) {
			this.size = size;
		}

		private EntityPlace place;

		public EntityPlace getPlace() {
			return this.place;
		}

		public void setPlace(EntityPlace place) {
			this.place = place;
		}

		public OneToManyMultipleSummary() {
		}

		public OneToManyMultipleSummary(Entity source,
				Collection<? extends Entity> collection, Class<? extends Entity> entityClass) {
			this.entityClass = entityClass;
			size = collection.size();
			place = (EntityPlace) RegistryHistoryMapper.get()
					.getPlaceByModelClass(entityClass);
			TruncatedObjectCriterion objectCriterion = Registry
					.impl(TruncatedObjectCriterion.class, source.entityClass());
			objectCriterion.withObject(source);
			place.def.addCriterionToSoleCriteriaGroup(objectCriterion);
		}
	}
}
