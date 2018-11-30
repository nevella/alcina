package cc.alcina.framework.common.client.csobjects;

import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LongWrapperHash;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

@MappedSuperclass
@RegistryLocation(registryPoint = AbstractDomainBase.class, implementationType = ImplementationType.MULTIPLE)
public abstract class AbstractDomainBase<T extends AbstractDomainBase>
		extends BaseBindable implements HasIdAndLocalId, HasVersionNumber {
	static final transient long serialVersionUID = 1L;

	public static final String CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID = AbstractDomainBase.class
			+ ".CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID";

	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	@GwtTransient
	long localId;

	@Override
	public DomainSupport domain() {
		return new DomainSupport();
	}

	@Override
	public boolean equals(Object obj) {
		return HiliHelper.equals(this, obj);
	}

	@Override
	@Display(name = "Id", orderingHint = 900, visible = @Permission(access = AccessLevel.ADMIN))
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public abstract long getId();

	@Override
	@Display(name = "Local id")
	@PropertyPermissions(read = @Permission(access = AccessLevel.ROOT), write = @Permission(access = AccessLevel.ROOT))
	@Transient
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

	@Override
	public int hashCode() {
		if (hash == 0) {
			if (getId() == 0 && getLocalId() == 0) {
				if (LooseContext.is(
						CONTEXT_USE_SYSTEM_HASH_CODE_IF_ZERO_ID_AND_LOCAL_ID)) {
					hash = System.identityHashCode(this);
					return hash;
				}
			}
			if (GWT.isScript()) {
				hash = LongWrapperHash.fastHash(getId())
						^ LongWrapperHash.fastHash(getLocalId())
						^ getClass().getName().hashCode();
			} else {
				hash = Long.valueOf(getId()).hashCode()
						^ Long.valueOf(getLocalId()).hashCode()
						^ getClass().getName().hashCode();
			}
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	public long provideIdOrLocalIdIfZero() {
		return getId() != 0 ? getId() : getLocalId();
	}

	public boolean provideIsLocal() {
		return getId() == 0 && getLocalId() != 0;
	}

	/**
	 * used - and why? Because an object we map will _always_ have either a
	 * local or persistent id - which means (for the lifetime of the
	 * client/webapp) - that the hash code will be unique. OK - but if we get
	 * the conceptually same object from the server - later - with a db id, and
	 * we check if a tm collection contains that object, it'll say "no" sad.
	 * probably some concept of "remapping listener" wouldn't be bad in the tm
	 * just in case of user sets/maps which need remapping
	 *
	 * hmm - wait - if we remap, we're making all sorts of problems for anything
	 * in a set better to not - and use a set implementation in the tm which
	 * maybe handles this sort of thing
	 *
	 * in fact...hmmm - used to be gethash/sethash, but _any way_ is really
	 * wrong objects from db are different to client-created objects - use one
	 * of the getobject(class,id,localid) methods if you want to look up
	 *
	 */
	// public void clearHash() {
	// hash = 0;
	// }
	// no listeners - this should be invisible to transform listeners
	@Override
	public void setLocalId(long localId) {
		this.localId = localId;
	}

	@Override
	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}

	public boolean superEquals(Object obj) {
		return super.equals(obj);
	}

	@Override
	/*
	 * the last line is to deal with a weird gwt/ff/webkit bug
	 */
	public String toString() {
		if (Reflections.classLookup() == null) {
			return new HiliLocator(this).toString();
		}
		if (!GwittirUtils.isIntrospectable(getClass())) {
			return super.toString();
		}
		String dn = Reflections.classLookup().displayNameForObject(this);
		dn = !CommonUtils.isNullOrEmpty(dn) ? dn : "[Object]";
		return dn.substring(0, dn.length());
	}

	public T writeable() {
		return (T) Domain.writeable(this);
	}

	protected int _compareTo(AbstractDomainBase o) {
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
		public <V extends HasIdAndLocalId> void addToProperty(V v,
				String propertyName) {
			TransformManager.get().modifyCollectionProperty(
					AbstractDomainBase.this, propertyName, v,
					CollectionModificationType.ADD);
		}

		public T createOrReturnWriteable() {
			HasEquivalence equivalent = HasEquivalenceHelper.getEquivalent(
					(Collection) Domain
							.values(AbstractDomainBase.this.getClass()),
					(HasEquivalence) AbstractDomainBase.this);
			if (equivalent != null) {
				return (T) equivalent;
			} else {
				return writeable();
			}
		}

		public T detachedToDomain() {
			return (T) Domain.detachedToDomain(AbstractDomainBase.this);
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
			return (T) Domain.detachedVersion(AbstractDomainBase.this);
		}

		public void detachFromDomain() {
			TransformManager.get()
					.deregisterDomainObject(AbstractDomainBase.this);
		}

		/*
		 * Basically server-side, connected version from a DomainStore
		 */
		public T domainVersion() {
			return (T) Domain.find(AbstractDomainBase.this);
		}

		public T domainVersionIfPersisted() {
			if (provideWasPersisted()) {
				return domainVersion();
			} else {
				return (T) AbstractDomainBase.this;
			}
		}

		public String hiliToString() {
			return new HiliLocator(AbstractDomainBase.this).toString();
		}

		/*
		 * iff AbstractDomainBase.this===domainVersion()
		 */
		public boolean isDomainVersion() {
			return Domain.isDomainVersion(AbstractDomainBase.this);
		}

		public boolean isRegistered() {
			return TransformManager.get().isRegistered(AbstractDomainBase.this);
		}

		public T register() {
			TransformManager.get()
					.registerDomainObject(AbstractDomainBase.this);
			return (T) AbstractDomainBase.this;
		}

		public <V extends HasIdAndLocalId> void removeFromProperty(V v,
				String propertyName) {
			TransformManager.get().modifyCollectionProperty(
					AbstractDomainBase.this, propertyName, v,
					CollectionModificationType.REMOVE);
		}

		/*
		 * server-side only
		 */
		public T transactionVersion() {
			return (T) Domain.transactionVersion(AbstractDomainBase.this);
		}
	}
}
