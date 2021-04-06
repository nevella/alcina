package cc.alcina.framework.common.client.logic.domain;

import java.util.Optional;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = PersistentImpl.class, targetClass = SystemProperty.class)
@DomainTransformPropagation(PropagationType.PERSISTENT)
/*
 * 
 * *DO * reference via foreign key constraints from large tables (since
 * intention is that table rows be non-removable).
 */
public abstract class SystemProperty<T extends SystemProperty>
		extends VersionableEntity<T> {
	public static final transient String CONTEXT_NO_COMMIT = SystemProperty.class
			.getName() + ".CONTEXT_NO_COMMIT";

	public static <P extends SystemProperty> P byId(long id) {
		return Domain.find(implementation(), id);
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <P extends SystemProperty> Optional<P> byKey(String key) {
		return (Optional<P>) Domain.query(implementation()).filter("key", key)
				.optional();
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <P extends SystemProperty<?>> P ensure(String key) {
		Optional<P> existing = byKey(key);
		return existing.orElseGet(() -> {
			P created = Domain.create(implementation());
			created.setKey(key);
			return created;
		});
	}

	private static <P extends SystemProperty> Class<P> implementation() {
		return (Class<P>) PersistentImpl
				.getImplementation(SystemProperty.class);
	}

	private String category;

	private String key;

	private String value;

	@Lob
	@Transient
	public String getCategory() {
		return this.category;
	}

	@Lob
	@Transient
	public String getKey() {
		return this.key;
	}

	@Lob
	@Transient
	public String getValue() {
		return this.value;
	}

	public void serializeObject(Object object) {
		setValue(TransformManager.serialize(object));
		setCategory(object.getClass().getName());
	}

	public void setCategory(String category) {
		String old_category = this.category;
		this.category = category;
		propertyChangeSupport().firePropertyChange("category", old_category,
				category);
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setKey(String key) {
		String old_key = this.key;
		this.key = key;
		propertyChangeSupport().firePropertyChange("key", old_key, key);
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}
}
