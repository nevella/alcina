package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.SQLException;

import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public abstract class PropertyStoreProjection<T extends HasIdAndLocalId>
		extends BaseProjection<T> {
	public PropertyStoreProjection(PropertyStore propertyStore,
			Class initialType, Class... secondaryTypes) {
		super(initialType, secondaryTypes);
		propertyStore.addProjection(this);
	}

	public void index(T t, boolean add) {
		if (add) {
			insert(t);
		} else {
			remove(t);
		}
	}

	public abstract void initPropertyDescriptors();

	public abstract void insert(Object[] row, long id) throws SQLException;
}
