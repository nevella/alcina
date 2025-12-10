package cc.alcina.framework.common.client.logic.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PermissionRule;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

/**
 * <p>
 * Base for classes which can be handled by the
 * {@link cc.alcina.framework.common.client.logic.domaintransform.TransformManager
 * TransformManager }. Note that the only id type supported is <code>long</code>
 * <p>
 * Gotchas
 * <ul>
 * <li>If an entity has a linked Set<? extends Entity> property, which is
 * default null, it must be populated on get for transofrm association
 * propagation - e.g:
 * 
 * <pre>
 * <code>
 * private Set<DodgyCitation> dodgyCitations;
 * 
 * ...
 * 
 * public Set<DodgyCitation> getDodgyCitations() {
		if (this.dodgyCitations == null) {
			dodgyCitations = new LiSet<>();
		}
		return dodgyCitations;
	}
 * 
 * </code>
 * </pre>
 * </ul>
 */
@MappedSuperclass
@NonClientRegistryPointType
@DomainTransformPropagation(PropagationType.PERSISTENT)
// ensure { "id", "localId" } are before other properties (because needed for
// the hash in recursive deserialization)
@JsonPropertyOrder(value = { "id", "localId" }, alphabetic = true)
@Registration(Entity.class)
public abstract class Entity<T extends Entity> extends Bindable
		implements HasVersionNumber, HasId, HasEntity {
	public static final transient String CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID = Entity.class
			+ ".CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID";

	public static final transient String CONTEXT_ALLOW_ID_MODIFICATION = Entity.class
			+ ".CONTEXT_ALLOW_ID_MODIFICATION";

	public static transient EntityClassResolver classResolver = new EntityClassResolver();

	protected volatile long id = 0;

	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	// has @GwtTransient annotation because we don't want to send
	// server-generated local ids to the client - or vice-versa
	@GwtTransient
	volatile long localId;

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

	// rewritten by class transformer
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public Class<? extends Entity> entityClass() {
		return classResolver.entityClass(this);
	}

	@Override
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public boolean equals(Object other) {
		/*
		 * Optimise for mvcc equality
		 */
		if (other == this) {
			return true;
		}
		if (other instanceof Entity) {
			Entity otherEntity = (Entity) other;
			if (otherEntity.getId() != getId()) {
				return false;
			}
			Entity domainIdentity = domainIdentity();
			Entity otherIdentity = otherEntity.domainIdentity();
			if (domainIdentity == otherIdentity) {
				return true;
			} else {
				return EntityHelper.equals(domainIdentity, otherIdentity);
			}
		} else {
			return false;
		}
	}

	@Override
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public abstract long getId();

	@Display
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.ROOT),
		write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public /**
			 * Used for object referencing within a client domain. Generated
			 * from a thread-safe increment counter (one counter per domain, not
			 * per-object-type).
			 *
			 * (Unused but viable) Can be 'packed' with the lower 31 bits of the
			 * clientInstance id (and negated) to make an effectively globally
			 * unique id (for the given domainstore/db) that can be used in the
			 * same set as the id field (per-class, db-generated) ids -
			 * assumption being that at most 2^32 creations per client instance,
			 * 2^31 client instances
			 */
	long getLocalId() {
		return this.localId;
	}

	@Override
	@Version
	@Column(name = "OPTLOCK")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.ROOT))
	public // AccessLevel.ADMIN), orderingHint = 991)
	int getVersionNumber() {
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
			int classHash = entityClass().getName().hashCode();
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

	@Override
	public <E extends Entity> E provideEntity() {
		return (E) domainIdentity();
	}

	@Override
	public void setId(long id) {
		if (this.id != 0 && this.id != id) {
			// this is allowable in non-mvcc systems (e.g. for changing
			// untrusted client ids)
			if (!LooseContext.is(CONTEXT_ALLOW_ID_MODIFICATION)) {
				throw new IllegalArgumentException(Ax.format(
						"Changing persistent id: %s => %s", this.id, id));
			}
		}
		this.id = id;
	}

	// not a propertychangeevent source - this should be invisible to transform
	// listeners
	public void setLocalId(long localId) {
		this.localId = localId;
	}

	@Override
	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}

	public final EntityLocator toLocator() {
		return EntityLocator.instanceLocator(domainIdentity());
	}

	@Override
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public String toString() {
		if (this instanceof HasDisplayName) {
			try {
				return ((HasDisplayName) this).displayName();
			} catch (Exception e) {
				return CommonUtils.toSimpleExceptionMessage(e) + " - "
						+ toLocator().toString();
			}
		}
		return toLocator().toString();
	}

	public final String toStringEntity() {
		try {
			return toLocator().toString();
		} catch (Exception e) {
			return Ax.format("Unable to return locator - class %s - id %s",
					getClass().getSimpleName(), id);
		}
	}

	public interface EntityBrowser {
		public EntityBrowser withEntity(Entity entity);

		void browse();

		String remoteUrl();
	}

	public class DomainSupport {
		public <V extends Entity> void addToProperty(String propertyName, V v) {
			TransformManager.get().modifyCollectionProperty(Entity.this,
					propertyName, v, CollectionModificationType.ADD);
		}

		public <V extends Entity> V checkPermission(PermissionRule rule)
				throws PermissionsException {
			return (V) rule.checkPermission(Entity.this);
		}

		/**
		 * Copies properties from 'this' (detached/projected copy of the entity)
		 * to the domain graph entity
		 */
		public T detachedToDomain() {
			return (T) Domain.detachedToDomain(Entity.this);
		}

		/**
		 * A disconnected projection of an entity. Useful for things like
		 * speculative writes and serialization. Normally generated using
		 * projection
		 *
		 */
		public T detachedVersion() {
			return (T) Domain.detachedVersion(Entity.this);
		}

		/**
		 * Remove transform manager listeners - this is appropriate when using a
		 * 'faux' object (negative id), or when simply muting transforms for a
		 * given object/mvcc objectversion
		 */
		public void detachFromDomain() {
			TransformManager.get().deregisterDomainObject(Entity.this);
		}

		/**
		 *
		 * Returns the transform graph version of the entity - does *not*
		 * populate lazy properties
		 *
		 */
		public T domainVersion() {
			return (T) Domain.find(Entity.this);
		}

		/**
		 * Populate the lazy properties of this entity (current tx only). See
		 * {@link Domain#ensurePopulated(Entity)}
		 * 
		 */
		public void ensurePopulated() {
			Domain.ensurePopulated(Entity.this);
		}

		public long getIdOrLocalIdIfZero() {
			return getId() != 0 ? getId() : getLocalId();
		}

		public boolean isLocal() {
			return getId() == 0 && getLocalId() != 0;
		}

		public boolean isNonDomain() {
			return !Domain.isDomainVersion(Entity.this);
		}

		public boolean isZeroIds() {
			return getId() == 0 && getLocalId() == 0;
		}

		public void log(String... paths) {
			Domain.logTree(Entity.this, paths);
		}

		public EntityBrowser browser() {
			return Registry.impl(EntityBrowser.class).withEntity(Entity.this);
		}

		public boolean notRemoved() {
			return Domain.notRemoved(Entity.this);
		}

		public void persistSerializables() {
			TransformManager.get().persistSerializables(Entity.this);
		}

		/*
		 * Attach this object to the Domain
		 */
		public T register() {
			TransformManager.get().registerDomainObject(Entity.this);
			return (T) Entity.this;
		}

		public <V extends Entity> void removeFromProperty(String propertyName,
				V v) {
			TransformManager.get().modifyCollectionProperty(Entity.this,
					propertyName, v, CollectionModificationType.REMOVE);
		}

		public String stringId() {
			return getId() == 0 ? null : String.valueOf(getId());
		}

		public boolean wasPersisted() {
			return getId() != 0;
		}

		public boolean wasRemoved() {
			return Domain.wasRemoved(Entity.this);
		}
	}

	public static class EntityByIdFilter implements Predicate<Entity> {
		private final boolean allowAllExceptId;

		private final long id;

		public EntityByIdFilter(long id, boolean allowAllExceptId) {
			this.id = id;
			this.allowAllExceptId = allowAllExceptId;
		}

		@Override
		public boolean test(Entity o) {
			return o != null && (o.getId() == id ^ allowAllExceptId);
		}
	}

	public static class EntityClassResolver {
		public Class<? extends Entity> entityClass(Entity e) {
			return e.getClass();
		}
	}

	public static class LastModifiedComparator
			implements Comparator<VersionableEntity> {
		public static final LastModifiedComparator INSTANCE = new LastModifiedComparator();

		public static final Comparator<VersionableEntity> REVERSED_INSTANCE = new LastModifiedComparator()
				.reversed();

		@Override
		public int compare(VersionableEntity o1, VersionableEntity o2) {
			return CommonUtils.compareDates(o1.getLastModificationDate(),
					o2.getLastModificationDate());
		}
	}

	public static class EntityComparator implements Comparator<Entity> {
		public static final EntityComparator INSTANCE = new EntityComparator();

		public static final EntityComparatorLocalsHigh LOCALS_HIGH = new EntityComparatorLocalsHigh();

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

	public interface Ownership {
		@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
		public static Stream<Property> getOwnerReflectors(Class<?> beanClass) {
			return Reflections.at(beanClass).properties().stream()
					.filter(pr -> pr.has(DomainProperty.class))
					.filter(pr -> pr.annotation(DomainProperty.class).owner());
		}

		public static List<Entity> getOwningEntities(Entity entity) {
			Entity cursor = entity;
			List<Entity> result = new ArrayList<>();
			while (true) {
				Entity f_cursor = cursor;
				List<Entity> values = (List) getOwnerReflectors(
						cursor.entityClass()).map(pr -> pr.get(f_cursor))
								.collect(Collectors.toList());
				if (values.size() > 1) {
					if (entity instanceof HasLogicalParent) {
						Entity logicalParent = ((HasLogicalParent) entity)
								.provideLogicalParent();
						values = Collections.singletonList(logicalParent);
					}
				}
				if (values.size() == 1 && values.get(0) != null) {
					cursor = values.get(0);
					result.add(0, cursor);
				} else {
					return result;
				}
			}
		}
	}
}
