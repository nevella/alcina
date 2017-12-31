package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import cc.alcina.framework.common.client.cache.BaseProjection;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public abstract class PropertyStoreProjection<T extends HasIdAndLocalId>
		extends BaseProjection<T> {
	public PropertyStoreProjection(PropertyStore propertyStore) {
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

	public abstract void insert(ResultSet rs, long id) throws SQLException;
}
