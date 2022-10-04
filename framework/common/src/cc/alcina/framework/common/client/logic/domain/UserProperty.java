package cc.alcina.framework.common.client.logic.domain;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasIUser;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER), delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPropagation(PropagationType.PERSISTENT)
@Registration({ PersistentImpl.class, UserProperty.class })
public abstract class UserProperty<T extends UserProperty>
		extends VersionableEntity<T> implements HasIUser, HasOwner {
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
		return (V) TransformManager.Serializer.get().deserialize(getValue(),
				clazz);
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

	@Override
	@Transient
	public IUser getOwner() {
		return getUser();
	}

	@Transient
	public UserPropertyPersistable.Support getUserPropertySupport() {
		return this.userPropertySupport;
	}

	@Lob
	@Transient
	public String getValue() {
		return this.value;
	}

	public UserPropertyPersistable providePersistable() {
		return this.userPropertySupport == null ? null
				: this.userPropertySupport.getPersistable();
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
		super.setId(id);
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
				TransformManager.ignoreChanges(() -> new CloneHelper()
						.copyBeanProperties(deserialize(),
								userPropertySupport.getPersistable(),
								DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING));
			}
		}
	}
}
