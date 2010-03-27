package cc.alcina.framework.common.client.csobjects;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.IgnoreNullsPropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

@MappedSuperclass
public abstract class AbstractDomainBase extends BaseBindable implements
		HasIdAndLocalId, HasVersionNumber {
	protected transient int hash = 0;

	protected transient String comparisonString;

	int versionNumber;

	long localId;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HasIdAndLocalId) {
			HasIdAndLocalId hili = (HasIdAndLocalId) obj;
			if (!hili.getClass().equals(getClass())) {
				return false;
			}
			if (getId() != 0 || getLocalId() != 0) {
				return (hili.getId() == getId() && hili.getLocalId() == getLocalId());
			} else {
				return this == obj;
			}
		}
		return false;
	}

	/**
	 * Hack - note that the old/newvalues of the propertychangeevent should
	 * !not! be read. For listeners on collection properties
	 */
	public void forceFirePropertyChange(String name) {
		this.propertyChangeSupport.firePropertyChange(name, false, true);
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
