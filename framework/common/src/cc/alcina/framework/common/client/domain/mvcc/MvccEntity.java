package cc.alcina.framework.common.client.domain.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * This acts as a wrapper for an {@link Entity} type, generating source along
 * with an {@link EntityWriter} implementation
 */
public class MvccEntity {
	Class<? extends Entity> clazz;

	public MvccEntity(Class<? extends Entity> clazz) {
		this.clazz = clazz;
	}

	public void write(EntityWriter writer) {
		writer.enter(clazz);
	}

	public interface EntityWriter {
		void enter(Class<? extends Entity> entityClass);

		Class<? extends Entity> getEntityClass();

		default String getTransformedClassName() {
			return getEntityClass().getSimpleName() + "_";
		}
	}
}
