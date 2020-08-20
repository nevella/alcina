package cc.alcina.framework.servlet.servlet.search;

import java.util.Collection;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityProjection.ContextProjector;
import cc.alcina.framework.common.client.logic.domain.EntityProjection.EntityMultipleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.EntityProjection.EntitySingleDataObjectDecorator;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjections;

/*
 * Deliberately does not extend SearchResultProjector. To use this, create a subclass of SearchResultProjector (with a higher registry priority) and delegate to this
 */
public class EntityDataObjectProjector {
	private boolean multiple;

	public <T> T project(T object) {
		multiple = object instanceof Collection;
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
				public void registerProjected(Object source, Object projected) {
					graphProjection.registerProjected( source,  projected);
				}
				
				@Override
				public Object project(Object source) {
					try {
						return graphProjection.project(source, context);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
					
				}
			};
			if (original instanceof VersionableEntity) {
				if (multiple) {
					EntityMultipleDataObjectDecorator multipleDataObjectDecorator = Registry
							.implOrNull(EntityMultipleDataObjectDecorator.class,
									((Entity) original).entityClass());
					if (multipleDataObjectDecorator != null) {
						return (T) multipleDataObjectDecorator.apply(
								(VersionableEntity) original, contextProjector);
					}
				}
				EntitySingleDataObjectDecorator singleDataObjectDecorator = Registry
						.implOrNull(EntitySingleDataObjectDecorator.class,
								((Entity) original).entityClass());
				if (singleDataObjectDecorator != null) {
					return (T) singleDataObjectDecorator.apply(
							(VersionableEntity) original, contextProjector);
				}
			}
			// lists (search result lists) will be projected normally - this is
			// to filter entity.[oneToMany]
			if (original instanceof Set) {
				return (T) new LiSet();
			}
			return super.filterData(original, projected, context,
					graphProjection);
		}
	}

}
