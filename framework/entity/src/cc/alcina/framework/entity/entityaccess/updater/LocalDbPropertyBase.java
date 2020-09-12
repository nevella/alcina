package cc.alcina.framework.entity.entityaccess.updater;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPersistable
public abstract class LocalDbPropertyBase extends Entity {
	public static final transient String KEY_FIELD_NAME = "propertyKey";

	public static final transient String VALUE_FIELD_NAME = "propertyValue";

	public static final transient String DB_UPDATE_VERSION = "DB_UPDATE_VERSION";

	public static final transient String DB_ENTITIES_VERSION = "DB_ENTITIES_VERSION";

	public static final transient String SERLVET_UPDATE_VERSION = "SERVLET_LAYER_UPDATE_VERSION";

	public static String getLocalDbProperty(String key) {
		return getOrSetLocalDbProperty(key, null, true);
	}

	// FIXME - mvcc.4 - this should always be in the domainstore
	public static LocalDbPropertyBase getLocalDbPropertyObject(String key) {
		CommonPersistenceLocal cpl = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence();
		Class<? extends LocalDbPropertyBase> implClass = AlcinaPersistentEntityImpl
				.getImplementation(LocalDbPropertyBase.class);
		LocalDbPropertyBase dbProperty = cpl.getItemByKeyValue(implClass,
				KEY_FIELD_NAME, key, true, null, false);
		return dbProperty;
	}

	public static String getOrSetLocalDbProperty(String key, String value,
			boolean get) {
		LocalDbPropertyBase dbPropertyObject = getLocalDbPropertyObject(key);
		if (get) {
			return dbPropertyObject.getPropertyValue();
		} else {
			try {
				CommonPersistenceLocal cpl = Registry
						.impl(CommonPersistenceProvider.class)
						.getCommonPersistence();
				Class<? extends LocalDbPropertyBase> implClass = AlcinaPersistentEntityImpl
						.getImplementation(LocalDbPropertyBase.class);
				cpl.setField(implClass, dbPropertyObject.getId(),
						VALUE_FIELD_NAME, value);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return null;
		}
	}

	public static String setLocalDbProperty(String key, String value) {
		return getOrSetLocalDbProperty(key, value, false);
	}

	private String propertyKey;

	protected String propertyValue;

	public String getPropertyKey() {
		return this.propertyKey;
	}

	@Lob
	@Transient
	public abstract String getPropertyValue();

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setPropertyKey(String propertyKey) {
		String old_propertyKey = this.propertyKey;
		this.propertyKey = propertyKey;
		propertyChangeSupport().firePropertyChange("propertyKey",
				old_propertyKey, propertyKey);
	}

	public void setPropertyValue(String propertyValue) {
		String old_propertyValue = this.propertyValue;
		this.propertyValue = propertyValue;
		propertyChangeSupport().firePropertyChange("propertyValue",
				old_propertyValue, propertyValue);
	}
}
