package cc.alcina.framework.common.client.logic.domain;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER), delete = @Permission(access = AccessLevel.ROOT))
@Bean
@RegistryLocation(registryPoint = PersistentImpl.class, targetClass = UserProperty.class)
@DomainTransformPropagation(PropagationType.PERSISTENT)
/*
 * Similar to the (jvm-only) KeyValuePersistentBase. user/key is unique,
 * user/category not. If used as a java object persistence container,
 * 
 * *DO NOT* reference via foreign key constraints from large tables (since
 * intention is that table rows be removable, and that would require huge
 * indicies). In general (say for type serialization signatures) use the
 * UserProperty.key value rather than an id ref/foreign key. This means a
 * possible breach of "referential integrity", (quotes intended) - this is once
 * place where the tradeoff comes down on the side of avoiding the constraint.
 * 
 * Editing notes - the tm-registered object should be the userProperty -
 * UserPropertyPersistable.getUserPropertySupport().getProperty(). The
 * UserPropertyPersistable object will not be transaction-aware (it's
 * effectively a snapshot - writeable only within the snapshot-creation tx).
 * TODO - check this with a precondition server side
 */
public abstract class UserProperty<T extends UserProperty>
		extends VersionableEntity<T> implements HasIUser {
	public static final transient String CONTEXT_NO_COMMIT = UserProperty.class
			.getName() + ".CONTEXT_NO_COMMIT";

	public static <P extends UserProperty<?>> P byId(long id) {
		return Domain.find(implementation(), id);
	}

	public static <P extends UserProperty<?>> Optional<P> byKey(String key) {
		return byUserKey(PermissionsManager.get().getUser(), key);
	}

	public static <P extends UserProperty<?>> Optional<P> byUserClass(
			IUser user, Class<? extends UserPropertyPersistable> clazz) {
		return byUserKey(user, clazz.getName());
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <P extends UserProperty<?>> Optional<P> byUserKey(IUser user,
			String key) {
		return (Optional<P>) Domain.query(implementation()).filter("user", user)
				.filter("key", key).optional();
	}

	public static <T, P extends UserProperty<?>> P ensure(Class<T> clazz) {
		UserProperty<?> property = ensure(clazz.getName());
		if (property.getValue() == null) {
			property.serializeObject(Reflections.newInstance(clazz));
		}
		return (P) property;
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <P extends UserProperty<?>> P ensure(IUser user, String key) {
		Optional<P> existing = byUserKey(user, key);
		return existing.orElseGet(() -> {
			P created = Domain.create(implementation());
			created.setUser(user);
			created.setKey(key);
			return created;
		});
	}

	public static <P extends UserProperty<?>> P ensure(String key) {
		return ensure(PermissionsManager.get().getUser(), key);
	}

	private static <P extends UserProperty<?>> Class<P> implementation() {
		return (Class<P>) PersistentImpl.getImplementation(UserProperty.class);
	}

	private String category;

	private String key;

	private String value;

	private UserPropertyPersistable.Support userPropertySupport;

	public UserProperty copy() {
		UserProperty copy = new CloneHelper()
				.shallowishBeanClone(domainIdentity());
		copy.userPropertySupport = new UserPropertyPersistable.Support(copy);
		// no need to set copy.userPropertySupport.persistable, since it'll be
		// generated on demand
		return copy;
	}

	public <V> V deserialize() {
		Class clazz = null;
		if (category != null) {
			try {
				clazz = Reflections.forName(category);
			} catch (Exception e) {
			}
		}
		return (V) TransformManager.resolveMaybeDeserialize(null, getValue(),
				null, clazz);
	}

	public synchronized UserPropertyPersistable.Support
			ensureUserPropertySupport() {
		if (userPropertySupport == null) {
			userPropertySupport = new UserPropertyPersistable.Support(
					domainIdentity());
		}
		return this.userPropertySupport;
	}

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
		setCategory(object.getClass().getName());
		setValue(TransformManager.serialize(object));
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
		if (!Objects.equals(old_value, value) && Ax.notBlank(value)) {
			if (userPropertySupport != null
					&& userPropertySupport.getPersistable() != null) {
				if (userPropertySupport != null
						&& userPropertySupport.getPersistable() != null) {
					new CloneHelper().copyBeanProperties(deserialize(),
							userPropertySupport.getPersistable(),
							DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING);
				}
			}
		}
	}
}
