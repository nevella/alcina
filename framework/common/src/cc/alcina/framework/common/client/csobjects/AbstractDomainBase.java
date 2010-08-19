package cc.alcina.framework.common.client.csobjects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

@MappedSuperclass
public abstract class AbstractDomainBase extends BaseBindable implements
		HasIdAndLocalId, HasVersionNumber {
	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	long localId;

	@Override
	public boolean equals(Object obj) {
		return HiliHelper.equals(this, obj);
	}

	/**
	 * Useful for collection listeners - a "check the kids" thing
	 */
	public void fireNullPropertyChange(String name) {
		((MutablePropertyChangeSupport) this.propertyChangeSupport)
				.fireNullPropertyChange(name);
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Id", orderingHint = 900), visible = @Permission(access = AccessLevel.ADMIN))
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public abstract long getId();

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Local id"))
	@PropertyPermissions(read = @Permission(access = AccessLevel.ROOT), write = @Permission(access = AccessLevel.ROOT))
	@Transient
	public long getLocalId() {
		return this.localId;
	}

	@Version
	@Column(name = "OPTLOCK")
	public int getVersionNumber() {
		return versionNumber;
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = Long.valueOf(getId()).hashCode()
					^ Long.valueOf(getLocalId()).hashCode()
					^ getClass().getName().hashCode();
		}
		return hash;
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
	 * in fact...hmmm - used to be gethash/sethash, but _any way_ is really wrong
	 * objects from db are different to client-created objects - use one of the
	 * getobject(class,id,localid) methods if you want to look up
	 * 
	 */
//	public void clearHash() {
//		hash = 0;
//	}

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
		if (!GwittirUtils.isIntrospectable(getClass())) {
			return super.toString();
		}
		String dn = CommonLocator.get().classLookup()
				.displayNameForObject(this);
		dn = !CommonUtils.isNullOrEmpty(dn) ? dn : "[Object]";
		return dn.substring(0, dn.length());
	}

	protected int _compareTo(AbstractDomainBase o) {
		if (comparisonString() != null && o.comparisonString() != null) {
			int i = comparisonString().compareTo(o.comparisonString());
			if (i != 0) {
				return i;
			}
		}
		return new Long(getId()).compareTo(new Long(o.getId()));
	}

	protected String comparisonString() {
		throw new RuntimeException(
				"no display name available, and using comparator");
	}
}
