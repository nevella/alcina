package cc.alcina.framework.servlet.servlet.search;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.ManyToMany;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.ContextProjector;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntityMultipleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.EntitySingleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;

/*
 * Deliberately does not extend SearchResultProjector. To use this, create a subclass of SearchResultProjector (with a higher registry priority) and delegate to this
 */
public class EntityDataObjectProjector {
	private boolean multiple;

	private boolean projectAsSingleEntityDataObjects;

	public EntityDataObjectProjector withProjectAsSingleEntityDataObjects(
			boolean projectAsSingleEntityDataObjects) {
		this.projectAsSingleEntityDataObjects = projectAsSingleEntityDataObjects;
		return this;
	}

	public <T> T project(T object) {
		multiple = object instanceof Collection
				&& !projectAsSingleEntityDataObjects;
		if (multiple) {
			Collection<VersionableEntity> collection = (Collection) object;
			if (collection.size() > 0) {
				Entity first = collection.iterator().next();
				EntityMultipleDataObjectDecorator multipleDataObjectDecorator = Registry
						.implOrNull(EntityMultipleDataObjectDecorator.class,
								(first).entityClass());
				if (multipleDataObjectDecorator != null) {
					object = (T) collection.stream()
							.map(multipleDataObjectDecorator)
							.collect(Collectors.toList());
				}
			}
		}
		return GraphProjections.defaultProjections()
				.dataFilter(new DataFilter()).project(object);
	}

	public class DataFilter extends CollectionProjectionFilter {
		@Override
		public <T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception {
			ContextProjector contextProjector = new ContextProjector() {
				@Override
				public Object project(Object source) {
					try {
						return graphProjection.project(source, context);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}

				@Override
				public <E> E registerProjected(E source, E dataObject) {
					return graphProjection.registerProjected(source,
							dataObject);
				}
			};
			if (projected instanceof EntityDataObject) {
				// passthrough
			} else if (original instanceof VersionableEntity) {
				if (multiple) {
					// FIXME - dirndl.2 - we can't (because of graphiness) guarantee
					// objects that exist at depth==2 are encountered first at
					// depth ==2
					// if (context.depth() > 2) {
					// return null;
					// }
				} else {
					EntitySingleDataObjectDecorator singleDataObjectDecorator = Registry
							.implOrNull(EntitySingleDataObjectDecorator.class,
									((Entity) original).entityClass());
					if (singleDataObjectDecorator != null) {
						projected = (T) singleDataObjectDecorator.apply(
								(VersionableEntity) original, contextProjector);
						return graphProjection.project(original, projected,
								context, false);
					}
				}
			} else {
				// lists (search result lists) will be projected normally -
				// this
				// is
				// to filter entity.[oneToMany]
				if (original instanceof Set) {
					Set replacement = maybeReplaceSet(original, projected,
							context);
					if (replacement != projected) {
						return (T) replacement;
					}
				}
			}
			return super.filterData(original, projected, context,
					graphProjection);
		}
	}

	public Class<? extends Bindable>
			getProjectedClass(ModelSearchResults modelSearchResults) {
		Class<VersionableEntity> clazz = null;
		if (modelSearchResults.def != null) {
			clazz = ((EntitySearchDefinition) modelSearchResults.def)
					.resultClass();
			EntityMultipleDataObjectDecorator multipleDataObjectDecorator = Registry
					.implOrNull(EntityMultipleDataObjectDecorator.class, clazz);
			if (multipleDataObjectDecorator != null) {
				return multipleDataObjectDecorator.getProjectedClass();
			}
		} else if (modelSearchResults.rawEntity != null) {
			clazz = modelSearchResults.rawEntity.entityClass();
			EntitySingleDataObjectDecorator singleDataObjectDecorator = Registry
					.implOrNull(EntitySingleDataObjectDecorator.class, clazz);
			if (singleDataObjectDecorator != null) {
				return singleDataObjectDecorator.createDataObject().getClass();
			}
		}
		return clazz;
	}

	public <T> Set maybeReplaceSet(T original, T projected,
			GraphProjectionContext context) {
		if (context.parent(o -> o instanceof VersionableEntity).isPresent()
				&& context.sourceOwner instanceof VersionableEntity) {
			if (SEUtilities
					.getPropertyDescriptorByName(
							((Entity) context.sourceOwner).entityClass(),
							context.fieldName)
					.getReadMethod().getAnnotation(ManyToMany.class) != null) {
			} else {
				return new LiSet();
			}
		}
		return (Set) projected;
	}
}
