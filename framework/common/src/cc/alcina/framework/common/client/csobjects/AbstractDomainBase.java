package cc.alcina.framework.common.client.csobjects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

@MappedSuperclass
public abstract class AbstractDomainBase extends BaseBindable
		implements HasIdAndLocalId, HasVersionNumber {
	static final transient long serialVersionUID = 1L;

	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	@GwtTransient
	long localId;

	@Override
	public boolean equals(Object obj) {
		return HiliHelper.equals(this, obj);
	}

	/**
	 * Useful for collection listeners - a "check the kids" thing
	 */
	public void fireNullPropertyChange(String name) {
		((MutablePropertyChangeSupport) this.propertyChangeSupport())
				.fireNullPropertyChange(name);
	}

	@Display(name = "Id", orderingHint = 900, visible = @Permission(access = AccessLevel.ADMIN) )
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE) , write = @Permission(access = AccessLevel.ROOT) )
	@Transient
	public abstract long getId();

	@Display(name = "Local id")
	@PropertyPermissions(read = @Permission(access = AccessLevel.ROOT) , write = @Permission(access = AccessLevel.ROOT) )
	@Transient
	public long getLocalId() {
		return this.localId;
	}

	@Version
	@Column(name = "OPTLOCK")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE) , write = @Permission(access = AccessLevel.ROOT) )
	public int getVersionNumber() {
		return versionNumber;
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			if (GWT.isScript()) {
				hash = fastHash(getId(), getLocalId(),
						getClass().getName().hashCode());
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

	@UnsafeNativeLong
	private native int fastHash(long id, long localId, int classHashCode)/*-{
        return LongWrapperHash.fastHash(id) ^ LongWrapperHash.fastHash(localId)
                ^ classHashCode;
	}-*/;

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
	public void setLocalId(long localId) {
		this.localId = localId;
	}

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

	public long provideIdOrLocalIdIfZero() {
		return getId() != 0 ? getId() : getLocalId();
	}
}
