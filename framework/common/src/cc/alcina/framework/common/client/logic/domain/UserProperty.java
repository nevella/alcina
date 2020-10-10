package cc.alcina.framework.common.client.logic.domain;

import java.util.Base64;
import java.util.Optional;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPersistable
@RegistryLocation(registryPoint = AlcinaPersistentEntityImpl.class, targetClass = UserProperty.class)
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
/*
 * Similar to the (jvm-only) KeyValuePersistentBase
 */
public abstract class UserProperty<T extends UserProperty> extends Entity<T>
		implements HasIUser {
	public static final transient String CONTEXT_NO_COMMIT = UserProperty.class
			.getName() + ".CONTEXT_NO_COMMIT";

	public static <P extends UserProperty> P byId(long id) {
		return Domain.find(implementation(), id);
	}

	public static <P extends UserProperty> Optional<P> byUserClass(IUser user,
			Class<? extends UserPropertyPersistable> clazz) {
		return byUserKey(user, clazz.getName());
	}

	public static <P extends UserProperty> Optional<P> byUserKey(IUser user,
			String key) {
		return (Optional<P>) Domain.query(implementation()).filter("user", user)
				.filter("key", key).optional();
	}

	private static Class<? extends UserProperty> implementation() {
		return AlcinaPersistentEntityImpl.getImplementation(UserProperty.class);
	}

	private String key;

	private String value;

	public <UPP extends UserPropertyPersistable> UPP deserialize() {
		return TransformManager.resolveMaybeDeserialize(null, getValue(), null);
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
		byte[] bytes = KryoUtils.serializeToByteArray(object);
		byte[] zipped = ResourceUtilities.gzipBytes(bytes);
		String serializeToBase64 = Base64.getEncoder().encodeToString(zipped);
		setValue(serializeToBase64);
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
