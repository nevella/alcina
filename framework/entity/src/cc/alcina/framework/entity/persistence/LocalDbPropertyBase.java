package cc.alcina.framework.entity.persistence;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@MappedSuperclass
/*
 * Legacy - use KeyValuePersistent by preference
 * 
 * Retains non-transform persistence to support db updates pre domainstore
 * 
 * Note that servlet_layer_update *could* use a transform-based store, but
 * doesn't need to (if servlet updaters are idempotent)
 * 
 * FIXME - apdm - revert to non-domain, use only for pre-store
 * (db_update_version) kvs
 */
@Registration({ PersistentImpl.class, LocalDbPropertyBase.class })
public abstract class LocalDbPropertyBase extends Entity {
	public static final transient String KEY_FIELD_NAME = "propertyKey";

	public static final transient String VALUE_FIELD_NAME = "propertyValue";

	public static final transient String DB_UPDATE_VERSION = "DB_UPDATE_VERSION";

	public static final transient String SERLVET_UPDATE_VERSION = "SERVLET_LAYER_UPDATE_VERSION";

	public static String getLocalDbProperty(String key) {
		return getOrSetLocalDbProperty(key, null, true);
	}

	public static LocalDbPropertyBase getLocalDbPropertyObject(String key) {
		CommonPersistenceLocal cpl = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence();
		Class<? extends LocalDbPropertyBase> implClass = PersistentImpl
				.getImplementation(LocalDbPropertyBase.class);
		LocalDbPropertyBase dbProperty = cpl.ensure(implClass, KEY_FIELD_NAME,
				key);
		return dbProperty;
	}

	public static String getOrSetLocalDbProperty(String key, String value,
			boolean get) {
		return getOrSetLocalDbPropertyPreDomainStore(key, value, get);
	}

	public static String setLocalDbProperty(String key, String value) {
		return getOrSetLocalDbProperty(key, value, false);
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
				Class<? extends LocalDbPropertyBase> implClass = PersistentImpl
						.getImplementation(LocalDbPropertyBase.class);
				cpl.setField(implClass, dbPropertyObject.getId(),
						VALUE_FIELD_NAME, value);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return null;
		}
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
