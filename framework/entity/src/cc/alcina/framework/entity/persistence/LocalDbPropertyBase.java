package cc.alcina.framework.entity.persistence;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPersistable
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
/*
 * Legacy - use KeyValuePersistent by preference
 * 
 * Retains non-transform persistence to support db updates pre domainstore
 * 
 * FIXME - mvcc.4 - revert to non-domain, use only for pre-store
 * (db_update_version) kvs
 */
public abstract class LocalDbPropertyBase extends Entity {
	public static final transient String KEY_FIELD_NAME = "propertyKey";

	public static final transient String VALUE_FIELD_NAME = "propertyValue";

	public static final transient String DB_UPDATE_VERSION = "DB_UPDATE_VERSION";

	public static final transient String DB_ENTITIES_VERSION = "DB_ENTITIES_VERSION";

	public static final transient String SERLVET_UPDATE_VERSION = "SERVLET_LAYER_UPDATE_VERSION";

	public static final transient String CONTEXT_USE_DB_PERSISTENCE = LocalDbPropertyBase.class
			.getName() + ".CONTEXT_USE_DB_PERSISTENCE";

	public static String getLocalDbProperty(String key) {
		return getOrSetLocalDbProperty(key, null, true);
	}

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
		if (DomainStore.stores().hasInitialisedDatabaseStore()
				&& !LooseContext.is(CONTEXT_USE_DB_PERSISTENCE)) {
			return getOrSetLocalDbPropertyDomainStore(key, value, get);
		} else {
			return getOrSetLocalDbPropertyPreDomainStore(key, value, get);
		}
	}

	public static String setLocalDbProperty(String key, String value) {
		return getOrSetLocalDbProperty(key, value, false);
	}

	private static String getOrSetLocalDbPropertyDomainStore(String key,
			String value, boolean get) {
		if (get) {
			return Domain.optionalByProperty(impl(), KEY_FIELD_NAME, key)
					.map(LocalDbPropertyBase::getPropertyValue).orElse(null);
		} else {
			Domain.ensure(impl(), KEY_FIELD_NAME, key).setPropertyValue(value);
			Transaction.commit();
			return null;
		}
	}

	private static String getOrSetLocalDbPropertyPreDomainStore(String key,
			String value, boolean get) {
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

	private static Class<? extends LocalDbPropertyBase> impl() {
		return AlcinaPersistentEntityImpl
				.getImplementation(LocalDbPropertyBase.class);
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
