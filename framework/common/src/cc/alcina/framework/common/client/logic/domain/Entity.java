package cc.alcina.framework.common.client.logic.domain;

import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccAccess;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccAccess.MvccAccessType;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

/**
 * Base for classes which can be handled by the
 * {@link cc.alcina.framework.common.client.logic.domaintransform.TransformManager
 * TransformManager }. Note that the only id type supported is <code>long</code>
 */
@MappedSuperclass
@RegistryLocation(registryPoint = Entity.class, implementationType = ImplementationType.MULTIPLE)
public abstract class Entity<T extends Entity> extends BaseBindable
		implements HasVersionNumber, HasId {
	public static final String CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID = Entity.class
			+ ".CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID";

	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	// has @GwtTransient annotation because we don't want to send
	// server-generated local ids to the client - or vice-versa
	@GwtTransient
	volatile long localId;

	public void delete() {
		Domain.delete(domainIdentity());
	}

	@MvccAccess(type = MvccAccessType.RESOLVE_TO_DOMAIN_IDENTITY)
	public DomainSupport domain() {
		return new DomainSupport();
	}

	// rewritten by class transformer
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public T domainIdentity() {
		return (T) this;
	}

	@Override
	public boolean equals(Object obj) {
		return EntityHelper.equals(domainIdentity(), obj);
	}

	@Override
	@Display(name = "Id", orderingHint = 900, visible = @Permission(access = AccessLevel.ADMIN))
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public abstract long getId();

	@Display(name = "Local id")
	@PropertyPermissions(read = @Permission(access = AccessLevel.ROOT), write = @Permission(access = AccessLevel.ROOT))
	@Transient
	/**
	 * Used for object referencing within a client domain. Generated from a
	 * thread-safe increment counter (one counter per domain, not
	 * per-object-type). Can be 'packed' with the lower 31 bits of the
	 * clientInstance id (and negated) to make an effectively globally unique id
	 * (for the jvm lifetime) that can be used in the same set as the id field
	 * (per-class, db-generated) ids
	 */
	public long getLocalId() {
		return this.localId;
	}

	@Override
	@Version
	@Column(name = "OPTLOCK")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	@Display(name = "Version number", visible = @Permission(access = AccessLevel.ADMIN), orderingHint = 991)
	public int getVersionNumber() {
		return versionNumber;
	}

	/*
	 * Having a hash function which tracks an object as it graduates from
	 * local-id to db-id is key for not needing to switch objects after
	 * transaction commit - and transactional object identity in mvcc. Equality
	 * is an extension of this treatment of the hash
	 * 
	 * So:
	 * 
	 * * If an object has its local id non-zero, the hash is derived *only from
	 * the localid and class *.
	 * 
	 * * If an object has non-zero id, the hash call falls through to the domain
	 * and asks if any objects have had its id/class tuple applied to an object
	 * whose local id *was set in this vm* - i.e. was created in this vm, not a
	 * transform from another vm. If so, the hashcode of that originally local
	 * object is returned
	 * 
	 * * Equality: if id is non-zero for either object, equality uses class and
	 * id (since the id of the originally local object will be updated by
	 * mvcc/gwt transformmanager server response handling code). Otherwise use
	 * reference equality ('==') - objects with only localids are in isolated
	 * transactions and or cascades and the use cases for cloning and then using
	 * equality are...limited
	 * 
	 * * Note that all of the above avoids the use of clientinstanceid - which
	 * would be a way to more simply define the inequality of objects with the
	 * same localid (but different originating vms), but with the cost of more
	 * complicated transforms and transform persistence
	 * 
	 * This method will always return the same result irresepective of mvcc
	 * version, so is not rerouted
	 * 
	 */
	@Override
	public int hashCode() {
		if (hash == 0) {
			if (getId() == 0 && getLocalId() == 0) {
				if (LooseContext.is(
						CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID)) {
					hash = System.identityHashCode(domainIdentity());
					return hash;
				}
			}
			int classHash = provideEntityClass().getName().hashCode();
			if (getLocalId() != 0) {
				if (GWT.isScript()) {
					hash = LongWrapperHash.fastHash(getLocalId()) ^ classHash;
				} else {
					hash = ((int) getLocalId()) ^ classHash;
				}
			} else {
				if (getId() == 0) {
					// still 0
				} else {
					if (GWT.isScript()) {
						hash = LongWrapperHash.fastHash(getId()) ^ classHash;
					} else {
						hash = ((int) getId()) ^ classHash;
					}
					hash = TransformManager.replaceWithCreatedLocalObjectHash(
							domainIdentity(), hash);
				}
			}
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	public Class<? extends Entity> provideEntityClass() {
		return getClass();
	}

	public long provideIdOrLocalIdIfZero() {
		return getId() != 0 ? getId() : getLocalId();
	}

	public boolean provideIsLocal() {
		return getId() == 0 && getLocalId() != 0;
	}

	public boolean provideIsNonDomain() {
		return getId() == 0 && getLocalId() == 0;
	}

	public String provideStringId() {
		return getId() == 0 ? null : String.valueOf(getId());
	}

	public long provideTransactionalId() {
		if (getId() == 0) {
			long clientInstanceId = CommonUtils
					.lv(PermissionsManager.get().getClientInstanceId());
			return -((clientInstanceId << 32) + getLocalId());
		} else {
			return getId();
		}
	}

	public boolean provideWasPersisted() {
		return getId() != 0;
	}

	// no listeners - this should be invisible to transform listeners
	public void setLocalId(long localId) {
		if (this.localId != 0) {
			int debug = 3;
		}
		this.localId = localId;
	}

	@Override
	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}

	@Override
	/*
	 * the last line is to deal with a weird gwt/ff/webkit bug
	 */
	public String toString() {
		if (Reflections.classLookup() == null) {
			return new EntityLocator(domainIdentity()).toString();
		}
		if (!GwittirUtils.isIntrospectable(getClass())) {
			return super.toString();
		}
		String dn = Reflections.classLookup()
				.displayNameForObject(domainIdentity());
		dn = !CommonUtils.isNullOrEmpty(dn) ? dn : "[Object]";
		return dn.substring(0, dn.length());
	}

	public String toStringEntity() {
		return new EntityLocator(domainIdentity()).toString();
	}

	public T writeable() {
		return domainIdentity();
	}

	protected int _compareTo(Entity o) {
		o = Domain.resolve(o);
		String s1 = comparisonString();
		String s2 = o.comparisonString();
		if (s1 != null && s2 != null) {
			int i = s1.compareTo(s2);
			if (i != 0) {
				return i;
			}
		} else {
			if (s1 != null) {
				return 1;
			}
			if (s2 != null) {
				return -1;
			}
		}
		return CommonUtils.compareLongs(getId(), o.getId());
	}

	protected String comparisonString() {
		throw new RuntimeException(
				"no display name available, and using comparator");
	}

	public class DomainSupport {
		public <V extends Entity> void addToProperty(String propertyName, V v) {
			TransformManager.get().modifyCollectionProperty(Entity.this,
					propertyName, v, CollectionModificationType.ADD);
		}

		public T detachedToDomain() {
			return (T) Domain.detachedToDomain(Entity.this);
		}

		public boolean detachedToDomainHasDelta() {
			int before = TransformManager.get().getTransforms().size();
			detachedToDomain();
			return TransformManager.get().getTransforms().size() != before;
		}

		/*
		 * Basically server-side, disconnected (lock-safe) version from a
		 * DomainStore
		 */
		public T detachedVersion() {
			return (T) Domain.detachedVersion(Entity.this);
		}

		public void detachFromDomain() {
			TransformManager.get().deregisterDomainObject(Entity.this);
		}

		/*
		 * Basically server-side, connected version from a DomainStore
		 */
		public T domainVersion() {
			return (T) Domain.find(Entity.this);
		}

		/*
		 * iff Entity.this===domainVersion()
		 */
		public boolean isDomainVersion() {
			return Domain.isDomainVersion(Entity.this);
		}

		public boolean isRegistered() {
			return TransformManager.get().isRegistered(Entity.this);
		}

		public T register() {
			TransformManager.get().registerDomainObject(Entity.this);
			return (T) Entity.this;
		}

		public <V extends Entity> void removeFromProperty(String propertyName,
				V v) {
			TransformManager.get().modifyCollectionProperty(Entity.this,
					propertyName, v, CollectionModificationType.REMOVE);
		}

		/*
		 * server-side only
		 */
		public T transactionVersion() {
			return (T) Domain.transactionVersion(Entity.this);
		}
	}

	public static class EntityByIdFilter implements CollectionFilter<Entity> {
		private final boolean allowAllExceptId;

		private final long id;

		public EntityByIdFilter(long id, boolean allowAllExceptId) {
			this.id = id;
			this.allowAllExceptId = allowAllExceptId;
		}

		@Override
		public boolean allow(Entity o) {
			return o != null && (o.getId() == id ^ allowAllExceptId);
		}
	}

	public static class EntityComparator implements Comparator<Entity> {
		public static final EntityComparator INSTANCE = new EntityComparator();

		public static final Comparator<Entity> REVERSED_INSTANCE = new EntityComparator()
				.reversed();

		@Override
		public int compare(Entity o1, Entity o2) {
			return EntityHelper.compare(o1, o2);
		}
	}

	public static class EntityComparatorLocalsHigh
			implements Comparator<Entity> {
		public static final EntityComparatorLocalsHigh INSTANCE = new EntityComparatorLocalsHigh();

		@Override
		public int compare(Entity o1, Entity o2) {
			return EntityHelper.compareLocalsHigh(o1, o2);
		}
	}

	public static class EntityComparatorPreferLocals
			implements Comparator<Entity> {
		@Override
		public int compare(Entity o1, Entity o2) {
			int i = o1.getClass().getName().compareTo(o2.getClass().getName());
			if (i != 0) {
				return i;
			}
			i = CommonUtils.compareLongs(o1.getLocalId(), o2.getLocalId());
			if (i != 0) {
				return i;
			}
			i = CommonUtils.compareLongs(o1.getId(), o2.getId());
			if (i != 0) {
				return i;
			}
			return CommonUtils.compareInts(o1.hashCode(), o2.hashCode());
		}
	}

	public static class EntityNoLocalComparator implements Comparator<Entity> {
		public static final EntityNoLocalComparator INSTANCE = new EntityNoLocalComparator();

		@Override
		public int compare(Entity o1, Entity o2) {
			return EntityHelper.compareNoLocals(o1, o2);
		}
	}
}
