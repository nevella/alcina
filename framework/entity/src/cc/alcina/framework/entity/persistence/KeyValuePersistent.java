package cc.alcina.framework.entity.persistence;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.domain.LazyPropertyLoadTask;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

@MappedSuperclass
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ROOT))
@DomainTransformPersistable
@RegistryLocation(registryPoint = PersistentImpl.class, targetClass = KeyValuePersistent.class)
public abstract class KeyValuePersistent<T extends KeyValuePersistent>
		extends VersionableEntity<T> {
	public static final transient String CONTEXT_NO_COMMIT = KeyValuePersistent.class
			.getName() + ".CONTEXT_NO_COMMIT";

	public static transient Function<String, String> keyMapper = Function
			.identity();

	public static <KVP extends KeyValuePersistent> KVP byId(long id) {
		return Domain.find(implementation(), id);
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <KVP extends KeyValuePersistent> Optional<KVP>
			byKey(String key) {
		return byKey(key, true);
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <KVP extends KeyValuePersistent> Optional<KVP>
			byKey(String key, boolean populate) {
		DomainQuery<? extends KeyValuePersistent> query = Domain
				.query(implementation());
		if (populate) {
			query.contextTrue(
					LazyPropertyLoadTask.CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES);
		}
		return (Optional<KVP>) query.filter("key", keyMapper.apply(key))
				.optional();
	}

	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public static <KVP extends KeyValuePersistent> List<KVP>
			byParentKey(String parentKey) {
		return (List<KVP>) Domain.query(implementation()).contextTrue(
				LazyPropertyLoadTask.CONTEXT_POPULATE_STREAM_ELEMENT_LAZY_PROPERTIES)
				.filter("parentKey", keyMapper.apply(parentKey)).list();
	}

	public static String getClassKey(Class clazz, String key) {
		return Ax.format("%s/%s", clazz.getName(), key);
	}

	public static void persist(String key, String value) {
		KeyValuePersistent writeable = (KeyValuePersistent) Domain
				.ensure(implementation(), "key", keyMapper.apply(key));
		writeable.setParentKey(SEUtilities.getParentPath(key));
		writeable.setValue(value);
		persist();
	}

	public static void persistObject(String key, Object value) {
		persist(key, KeyValuePersistent.toSerializableForm(value));
	}

	public static void remove(String key) {
		Optional<KeyValuePersistent> persistent = byKey(key);
		if (persistent.isPresent()) {
			persistent.get().delete();
			persist();
		}
	}

	public static String toSerializableForm(Object object) {
		byte[] bytes = KryoUtils.serializeToByteArray(object);
		byte[] zipped = ResourceUtilities.gzipBytes(bytes);
		return Base64.getEncoder().encodeToString(zipped);
	}

	private static Class<? extends KeyValuePersistent> implementation() {
		return PersistentImpl.getImplementation(KeyValuePersistent.class);
	}

	private static void persist() {
		if (!LooseContext.is(CONTEXT_NO_COMMIT)) {
			Transaction.commit();
		}
	}

	private String key;

	private String value;

	private String parentKey;

	public <V> V deserializeObject(Class<V> knownType) {
		byte[] zipped = Base64.getDecoder().decode(getValue());
		byte[] bytes = ResourceUtilities.gunzipBytes(zipped);
		return KryoUtils.deserializeFromByteArray(bytes, knownType);
	}

	@Transient
	public byte[] getBytes() {
		return Base64.getDecoder().decode(getValue());
	}

	@Lob
	@Transient
	public String getKey() {
		return this.key;
	}

	@Lob
	@Transient
	public String getParentKey() {
		return this.parentKey;
	}

	@Lob
	@Transient
	public String getValue() {
		return this.value;
	}

	public void setBytes(byte[] bytes) {
		setValue(Base64.getEncoder().encodeToString(bytes));
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

	public void setParentKey(String parentKey) {
		String old_parentKey = this.parentKey;
		this.parentKey = parentKey;
		propertyChangeSupport().firePropertyChange("parentKey", old_parentKey,
				parentKey);
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}
}
