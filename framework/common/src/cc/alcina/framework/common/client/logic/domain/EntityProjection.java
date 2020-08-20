package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;
import java.util.Collection;

public class EntityProjection {
	public interface ContextProjector{
		public abstract Object project(Object source);

		public abstract void registerProjected(Object source,
				Object projected);
	}
	public abstract static class EntitySingleDataObjectDecorator<E extends VersionableEntity> {
		public abstract E apply(E source,ContextProjector projector);
	}

	public abstract static class EntityMultipleDataObjectDecorator<E extends VersionableEntity> {
		public abstract E apply(E source,ContextProjector projector);
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

		public OneToManySummary<E> withCollection(Collection<E> collection,ContextProjector projector) {
			size = collection.size();
			lastModified = collection.stream().sorted(
					VersionableEntity.LAST_MODIFIED_COMPARATOR_DESCENDING)
					.findFirst().map(e->(E)projector.project(e)).orElse(null);
			return this;
		}
	}
}
